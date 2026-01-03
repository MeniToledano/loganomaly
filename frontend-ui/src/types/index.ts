export interface User {
  token: string;
  username: string;
  email: string;
}

export interface Alert {
  id: string;
  type: 'HIGH_ERROR_RATE' | 'KEYWORD_MATCH' | 'UNUSUAL_LOGIN' | 'CUSTOM_ANOMALY';
  severity: 'INFO' | 'WARNING' | 'CRITICAL';
  message: string;
  service: string;
  detectedAt: string;
  acknowledged: boolean;
  acknowledgedAt?: string;
  acknowledgedBy?: string;
}

export interface AlertStats {
  totalAlerts: number;
  unacknowledgedAlerts: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  email: string;
}

export interface ApiError {
  error: string;
  status: number;
  timestamp: number;
}


