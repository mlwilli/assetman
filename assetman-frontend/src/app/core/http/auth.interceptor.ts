import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenStorage } from '../auth/token.storage';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.headers.has('X-Skip-Auth')) return next(req);

  const url = req.url ?? '';
  const isAuthEndpoint =
    url.includes('/api/auth/login') ||
    url.includes('/api/auth/refresh') ||
    url.includes('/api/auth/signup-tenant') ||
    url.includes('/api/auth/forgot-password') ||
    url.includes('/api/auth/reset-password');

  if (isAuthEndpoint) return next(req);

  const tokenStorage = inject(TokenStorage);
  const accessToken = tokenStorage.getAccessToken();
  if (!accessToken) return next(req);

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${accessToken}` },
    }),
  );
};
