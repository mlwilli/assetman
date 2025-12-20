import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import {
  CreateLocationRequest,
  LocationDto,
  LocationTreeNodeDto,
  LocationType,
  UpdateLocationRequest,
} from './location.models';

@Injectable({ providedIn: 'root' })
export class LocationApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  list(params?: {
    type?: LocationType;
    parentId?: string;
    active?: boolean;
    search?: string;
  }): Observable<LocationDto[]> {
    // IMPORTANT: Never allow "undefined" to be serialized into query params.
    const clean: Record<string, string> = {};

    if (params) {
      if (params.type != null) clean['type'] = String(params.type);

      const parentId = String(params.parentId ?? '').trim();
      if (parentId) clean['parentId'] = parentId;

      if (params.active != null) clean['active'] = String(params.active);

      const search = String(params.search ?? '').trim();
      if (search) clean['search'] = search;
    }

    return this.http.get<LocationDto[]>(`${this.baseUrl}/api/locations`, {
      params: clean as any,
    });
  }

  getTree(): Observable<LocationTreeNodeDto[]> {
    return this.http.get<LocationTreeNodeDto[]>(
      `${this.baseUrl}/api/locations/tree`,
    );
  }

  get(id: string): Observable<LocationDto> {
    return this.http.get<LocationDto>(`${this.baseUrl}/api/locations/${id}`);
  }

  create(request: CreateLocationRequest): Observable<LocationDto> {
    return this.http.post<LocationDto>(`${this.baseUrl}/api/locations`, request);
  }

  update(id: string, request: UpdateLocationRequest): Observable<LocationDto> {
    return this.http.put<LocationDto>(
      `${this.baseUrl}/api/locations/${id}`,
      request,
    );
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/locations/${id}`);
  }
}
