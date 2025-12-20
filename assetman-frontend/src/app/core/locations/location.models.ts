export type LocationType =
  | 'COUNTRY'
  | 'REGION'
  | 'SITE'
  | 'BUILDING'
  | 'FLOOR'
  | 'ROOM'
  | 'OTHER';

export interface LocationDto {
  id: string;
  tenantId: string;
  name: string;
  type: LocationType;
  code?: string | null;
  parentId?: string | null;
  path?: string | null;
  active: boolean;
  sortOrder?: number | null;
  description?: string | null;
  externalRef?: string | null;
  customFieldsJson?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface LocationTreeNodeDto {
  id: string;
  name: string;
  type: LocationType;
  code?: string | null;
  parentId?: string | null;
  active: boolean;
  sortOrder?: number | null;
  children: LocationTreeNodeDto[];
}

/**
 * Matches backend: CreateLocationRequest
 * assetman-backend/.../location/web/LocationDtos.kt
 */
export interface CreateLocationRequest {
  name: string;
  type: LocationType;
  parentId?: string | null;
  code?: string | null;
  active?: boolean;
  sortOrder?: number | null;
  description?: string | null;
  externalRef?: string | null;
  customFieldsJson?: string | null;
}

/**
 * Matches backend: UpdateLocationRequest
 * assetman-backend/.../location/web/LocationDtos.kt
 */
export interface UpdateLocationRequest {
  name: string;
  type: LocationType;
  parentId?: string | null;
  code?: string | null;
  active?: boolean;
  sortOrder?: number | null;
  description?: string | null;
  externalRef?: string | null;
  customFieldsJson?: string | null;
}
