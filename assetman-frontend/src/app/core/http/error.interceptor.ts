import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {catchError, throwError} from 'rxjs';
import { NotificationService } from '../ui/notification.service';
import { ApiErrorResponse } from './api-error.model';
import { AuthService } from '../auth/auth.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(req).pipe(
    // NOTE: refresh interceptor already ran before this
    // This interceptor handles final outcomes only
    // eslint-disable-next-line rxjs/no-implicit-any-catch
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse)) {
        notify.error('Unexpected error occurred');
        return throwError(() => err);
      }

      const apiError = err.error as ApiErrorResponse | null;

      switch (err.status) {
        case 400:
          // Validation or bad request
          if (apiError?.validationErrors?.length) {
            notify.error('Please correct the highlighted errors.');
          } else {
            notify.error(apiError?.message ?? 'Invalid request');
          }
          break;

        case 401:
          // Refresh already attempted and failed
          auth.logout().subscribe({ error: () => {} });
          void router.navigateByUrl('/login');
          break;

        case 403:
          void router.navigateByUrl('/forbidden');
          break;

        case 404:
          notify.error('Resource not found');
          break;

        case 409:
          notify.error(apiError?.message ?? 'Conflict detected');
          break;

        default:
          notify.error('An unexpected server error occurred');
      }

      return throwError(() => err);
    }),
  );
};
