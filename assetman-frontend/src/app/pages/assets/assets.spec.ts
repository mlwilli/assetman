import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { vi } from 'vitest';

import { AssetsPageComponent } from './assets';
import { authInterceptor } from '../../core/http/auth.interceptor';
import { refreshInterceptor } from '../../core/http/refresh.interceptor';

describe('AssetsPageComponent', () => {
  let fixture: ComponentFixture<AssetsPageComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();

    await TestBed.configureTestingModule({
      imports: [AssetsPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AssetsPageComponent);
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
    httpMock.verify();
  });

  it('creates and calls /api/assets on load', async () => {
    fixture.detectChanges();

    // LocationPicker loads tree immediately
    const treeReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/locations/tree'),
    );
    treeReq.flush([
      {
        id: 'loc-root',
        name: 'HQ',
        type: 'SITE',
        code: null,
        parentId: null,
        active: true,
        sortOrder: null,
        children: [],
      },
    ]);

    // Assets request is debounced
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    const assetReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    assetReq.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('reloads assets with locationId when a location is selected in the picker', async () => {
    fixture.detectChanges();

    // 1) Flush locations/tree
    const treeReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/locations/tree'),
    );
    treeReq.flush([
      {
        id: 'loc-root',
        name: 'HQ',
        type: 'SITE',
        code: null,
        parentId: null,
        active: true,
        sortOrder: null,
        children: [
          {
            id: 'loc-floor-1',
            name: 'Floor 1',
            type: 'FLOOR',
            code: null,
            parentId: 'loc-root',
            active: true,
            sortOrder: null,
            children: [],
          },
        ],
      },
    ]);

    // 2) Flush initial assets load
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    const initialAssetsReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );
    initialAssetsReq.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    // 3) Simulate selecting a location (same end effect as the picker output)
    fixture.componentInstance.form.patchValue({ locationId: 'loc-floor-1' });

    // 4) Debounced reload
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    // IMPORTANT:
    // Filter change triggers pageSubject.next(0), so combineLatest emits twice and
    // HttpTestingController sees TWO /api/assets requests (first one gets canceled by switchMap).
    const assetReqs = httpMock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    expect(assetReqs.length).toBeGreaterThanOrEqual(1);

    const getLocationId = (req: { request: any }) => {
      const p = req.request.params;
      return (
        p.get('locationId') ??
        p.get('filters.locationId') ??
        p.get('filters[locationId]') ??
        null
      );
    };

    const reqWithLocation = assetReqs.find(
      (r) => getLocationId(r) === 'loc-floor-1',
    );
    expect(reqWithLocation).toBeTruthy();

// ✅ Flush ONLY the final active request
    const finalReq = assetReqs[assetReqs.length - 1];
    finalReq.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });
});
