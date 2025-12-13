import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AppShellComponent } from './app-shell';
import { AuthService } from '../../core/auth/auth.service';

class AuthServiceStub {
  user$ = of(null);
  sessionReady$ = of(true);
  ensureSessionOnce() {
    return of(null);
  }
  logout() {
    return of(void 0);
  }
}

describe('AppShellComponent', () => {
  let fixture: ComponentFixture<AppShellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useClass: AuthServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppShellComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
