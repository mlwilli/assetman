import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UnitDto, UnitStatus, Uuid } from './unit.models';

@Injectable({ providedIn: 'root' })
export class UnitApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  list(params?: { propertyId?: Uuid; status?: UnitStatus; search?: string }): Observable<UnitDto[]> {
    return this.http.get<UnitDto[]>(`${this.baseUrl}/api/units`, {
      params: params as any,
    });
  }

  get(id: Uuid): Observable<UnitDto> {
    return this.http.get<UnitDto>(`${this.baseUrl}/api/units/${id}`);
  }
}
