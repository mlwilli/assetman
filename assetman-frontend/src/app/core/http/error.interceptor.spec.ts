import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { errorInterceptor } from './error.interceptor';
import { NotificationService } from '../ui/notification.service';
import { AuthService } from '../auth/auth.service';
import { TokenStorage } from '../auth/token.storage';

class MockNotificationService {
  last: string | null = null;
  error(msg: string) { this.last = msg; }
  warn(_: string) {}
  success(_: string) {}
}

class MockAuthService {
  logout() { return { subscribe: (_: any) => {} } as any; }
}

class MockTokenStorage {
  getAccessToken() { return null; }
  getRefreshToken() { return null; }
  clear() {}
}

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let router: Router;
  let notify: MockNotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useClass: MockNotificationService },
        { provide: AuthService, useClass: MockAuthService },
        { provide: TokenStorage, useClass: MockTokenStorage },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    notify = TestBed.inject(NotificationService) as any;

    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

  });

  afterEach(() => httpMock.verify());

  it('redirects to /forbidden on 403', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/test');
    req.flush(
      { status: 403, error: 'Forbidden', message: 'nope', timestamp: new Date().toISOString() },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(router.navigateByUrl).toHaveBeenCalledWith('/forbidden');
  });

  it('redirects to /login on 401 (final outcome)', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/test');
    req.flush(
      { status: 401, error: 'Unauthorized', message: 'nope', timestamp: new Date().toISOString() },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('shows conflict message on 409', () => {
    http.get('/api/test').subscribe({ error: () => {} });

    const req = httpMock.expectOne('/api/test');
    req.flush(
      { status: 409, error: 'Conflict', message: 'Already exists', timestamp: new Date().toISOString() },
      { status: 409, statusText: 'Conflict' },
    );

    expect(notify.last).toBe('Already exists');
  });
});
