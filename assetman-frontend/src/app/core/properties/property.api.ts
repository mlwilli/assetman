import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PropertyDto, PropertyType, Uuid } from './property.models';

@Injectable({ providedIn: 'root' })
export class PropertyApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  list(params?: { type?: PropertyType; search?: string }): Observable<PropertyDto[]> {
    return this.http.get<PropertyDto[]>(`${this.baseUrl}/api/properties`, {
      params: params as any,
    });
  }

  get(id: Uuid): Observable<PropertyDto> {
    return this.http.get<PropertyDto>(`${this.baseUrl}/api/properties/${id}`);
  }
}
