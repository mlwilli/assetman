import { ApplicationConfig, provideAppInitializer, inject } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideNativeDateAdapter } from '@angular/material/core';
import { firstValueFrom } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/http/auth.interceptor';
import { refreshInterceptor } from './core/http/refresh.interceptor';
import { errorInterceptor } from './core/http/error.interceptor';
import { AuthService } from './core/auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),

    // Material datepicker needs a DateAdapter. This is the simplest enterprise-default.
    provideNativeDateAdapter(),

    // ✅ Ensure we resolve /me (or decide anonymous) BEFORE guards run.
    provideAppInitializer(() => {
      const auth = inject(AuthService);
      return firstValueFrom(auth.ensureSessionOnce());
    }),

    provideHttpClient(
      withInterceptors([authInterceptor, refreshInterceptor, errorInterceptor]),
    ),
  ],
};
