import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';

import { LocationApi } from './location.api';
import {
  CreateLocationRequest,
  LocationDto,
  LocationTreeNodeDto,
  UpdateLocationRequest,
} from './location.models';
import { environment } from '../../../environments/environment';

describe('LocationApi', () => {
  let api: LocationApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });

    api = TestBed.inject(LocationApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('loads location tree', () => {
    let result: LocationTreeNodeDto[] | undefined;

    api.getTree().subscribe(r => (result = r));

    const req = httpMock.expectOne(
      `${environment.apiBaseUrl}/api/locations/tree`,
    );
    expect(req.request.method).toBe('GET');

    req.flush([]);

    expect(result).toEqual([]);
  });

  it('creates a location (POST /api/locations)', () => {
    const request: CreateLocationRequest = {
      name: 'HQ',
      type: 'BUILDING',
      parentId: null,
      code: 'HQ-01',
      active: true,
      sortOrder: 10,
      description: 'Main building',
      externalRef: 'EXT-1',
      customFieldsJson: '{"foo":"bar"}',
    };

    let saved: LocationDto | undefined;

    api.create(request).subscribe(r => (saved = r));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/locations`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);

    const response: LocationDto = {
      id: '11111111-1111-1111-1111-111111111111',
      tenantId: '22222222-2222-2222-2222-222222222222',
      name: request.name,
      type: request.type,
      code: request.code ?? null,
      parentId: request.parentId ?? null,
      path: '/some/path',
      active: request.active ?? true,
      sortOrder: request.sortOrder ?? null,
      description: request.description ?? null,
      externalRef: request.externalRef ?? null,
      customFieldsJson: request.customFieldsJson ?? null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    req.flush(response);

    expect(saved).toEqual(response);
  });

  it('updates a location (PUT /api/locations/:id)', () => {
    const id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';

    const request: UpdateLocationRequest = {
      name: 'HQ Updated',
      type: 'BUILDING',
      parentId: null,
      code: 'HQ-02',
      active: false,
      sortOrder: 20,
      description: 'Updated',
      externalRef: 'EXT-2',
      customFieldsJson: '{"baz":"qux"}',
    };

    let saved: LocationDto | undefined;

    api.update(id, request).subscribe(r => (saved = r));

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/locations/${id}`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual(request);

    const response: LocationDto = {
      id,
      tenantId: '22222222-2222-2222-2222-222222222222',
      name: request.name,
      type: request.type,
      code: request.code ?? null,
      parentId: request.parentId ?? null,
      path: '/some/path',
      active: request.active ?? true,
      sortOrder: request.sortOrder ?? null,
      description: request.description ?? null,
      externalRef: request.externalRef ?? null,
      customFieldsJson: request.customFieldsJson ?? null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    req.flush(response);

    expect(saved).toEqual(response);
  });

  it('deletes a location (DELETE /api/locations/:id)', () => {
    const id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';

    let completed = false;
    api.delete(id).subscribe({
      next: () => {
        // delete<void> emits undefined on success
      },
      complete: () => {
        completed = true;
      },
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/locations/${id}`);
    expect(req.request.method).toBe('DELETE');

    req.flush(null);

    expect(completed).toBe(true);
  });
});
