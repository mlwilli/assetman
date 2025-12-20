import { Injectable } from '@angular/core';
import { HttpBackend, HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';
import {
  AuthDto,
  CurrentUserDto,
  LoginRequest,
  RefreshTokenRequest,
} from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthApi {
  private readonly httpBypass: HttpClient;
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient, httpBackend: HttpBackend) {
    this.httpBypass = new HttpClient(httpBackend);
  }

  login(req: LoginRequest): Observable<AuthDto> {
    return this.http.post<AuthDto>(`${this.baseUrl}/api/auth/login`, req);
  }

  /**
   * Bypass interceptors to prevent refresh recursion.
   * Also sets X-Skip-Auth so authInterceptor never attaches Authorization to it.
   */
  refresh(req: RefreshTokenRequest): Observable<AuthDto> {
    const headers = new HttpHeaders({ 'X-Skip-Auth': 'true' });
    return this.httpBypass.post<AuthDto>(`${this.baseUrl}/api/auth/refresh`, req, {
      headers,
    });
  }

  /**
   * Backend requires a RefreshTokenRequest body so it can revoke the refresh token.
   */
  logout(req: RefreshTokenRequest): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/api/auth/logout`, req);
  }

  me(): Observable<CurrentUserDto> {
    return this.http.get<CurrentUserDto>(`${this.baseUrl}/api/me`);
  }
}
