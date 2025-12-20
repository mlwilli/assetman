export type Uuid = string;

/**
 * Backend enum values (AssetStatus.kt).
 * Keep this as a string union so the UI is type-safe and we never send invalid values.
 */
export type AssetStatus =
  | 'PLANNED'
  | 'PROCURED'
  | 'IN_SERVICE'
  | 'UNDER_MAINTENANCE'
  | 'RETIRED'
  | 'DISPOSED';

export interface SelectOption<T extends string> {
  value: T;
  label: string;
}

/**
 * Single source of truth for status dropdowns and display labels.
 */
export const ASSET_STATUS_OPTIONS: ReadonlyArray<SelectOption<AssetStatus>> = [
  { value: 'PLANNED', label: 'Planned' },
  { value: 'PROCURED', label: 'Procured' },
  { value: 'IN_SERVICE', label: 'In Service' },
  { value: 'UNDER_MAINTENANCE', label: 'Under Maintenance' },
  { value: 'RETIRED', label: 'Retired' },
  { value: 'DISPOSED', label: 'Disposed' },
] as const;

/**
 * Utility for displaying a friendly label when you only have the enum value.
 */
export function assetStatusLabel(status: string | null | undefined): string {
  if (!status) return '-';
  const found = ASSET_STATUS_OPTIONS.find((o) => o.value === status);
  return found?.label ?? String(status);
}

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
