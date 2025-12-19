import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { LocationFormPageComponent } from './location-form';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../core/auth/auth.service';

class MockAuthService {
  currentUser: any = { roles: ['OWNER'], email: 'x@example.com' };
  user$ = { subscribe: () => ({ unsubscribe: () => {} }) } as any;
}

describe('LocationFormPageComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: MockAuthService },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
  });

  it('creates (smoke)', () => {
    const fixture = TestBed.createComponent(LocationFormPageComponent);
    fixture.detectChanges();

    // Parent picker will request the tree immediately
    const treeReq = httpMock.expectOne(`${environment.apiBaseUrl}/api/locations/tree`);
    expect(treeReq.request.method).toBe('GET');
    treeReq.flush([]);

    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });
});
