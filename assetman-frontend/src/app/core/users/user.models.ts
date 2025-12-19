export type Uuid = string;

export interface UserDirectoryDto {
  id: Uuid;
  email: string;
  fullName: string;
  displayName?: string | null;
  active: boolean;
  label: string; // backend computed property
}

export function userLabel(u: UserDirectoryDto): string {
  // Prefer backend label; fall back just in case
  return (u.label ?? '').trim() || (u.displayName ?? '').trim() || u.fullName || u.email;
}
