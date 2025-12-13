import { TestBed } from '@angular/core/testing';
import { tap } from 'rxjs';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { authInterceptor } from './auth.interceptor';
import { refreshInterceptor } from './refresh.interceptor';

import { TokenStorage } from '../auth/token.storage';
import { AuthService } from '../auth/auth.service';
import type { AuthDto } from '../auth/auth.models';

// Run bash$ npx vitest run --config vite.config.ts


class MockTokenStorage {
  private access: string | null = 'BAD';
  private refresh: string | null = 'REFRESH_OK';

  getAccessToken() {
    return this.access;
  }
  getRefreshToken() {
    return this.refresh;
  }
  setTokens(t: { accessToken: string; refreshToken: string }) {
    this.access = t.accessToken;
    this.refresh = t.refreshToken;
  }
  clear() {
    this.access = null;
    this.refresh = null;
  }
}

describe('refreshInterceptor (HTTP 401 -> refresh -> retry)', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let tokens: MockTokenStorage;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        // refreshInterceptor calls router.navigateByUrl('/login') on some failure paths
        provideRouter([{ path: 'login', children: [] }]),

        // Only the interceptors under test
        provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor])),
        provideHttpClientTesting(),

        { provide: TokenStorage, useClass: MockTokenStorage },

        // Stub AuthService but keep "real-ish" behavior:
        // refreshTokens() MUST update TokenStorage (matching production AuthService)
        {
          provide: AuthService,
          useFactory: () => {
            const http = TestBed.inject(HttpClient);
            const tokens = TestBed.inject(TokenStorage) as unknown as MockTokenStorage;

            return {
              refreshTokens: () =>
                http
                  .post<AuthDto>('http://localhost:8080/api/auth/refresh', {
                    refreshToken: tokens.getRefreshToken(),
                  })
                  .pipe(tap((t) => tokens.setTokens(t))),

              logout: () => {
                tokens.clear();
                return http.post<void>('http://localhost:8080/api/auth/logout', {});
              },
            } satisfies Pick<AuthService, 'refreshTokens' | 'logout'>;
          },
        },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenStorage) as unknown as MockTokenStorage;
  });

  afterEach(() => {
    // Ensures every request created during a test was asserted + flushed.
    httpMock.verify();
  });

  it(
    [
      'When a protected API call returns 401 (Unauthorized),',
      'the interceptor should:',
      '  1) call /api/auth/refresh exactly once,',
      '  2) update stored tokens,',
      '  3) retry the original request with the NEW access token,',
      'Result: original request succeeds after refresh.',
    ].join('\n'),
    () => {
      console.info('\n[Test] Scenario: Single request 401 -> refresh -> retry -> success');
      console.info('[Test] Given: access token = BAD, refresh token = REFRESH_OK');

      // Arrange
      const url = 'http://localhost:8080/api/me';
      const refreshUrl = 'http://localhost:8080/api/auth/refresh';

      let responseSeen = false;
      let responsePayload: unknown;

      // Act: make a protected call (should include Authorization header from authInterceptor)
      http.get(url).subscribe((res) => {
        responseSeen = true;
        responsePayload = res;
        console.info('[Test] Then: subscriber received a successful response (after retry)');
      });

      // Assert 1: initial request contains the BAD token and fails with 401
      console.info('[Test] Expect: first /api/me goes out with Authorization: Bearer BAD');
      const me1 = httpMock.expectOne(url);
      expect(me1.request.headers.get('Authorization')).toBe('Bearer BAD');

      console.info('[Test] Simulate: backend returns 401 Unauthorized for first /api/me');
      me1.flush({}, { status: 401, statusText: 'Unauthorized' });

      // Assert 2: interceptor triggers refresh call exactly once
      console.info('[Test] Expect: interceptor makes ONE refresh call to /api/auth/refresh');
      const refresh = httpMock.expectOne(refreshUrl);
      expect(refresh.request.method).toBe('POST');

      console.info('[Test] Expect: refresh request body includes current refresh token');
      expect(refresh.request.body).toEqual({ refreshToken: 'REFRESH_OK' });

      console.info('[Test] Simulate: refresh returns NEW tokens (GOOD / REFRESH2)');
      refresh.flush({ accessToken: 'GOOD', refreshToken: 'REFRESH2' } satisfies AuthDto);

      // Assert 3: original request is retried using the NEW access token
      console.info('[Test] Expect: original request is retried with Authorization: Bearer GOOD');
      const me2 = httpMock.expectOne(url);
      expect(me2.request.headers.get('Authorization')).toBe('Bearer GOOD');

      console.info('[Test] Simulate: backend returns success payload for retried /api/me');
      me2.flush({ userId: '1', tenantId: '1', email: 'a', fullName: 'b', roles: [] });

      // Final assertions (makes pass/fail very explicit)
      console.info('[Test] Verify: subscriber saw success');
      expect(responseSeen).toBe(true);
      expect(responsePayload).toBeTruthy();

      console.info('[Test] Verify: TokenStorage now contains the refreshed access token');
      expect(tokens.getAccessToken()).toBe('GOOD');
      expect(tokens.getRefreshToken()).toBe('REFRESH2');
    },
  );

  it(
    [
      'When TWO protected API calls fail with 401 at the same time,',
      'the interceptor should use "single-flight" refresh behavior:',
      '  1) only ONE /api/auth/refresh call is made,',
      '  2) BOTH original requests are retried after refresh,',
      '  3) BOTH retries use the NEW access token.',
      'Result: two calls recover using one refresh request.',
    ].join('\n'),
    () => {
      console.info('\n[Test] Scenario: Two concurrent 401s -> single refresh -> both retry');
      console.info('[Test] Given: access token = BAD, refresh token = REFRESH_OK');

      // Arrange
      const url = 'http://localhost:8080/api/me';
      const refreshUrl = 'http://localhost:8080/api/auth/refresh';

      // Act: fire two calls without waiting (concurrent)
      http.get(url).subscribe(() => console.info('[Test] Then: call #1 eventually succeeds'));
      http.get(url).subscribe(() => console.info('[Test] Then: call #2 eventually succeeds'));

      // Assert 1: both initial calls went out with BAD token
      console.info('[Test] Expect: two initial /api/me requests exist');
      const initial = httpMock.match(url);
      expect(initial.length).toBe(2);

      console.info('[Test] Expect: both initial requests have Authorization: Bearer BAD');
      expect(initial[0].request.headers.get('Authorization')).toBe('Bearer BAD');
      expect(initial[1].request.headers.get('Authorization')).toBe('Bearer BAD');

      // Simulate both failing with 401
      console.info('[Test] Simulate: both initial /api/me requests return 401 Unauthorized');
      initial[0].flush({}, { status: 401, statusText: 'Unauthorized' });
      initial[1].flush({}, { status: 401, statusText: 'Unauthorized' });

      // Assert 2: refresh is called only once
      console.info('[Test] Expect: ONLY ONE refresh request is made');
      const refresh = httpMock.expectOne(refreshUrl);
      expect(refresh.request.method).toBe('POST');
      expect(refresh.request.body).toEqual({ refreshToken: 'REFRESH_OK' });

      console.info('[Test] Simulate: refresh returns NEW tokens (GOOD / REFRESH2)');
      refresh.flush({ accessToken: 'GOOD', refreshToken: 'REFRESH2' } satisfies AuthDto);

      // Assert 3: both original requests are retried with the new token
      console.info('[Test] Expect: both original calls are retried after refresh');
      const retries = httpMock.match(url);
      expect(retries.length).toBe(2);

      console.info('[Test] Expect: both retries use Authorization: Bearer GOOD');
      expect(retries[0].request.headers.get('Authorization')).toBe('Bearer GOOD');
      expect(retries[1].request.headers.get('Authorization')).toBe('Bearer GOOD');

      console.info('[Test] Simulate: backend returns success for both retries');
      retries[0].flush({ userId: '1', tenantId: '1', email: 'a', fullName: 'b', roles: [] });
      retries[1].flush({ userId: '1', tenantId: '1', email: 'a', fullName: 'b', roles: [] });

      console.info('[Test] Verify: TokenStorage was updated once refresh succeeded');
      expect(tokens.getAccessToken()).toBe('GOOD');
      expect(tokens.getRefreshToken()).toBe('REFRESH2');
    },
  );
});
