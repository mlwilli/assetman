import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { AssetApi } from './asset.api';

describe('AssetApi', () => {
  let api: AssetApi;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), AssetApi],
    });

    api = TestBed.inject(AssetApi);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listAssets calls GET /api/assets with page/size and provided filters', () => {
    api
      .listAssets({
        page: 2,
        size: 50,
        filters: {
          search: 'drill',
          status: 'IN_SERVICE',
          category: 'Tools',
          locationId: '11111111-1111-1111-1111-111111111111',
        },
      })
      .subscribe((res) => {
        expect(res.page).toBe(2);
        expect(res.size).toBe(50);
        expect(res.totalElements).toBe(1);
        expect(res.content[0].name).toBe('Hammer Drill');
      });

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.get('search')).toBe('drill');
    expect(req.request.params.get('status')).toBe('IN_SERVICE');
    expect(req.request.params.get('category')).toBe('Tools');
    expect(req.request.params.get('locationId')).toBe(
      '11111111-1111-1111-1111-111111111111',
    );

    req.flush({
      content: [
        { id: 'a', tenantId: 't', name: 'Hammer Drill', status: 'IN_SERVICE' },
      ],
      page: 2,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
  });

  it('listAssets does not send empty-string filters', () => {
    api
      .listAssets({
        page: 0,
        size: 20,
        filters: {
          search: '',
          status: null,
          category: '',
          locationId: '',
          propertyId: '',
          unitId: '',
          assignedUserId: '',
        },
      })
      .subscribe();

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );

    // Should not include “empty” params
    expect(req.request.params.has('search')).toBe(false);
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('category')).toBe(false);
    expect(req.request.params.has('locationId')).toBe(false);
    expect(req.request.params.has('propertyId')).toBe(false);
    expect(req.request.params.has('unitId')).toBe(false);
    expect(req.request.params.has('assignedUserId')).toBe(false);

    // But paging params must still be present
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');

    req.flush({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  it('getAsset calls GET /api/assets/:id', () => {
    api.getAsset('abc').subscribe((res) => {
      expect(res.id).toBe('abc');
    });

    const req = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/assets/abc'),
    );

    req.flush({ id: 'abc', tenantId: 't', name: 'X', status: 'IN_SERVICE' });
  });

  it('createAsset calls POST /api/assets', () => {
    api.createAsset({ name: 'New', status: 'IN_SERVICE' }).subscribe((res) => {
      expect(res.name).toBe('New');
    });

    const req = httpMock.expectOne(
      (r) => r.method === 'POST' && r.url.includes('/api/assets'),
    );

    expect(req.request.body.name).toBe('New');

    req.flush({ id: 'newid', tenantId: 't', name: 'New', status: 'IN_SERVICE' });
  });

  it('updateAsset calls PUT /api/assets/:id', () => {
    api
      .updateAsset('id1', { name: 'Updated', status: 'RETIRED' })
      .subscribe((res) => {
        expect(res.name).toBe('Updated');
      });

    const req = httpMock.expectOne(
      (r) => r.method === 'PUT' && r.url.includes('/api/assets/id1'),
    );

    expect(req.request.body.status).toBe('RETIRED');

    req.flush({ id: 'id1', tenantId: 't', name: 'Updated', status: 'RETIRED' });
  });

  it('deleteAsset calls DELETE /api/assets/:id', () => {
    api.deleteAsset('id2').subscribe();

    const req = httpMock.expectOne(
      (r) => r.method === 'DELETE' && r.url.includes('/api/assets/id2'),
    );

    req.flush(null);
  });
});
