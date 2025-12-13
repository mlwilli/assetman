import { TestBed } from '@angular/core/testing';
import { Router, CanMatchFn, GuardResult, MaybeAsync } from '@angular/router';
import { of, isObservable, from, firstValueFrom } from 'rxjs';
import { describe, it, expect, vi } from 'vitest';

import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';
import type { CurrentUserDto } from './auth.models';

function toObservable<T>(value: MaybeAsync<T>) {
  if (isObservable(value)) return value;
  if (value instanceof Promise) return from(value);
  return of(value);
}

describe('roleGuard', () => {
  function setup(opts: { user: CurrentUserDto | null }) {
    const router = {
      parseUrl: vi.fn((url: string) => ({ url })),
    } as unknown as Router;

    const auth = {
      sessionReady$: of(true),
      currentUser: opts.user,
    } as unknown as AuthService;

    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: auth },
      ],
    });

    return { router };
  }

  it('redirects to /login when no user session exists', async () => {
    const { router } = setup({ user: null });

    const guardFn: CanMatchFn = roleGuard(['ADMIN']);

    const result = TestBed.runInInjectionContext(() =>
      guardFn({} as any, [] as any),
    );

    const resolved = await firstValueFrom(
      toObservable(result as MaybeAsync<GuardResult>),
    );

    expect(router.parseUrl).toHaveBeenCalledWith('/login');
    expect((resolved as any).url).toBe('/login');
  });
});
