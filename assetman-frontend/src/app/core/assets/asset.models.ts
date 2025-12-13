export type Uuid = string;

/**
 * We intentionally keep AssetStatus as string for now.
 * Reason: we haven't confirmed backend enum values (e.g. IN_SERVICE, RETIRED, etc.).
 * This avoids guessing while still allowing filtering by passing raw text.
 */
export type AssetStatus = string;

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AssetDto {
  id: Uuid;
  tenantId: Uuid;
  name: string;
  status: AssetStatus;

  // optional fields (present in backend DTO)
  code?: string | null;
  category?: string | null;
  manufacturer?: string | null;
  model?: string | null;
  serialNumber?: string | null;
  assetTag?: string | null;
  externalRef?: string | null;

  tags?: string[]; // backend is List<String>

  locationId?: Uuid | null;
  propertyId?: Uuid | null;
  unitId?: Uuid | null;
  assignedUserId?: Uuid | null;

  purchaseCost?: number | null;
  residualValue?: number | null;

  purchaseDate?: string | null;
  inServiceDate?: string | null;
  retiredDate?: string | null;
  disposedDate?: string | null;
  warrantyExpiryDate?: string | null;

  depreciationYears?: number | null;

  createdAt?: string;
  updatedAt?: string;

  customFieldsJson?: string | null;
}

export interface AssetListFilters {
  status?: AssetStatus | null;
  category?: string | null;
  locationId?: Uuid | null;
  propertyId?: Uuid | null;
  unitId?: Uuid | null;
  assignedUserId?: Uuid | null;
  search?: string | null;
}

export interface AssetListRequest {
  filters?: AssetListFilters;
  page?: number;
  size?: number;
}

export interface AssetUpsertRequest {
  name: string;
  status: AssetStatus;

  manufacturer?: string | null;
  code?: string | null;
  model?: string | null;
  serialNumber?: string | null;
  assetTag?: string | null;
  externalRef?: string | null;
  category?: string | null;

  tags?: string[] | null;

  purchaseDate?: string | null;
  inServiceDate?: string | null;
  retiredDate?: string | null;
  disposedDate?: string | null;
  warrantyExpiryDate?: string | null;

  purchaseCost?: number | null;
  residualValue?: number | null;
  depreciationYears?: number | null;

  locationId?: Uuid | null;
  propertyId?: Uuid | null;
  unitId?: Uuid | null;
  assignedUserId?: Uuid | null;

  customFieldsJson?: string | null;
}
