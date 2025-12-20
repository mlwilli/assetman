export type Uuid = string;

export type PropertyType =
  | 'RESIDENTIAL'
  | 'COMMERCIAL'
  | 'INDUSTRIAL'
  | 'LAND'
  | 'MIXED_USE'
  | 'OTHER';

export interface PropertyDto {
  id: Uuid;
  tenantId: Uuid;
  name: string;
  type: PropertyType;
  code?: string | null;
  locationId?: Uuid | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export function propertyLabel(p: PropertyDto): string {
  const code = (p.code ?? '').trim();
  return code ? `${p.name} (${code})` : p.name;
}
