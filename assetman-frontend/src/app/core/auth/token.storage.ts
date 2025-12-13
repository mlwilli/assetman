import { Injectable } from '@angular/core';
import { AuthDto } from './auth.models';

const ACCESS_TOKEN_KEY = 'assetman.accessToken';
const REFRESH_TOKEN_KEY = 'assetman.refreshToken';

@Injectable({ providedIn: 'root' })
export class TokenStorage {
  private accessTokenCache: string | null = null;
  private refreshTokenCache: string | null = null;

  getAccessToken(): string | null {
    if (this.accessTokenCache !== null) return this.accessTokenCache;
    const v = localStorage.getItem(ACCESS_TOKEN_KEY);
    this.accessTokenCache = v;
    return v;
  }

  getRefreshToken(): string | null {
    if (this.refreshTokenCache !== null) return this.refreshTokenCache;
    const v = localStorage.getItem(REFRESH_TOKEN_KEY);
    this.refreshTokenCache = v;
    return v;
  }

  setTokens(tokens: AuthDto): void {
    this.accessTokenCache = tokens.accessToken ?? null;
    this.refreshTokenCache = tokens.refreshToken ?? null;

    if (tokens.accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
    else localStorage.removeItem(ACCESS_TOKEN_KEY);

    if (tokens.refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
    else localStorage.removeItem(REFRESH_TOKEN_KEY);
  }

  clear(): void {
    this.accessTokenCache = null;
    this.refreshTokenCache = null;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  }
}
