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
