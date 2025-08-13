// TypeScript interfaces for User domain - Frontend-Backend communication

export interface UserDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  emailVerified: boolean;
  createdAt: string; // ISO 8601 date string
  updatedAt: string; // ISO 8601 date string
  roles: UserRoleDto[];
  preferences: Record<string, string>; // Key-value pairs for preferences
}

export interface UserRoleDto {
  id: string;
  role: Role;
  grantedAt: string; // ISO 8601 date string
  expiresAt?: string; // ISO 8601 date string, optional
  grantedBy?: string; // User ID who granted the role
}

export interface UserPreferenceDto {
  id: string;
  preferenceKey: string;
  preferenceValue: string;
  createdAt: string;
  updatedAt: string;
}

export enum Role {
  FREE_USER = 'FREE_USER',
  PRO_USER = 'PRO_USER',
  ENTERPRISE_USER = 'ENTERPRISE_USER',
  ADMIN = 'ADMIN',
  SUPER_ADMIN = 'SUPER_ADMIN'
}

// Request/Response DTOs for API endpoints
export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UpdateUserRequest {
  firstName?: string;
  lastName?: string;
  preferences?: Record<string, string>;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  success: boolean;
  data: {
    user: UserDto;
    accessToken: string;
    refreshToken: string;
    expiresIn: number; // seconds
  };
  meta: ResponseMeta;
}

export interface UserRegistrationRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface UserRegistrationResponse {
  success: boolean;
  data: {
    user: UserDto;
    emailVerificationRequired: boolean;
  };
  meta: ResponseMeta;
}

export interface ResponseMeta {
  timestamp: string;
  correlationId: string;
  version: string;
}