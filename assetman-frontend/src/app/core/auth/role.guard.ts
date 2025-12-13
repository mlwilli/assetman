import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { map } from 'rxjs';

export function roleGuard(allowedRoles: string[]): CanMatchFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);

    return auth.sessionReady$.pipe(
      map(() => {
        const user = auth.currentUser;

        if (!user) {
          return router.parseUrl('/login');
        }

        const hasRole = (user.roles ?? []).some((r: string) => allowedRoles.includes(r));
        return hasRole ? true : (router.parseUrl('/forbidden') as UrlTree);
      }),
    );
  };
}
