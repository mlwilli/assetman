import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, ReplaySubject, catchError, map, of, switchMap, tap, throwError } from 'rxjs';
import { AuthApi } from './auth.api';
import { AuthDto, CurrentUserDto, LoginRequest } from './auth.models';
import { TokenStorage } from './token.storage';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSubject = new BehaviorSubject<CurrentUserDto | null>(null);
  readonly user$ = this.userSubject.asObservable();

  private readonly sessionReadySubject = new ReplaySubject<boolean>(1);
  readonly sessionReady$ = this.sessionReadySubject.asObservable();

  private sessionInitStarted = false;

  constructor(
    private readonly api: AuthApi,
    private readonly tokens: TokenStorage,
  ) {}

  hasAccessToken(): boolean {
    return !!this.tokens.getAccessToken();
  }

  /** Cached snapshot for guards/components */
  get currentUser(): CurrentUserDto | null {
    return this.userSubject.value;
  }

  /** True once we have attempted to resolve /me (or determined no session). */
  get isSessionReady(): boolean {
    // Not directly readable from ReplaySubject; use sessionInitStarted + currentUser checks in guard.
    return this.sessionInitStarted;
  }

  login(req: LoginRequest): Observable<CurrentUserDto> {
    return this.api.login(req).pipe(
      tap((tokens: AuthDto) => this.tokens.setTokens(tokens)),
      switchMap(() => this.api.me()),
      tap(user => this.userSubject.next(user)),
      tap(() => this.sessionReadySubject.next(true)),
    );
  }

  /**
   * Initialize session ONCE per app load.
   * - If no token -> session ready (anonymous)
   * - If token -> attempt /me; if it fails -> anonymous
   */
  ensureSessionOnce(): Observable<CurrentUserDto | null> {
    if (this.sessionInitStarted) {
      return of(this.userSubject.value);
    }
    this.sessionInitStarted = true;

    if (!this.hasAccessToken()) {
      this.userSubject.next(null);
      this.sessionReadySubject.next(true);
      return of(null);
    }

    return this.api.me().pipe(
      tap(user => this.userSubject.next(user)),
      tap(() => this.sessionReadySubject.next(true)),
      map(user => user as CurrentUserDto),
      catchError(() => {
        this.userSubject.next(null);
        this.sessionReadySubject.next(true);
        return of(null);
      }),
    );
  }

  refreshTokens(): Observable<AuthDto> {
    const refreshToken = this.tokens.getRefreshToken();
    if (!refreshToken) return throwError(() => new Error('No refresh token available'));

    return this.api.refresh({ refreshToken }).pipe(
      tap(newTokens => this.tokens.setTokens(newTokens)),
    );
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(
      catchError(() => of(void 0)),
      tap(() => {
        this.tokens.clear();
        this.userSubject.next(null);
        // keep sessionInitStarted true; session is now anonymous but "known"
        this.sessionReadySubject.next(true);
      }),
      map(() => void 0),
    );
  }
}
