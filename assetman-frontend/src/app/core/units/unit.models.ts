export type Uuid = string;

export type UnitStatus = 'VACANT' | 'OCCUPIED' | 'RESERVED' | 'OUT_OF_SERVICE';

export interface UnitDto {
  id: Uuid;
  tenantId: Uuid;
  propertyId: Uuid;
  name: string;
  floor?: string | null;
  status: UnitStatus;
  createdAt: string;
  updatedAt: string;
}

export function unitLabel(u: UnitDto): string {
  const floor = (u.floor ?? '').trim();
  return floor ? `${u.name} (Floor ${floor})` : u.name;
}
