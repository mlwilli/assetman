import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDirectoryDto, Uuid } from './user.models';

@Injectable({ providedIn: 'root' })
export class UserApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  list(params?: { search?: string; limit?: number; activeOnly?: boolean }): Observable<UserDirectoryDto[]> {
    return this.http.get<UserDirectoryDto[]>(`${this.baseUrl}/api/users`, {
      params: params as any,
    });
  }

  get(id: Uuid): Observable<UserDirectoryDto> {
    return this.http.get<UserDirectoryDto>(`${this.baseUrl}/api/users/${id}`);
  }
}
