import {
  HttpErrorResponse,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  catchError,
  finalize,
  map,
  of,
  shareReplay,
  switchMap,
  throwError,
} from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenStorage } from '../auth/token.storage';

let refreshInFlight$: Observable<void> | null = null;

export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const tokens = inject(TokenStorage);
  const router = inject(Router);

  const url = req.url ?? '';
  const isAuthCall =
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/refresh') ||
    url.includes('/api/auth/logout') ||
    url.includes('/api/auth/signup-tenant') ||
    url.includes('/api/auth/forgot-password') ||
    url.includes('/api/auth/reset-password');

  if (isAuthCall) return next(req);

  const forceLogoutAndRedirect = (cause: unknown) => {
    // IMPORTANT: no subscribe() here. We return a chain.
    return auth.logout().pipe(
      catchError(() => of(void 0)), // never let logout failure mask the original issue
      switchMap(() => {
        void router.navigateByUrl('/login');
        return throwError(() => cause);
      }),
    );
  };

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) return throwError(() => err);
      if (err.status !== 401) return throwError(() => err);

      // If no refresh token, we can't refresh -> logout + redirect
      if (!tokens.getRefreshToken()) {
        return forceLogoutAndRedirect(err);
      }

      // Single-flight refresh
      if (!refreshInFlight$) {
        refreshInFlight$ = auth.refreshTokens().pipe(
          map(() => void 0),
          // shareReplay ensures concurrent 401s share the same refresh request/result
          shareReplay({ bufferSize: 1, refCount: false }),
          finalize(() => {
            refreshInFlight$ = null;
          }),
        );
      }

      return refreshInFlight$.pipe(
        switchMap(() => {
          const access = tokens.getAccessToken();
          if (!access) {
            // refresh "succeeded" but we still don't have an access token -> treat as auth failure
            return forceLogoutAndRedirect(err);
          }

          const retriedRequest: HttpRequest<unknown> = req.clone({
            setHeaders: { Authorization: `Bearer ${access}` },
          });

          return next(retriedRequest);
        }),
        catchError((refreshErr) => forceLogoutAndRedirect(refreshErr)),
      );
    }),
  );
};
