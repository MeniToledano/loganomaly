import { API_CONFIG } from './config';
import type { Alert, AlertStats } from '../types';

export async function fetchAlerts(): Promise<Alert[]> {
  const response = await fetch(`${API_CONFIG.ANALYSIS_URL}/api/alerts`);
  if (!response.ok) {
    throw new Error('Failed to fetch alerts');
  }
  return response.json();
}

export async function fetchUnacknowledgedAlerts(): Promise<Alert[]> {
  const response = await fetch(`${API_CONFIG.ANALYSIS_URL}/api/alerts/unacknowledged`);
  if (!response.ok) {
    throw new Error('Failed to fetch unacknowledged alerts');
  }
  return response.json();
}

export async function fetchAlertStats(): Promise<AlertStats> {
  const response = await fetch(`${API_CONFIG.ANALYSIS_URL}/api/alerts/stats`);
  if (!response.ok) {
    throw new Error('Failed to fetch alert stats');
  }
  return response.json();
}

export async function acknowledgeAlert(id: string, acknowledgedBy: string): Promise<Alert> {
  const response = await fetch(
    `${API_CONFIG.ANALYSIS_URL}/api/alerts/${id}/acknowledge?acknowledgedBy=${encodeURIComponent(acknowledgedBy)}`,
    { method: 'PATCH' }
  );
  if (!response.ok) {
    throw new Error('Failed to acknowledge alert');
  }
  return response.json();
}


