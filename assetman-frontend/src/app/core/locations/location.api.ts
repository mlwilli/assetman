import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import { LocationDto, LocationTreeNodeDto, LocationType } from './location.models';

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
    return this.http.get<LocationDto[]>(`${this.baseUrl}/api/locations`, {
      params: params as any,
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
}
