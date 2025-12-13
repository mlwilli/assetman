export type Role = 'OWNER' | 'ADMIN' | 'MANAGER' | 'TECHNICIAN' | 'VIEWER';

export interface LoginRequest {
  tenantSlug: string;
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

/** Backend: AuthDto */
export interface AuthDto {
  accessToken: string;
  refreshToken: string;
}

/** Backend: CurrentUserDto */
export interface CurrentUserDto {
  userId: string;
  tenantId: string;
  email: string;
  fullName: string;
  roles: string[]; // backend returns List<String>
}
