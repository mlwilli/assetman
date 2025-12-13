import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';

import { LocationApi } from './location.api';
import { LocationTreeNodeDto } from './location.models';
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

  afterEach(() => httpMock.verify());

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
});
