import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { vi } from 'vitest';

import { LocationsPageComponent } from './locations';
import { environment } from '../../../environments/environment';

describe('LocationsPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.useRealTimers();
  });

  it('creates and calls /api/locations/tree on initial load (tree view)', async () => {
    const fixture = TestBed.createComponent(LocationsPageComponent);
    fixture.detectChanges();

    // Drive debounceTime(200)
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    const treeReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/locations/tree`,
    );
    expect(treeReq.request.method).toBe('GET');
    treeReq.flush([]);

    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('calls /api/locations when switching to list view', async () => {
    const fixture = TestBed.createComponent(LocationsPageComponent);
    fixture.detectChanges();

    // Initial tree load
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    const treeReq = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/locations/tree`,
    );
    treeReq.flush([]);

    // Switch to list view
    fixture.componentInstance.setViewMode('list');

    // allow pipeline to run again
    fixture.detectChanges();
    vi.advanceTimersByTime(250);
    await Promise.resolve();

    const listReq = httpMock.expectOne(req =>
      req.url === `${environment.apiBaseUrl}/api/locations`,
    );
    expect(listReq.request.method).toBe('GET');
    listReq.flush([]);

    fixture.detectChanges();
  });
});
