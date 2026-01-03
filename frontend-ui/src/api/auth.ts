import { API_CONFIG } from './config';
import type { LoginRequest, AuthResponse, ApiError } from '../types';

export async function loginUser(credentials: LoginRequest): Promise<AuthResponse> {
  const response = await fetch(`${API_CONFIG.AUTH_URL}/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const error: ApiError = await response.json();
    throw new Error(error.error || 'Login failed');
  }

  return response.json();
}

export async function registerUser(data: LoginRequest & { email: string }): Promise<AuthResponse> {
  const response = await fetch(`${API_CONFIG.AUTH_URL}/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const error: ApiError = await response.json();
    throw new Error(error.error || 'Registration failed');
  }

  return response.json();
}


