import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { AssetFormPageComponent } from './asset-form';
import { authInterceptor } from '../../core/http/auth.interceptor';
import { refreshInterceptor } from '../../core/http/refresh.interceptor';
import { errorInterceptor } from '../../core/http/error.interceptor';
import { AuthService } from '../../core/auth/auth.service';

describe('AssetFormPageComponent', () => {
  let fixture: ComponentFixture<AssetFormPageComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetFormPageComponent],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(new Map() as any) } }, // create mode
        provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    // Ensure UI permission check passes without guessing your CurrentUserDto shape:
    // We set roles as an array, which normalizeRoles supports.
    const auth = TestBed.inject(AuthService);
    (auth as any).userSubject?.next?.({ roles: ['OWNER'] }); // if private in your build, this is harmless no-op

    fixture = TestBed.createComponent(AssetFormPageComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('creates without calling /api/assets until submit (location tree may load)', () => {
    fixture.detectChanges();

    // LocationPicker loads the tree once on render.
    const treeReq = httpMock.expectOne(
      (r) => r.method === 'GET' && r.url.includes('/api/locations/tree'),
    );

    treeReq.flush([
      {
        id: 'loc-1',
        name: 'HQ',
        type: 'SITE',
        code: null,
        parentId: null,
        active: true,
        sortOrder: null,
        children: [],
      },
    ]);

    // Ensure no asset calls on create-mode load.
    const assetReqs = httpMock.match(
      (r) => r.method === 'GET' && r.url.includes('/api/assets'),
    );
    expect(assetReqs.length).toBe(0);

    expect(fixture.componentInstance).toBeTruthy();
  });
});
