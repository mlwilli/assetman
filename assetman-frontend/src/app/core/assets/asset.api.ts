import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssetDto,
  AssetListRequest,
  AssetUpsertRequest,
  PageResponse,
  Uuid,
} from './asset.models';

@Injectable({ providedIn: 'root' })
export class AssetApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  listAssets(req?: AssetListRequest): Observable<PageResponse<AssetDto>> {
    const page = req?.page ?? 0;
    const size = req?.size ?? 20;

    let params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));

    const f = req?.filters;

    params = this.addIfPresent(params, 'status', f?.status);
    params = this.addIfPresent(params, 'category', f?.category);
    params = this.addIfPresent(params, 'locationId', f?.locationId);
    params = this.addIfPresent(params, 'propertyId', f?.propertyId);
    params = this.addIfPresent(params, 'unitId', f?.unitId);
    params = this.addIfPresent(params, 'assignedUserId', f?.assignedUserId);
    params = this.addIfPresent(params, 'search', f?.search);

    return this.http.get<PageResponse<AssetDto>>(`${this.baseUrl}/api/assets`, {
      params,
    });
  }

  getAsset(id: Uuid): Observable<AssetDto> {
    return this.http.get<AssetDto>(`${this.baseUrl}/api/assets/${id}`);
  }

  createAsset(request: AssetUpsertRequest): Observable<AssetDto> {
    return this.http.post<AssetDto>(`${this.baseUrl}/api/assets`, request);
  }

  updateAsset(id: Uuid, request: AssetUpsertRequest): Observable<AssetDto> {
    return this.http.put<AssetDto>(`${this.baseUrl}/api/assets/${id}`, request);
  }

  deleteAsset(id: Uuid): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/assets/${id}`);
  }

  /**
   * Adds a query param if the value is "meaningful".
   * Drops: null, undefined, empty string, whitespace-only string.
   */
  private addIfPresent(
    params: HttpParams,
    key: string,
    value: unknown,
  ): HttpParams {
    if (value === null || value === undefined) return params;

    const s = String(value).trim();
    if (!s) return params;

    return params.set(key, s);
  }
}
