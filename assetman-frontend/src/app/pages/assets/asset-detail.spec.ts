import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AssetDetailPageComponent } from './asset-detail';
import { authInterceptor } from '../../core/http/auth.interceptor';
import { refreshInterceptor } from '../../core/http/refresh.interceptor';
import { errorInterceptor } from '../../core/http/error.interceptor';

describe('AssetDetailPageComponent', () => {
  let fixture: ComponentFixture<AssetDetailPageComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetDetailPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(new Map([['id', 'asset-1']]) as any) },
        },
        provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetDetailPageComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('loads asset via GET /api/assets/:id', () => {
    fixture.detectChanges();

    const req = httpMock.expectOne((r) => r.method === 'GET' && r.url.includes('/api/assets/asset-1'));
    req.flush({ id: 'asset-1', tenantId: 't', name: 'A', status: 'IN_SERVICE' });

    expect(fixture.componentInstance).toBeTruthy();
  });
});
