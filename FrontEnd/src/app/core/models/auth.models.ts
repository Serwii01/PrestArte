import { Role } from './user.models';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  userId: number;
  email: string;
  name: string;
  role: Role;
  expiresInMs: number;
}

export interface RegistrationRequest {
  email: string;
  password: string;
  name: string;
  phone?: string;
  taxId?: string;
  role: 'COLLECTOR' | 'FOUNDATION' | 'TRANSPORT';
}
