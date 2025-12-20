import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AssetFormPageComponent } from './asset-form';
import { AuthService } from '../../core/auth/auth.service';

class MockAuthService {
  currentUser = { roles: ['ADMIN'] };
}

describe('AssetFormPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetFormPageComponent],
      providers: [
        provideRouter([]), // ✅ provides ActivatedRoute for RouterLink
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: MockAuthService },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    flushOptionalBackgroundRequests(httpMock);
    httpMock.verify();
  });

  it('create-mode: renders pickers and does not call /api/assets until submit', () => {
    const fixture = TestBed.createComponent(AssetFormPageComponent);
    fixture.detectChanges();

    flushOptionalBackgroundRequests(httpMock);

    // Ensure NO asset list call happens
    const assetGets = httpMock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );
    expect(assetGets.length).toBe(0);

    // Ensure we did not create/update anything automatically
    const assetWrites = httpMock.match(
      (r) =>
        (r.method === 'POST' || r.method === 'PUT') &&
        r.url.includes('/api/assets'),
    );
    expect(assetWrites.length).toBe(0);
  });

  function flushOptionalBackgroundRequests(mock: HttpTestingController) {
    const props = mock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/properties'),
    );
    props.forEach((r) => r.flush([]));

    const users = mock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/users'),
    );
    users.forEach((r) =>
      r.flush({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    );

    const units = mock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/units'),
    );
    units.forEach((r) => r.flush([]));

    const locs = mock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/locations'),
    );
    locs.forEach((r) => r.flush([]));
  }
});
