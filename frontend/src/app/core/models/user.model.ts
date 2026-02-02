export interface User {
  id: string;
  name: string;
  email: string;
  role: 'CLIENT' | 'SELLER';
  avatar?: string;
  createdAt?: Date;
  updatedAt?: Date;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: 'CLIENT' | 'SELLER';
}

export interface AuthResponse {
  token: string;
  type: string;
  userId: string;
  email: string;
  name: string;
  role: 'CLIENT' | 'SELLER';
  avatar?: string;
}
