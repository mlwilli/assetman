import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { convertToParamMap, ActivatedRoute } from '@angular/router';
import { BehaviorSubject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { LocationDetailPageComponent } from './location-detail';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

class MockAuthService {
  currentUser: any = { roles: ['OWNER'], email: 'x@example.com' };
  user$ = { subscribe: () => ({ unsubscribe: () => {} }) } as any;
}

describe('LocationDetailPageComponent', () => {
  let httpMock: HttpTestingController;

  // ActivatedRoute.paramMap stub
  const paramMap$ = new BehaviorSubject(convertToParamMap({ id: 'LOC_1' }));
  const activatedRouteStub: Partial<ActivatedRoute> = {
    paramMap: paramMap$.asObservable(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: MockAuthService },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('creates and calls /api/locations/:id on load', () => {
    const fixture = TestBed.createComponent(LocationDetailPageComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/locations/LOC_1`);
    expect(req.request.method).toBe('GET');

    req.flush({
      id: 'LOC_1',
      tenantId: 'TENANT_1',
      name: 'HQ',
      type: 'SITE',
      code: null,
      parentId: null,
      path: '/LOC_1',
      active: true,
      sortOrder: null,
      description: null,
      externalRef: null,
      customFieldsJson: null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });

    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});
