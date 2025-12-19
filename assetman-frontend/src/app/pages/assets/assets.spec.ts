import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { AssetsPageComponent } from './assets';
import { AuthService } from '../../core/auth/auth.service';

class MockAuthService {
  currentUser = { roles: ['ADMIN'] };
}

describe('AssetsPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();

    await TestBed.configureTestingModule({
      imports: [AssetsPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: MockAuthService },
      ],
      teardown: { destroyAfterEach: true },
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // If a test fails before flushing timers, this keeps cleanup consistent
    try {
      vi.runOnlyPendingTimers();
    } finally {
      vi.useRealTimers();
    }

    // Verify no unexpected HTTP calls remain open
    httpMock.verify();
  });

  /**
   * AssetsPage imports the real LocationPickerComponent (and possibly other
   * shared pickers transitively). Those components issue HTTP requests on init.
   * We must flush them so httpMock.verify() doesn't fail.
   */
  function flushPickerStartupRequests() {
    // /api/locations/tree
    const locReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/locations/tree'),
    );
    locReq.flush([]); // safe: "no locations" for this test

    // /api/properties?search=undefined  (shape can be list; this unblocks request)
    const propReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/properties'),
    );
    propReq.flush([]); // minimal “no properties”

    // /api/users?... (could be list or page; use empty list to unblock)
    const userReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/users'),
    );
    userReq.flush([]); // minimal “no users”
  }

  it('creates and calls /api/assets on load', () => {
    TestBed.createComponent(AssetsPageComponent);

    // AssetsPage uses debounceTime(200) on form changes
    vi.advanceTimersByTime(250);

    // Flush startup calls from embedded pickers/components
    flushPickerStartupRequests();

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');

    // empty filters should not be sent
    expect(req.request.params.has('search')).toBe(false);
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('category')).toBe(false);
    expect(req.request.params.has('locationId')).toBe(false);
    expect(req.request.params.has('propertyId')).toBe(false);
    expect(req.request.params.has('unitId')).toBe(false);
    expect(req.request.params.has('assignedUserId')).toBe(false);

    req.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it('reloads assets with locationId when a location is selected', () => {
    const fixture = TestBed.createComponent(AssetsPageComponent);
    const component = fixture.componentInstance;

    // initial load
    vi.advanceTimersByTime(250);
    flushPickerStartupRequests();

    const first = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );
    first.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    // simulate location selection
    const locId = '11111111-1111-1111-1111-111111111111';
    component.onLocationSelected(locId);

    vi.advanceTimersByTime(250);

    // ✅ there can be TWO requests; capture both
    const assetReqs = httpMock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    expect(assetReqs.length).toBeGreaterThanOrEqual(1);

    // Assert at least one request includes locationId
    const withLocation = assetReqs.filter(
      (r) => r.request.params.get('locationId') === locId,
    );
    expect(withLocation.length).toBeGreaterThanOrEqual(1);

    // Flush all of them so verify() passes
    // Flush only active requests (cancelled ones throw if you flush them)
    for (const r of assetReqs) {
      try {
        r.flush({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        });
      } catch (e: any) {
        // ignore cancelled requests (switchMap cancels the prior HTTP)
        if (!String(e?.message ?? e).includes('cancelled request')) throw e;
      }
    }

  });
});
