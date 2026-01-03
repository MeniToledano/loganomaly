import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchAlerts, fetchAlertStats, acknowledgeAlert } from '../api/alerts';
import type { Alert, AlertStats } from '../types';
import './Dashboard.css';

export default function Dashboard() {
  const [alerts, setAlerts] = useState<Alert[]>([]);
  const [stats, setStats] = useState<AlertStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<'all' | 'unacknowledged'>('all');

  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const loadData = useCallback(async () => {
    try {
      const [alertsData, statsData] = await Promise.all([
        fetchAlerts(),
        fetchAlertStats(),
      ]);
      setAlerts(alertsData);
      setStats(statsData);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    const interval = setInterval(loadData, 10000); // Refresh every 10 seconds
    return () => clearInterval(interval);
  }, [loadData]);

  const handleAcknowledge = async (alertId: string) => {
    if (!user) return;
    try {
      await acknowledgeAlert(alertId, user.username);
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to acknowledge alert');
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const filteredAlerts = filter === 'unacknowledged'
    ? alerts.filter((a) => !a.acknowledged)
    : alerts;

  const getSeverityClass = (severity: string) => {
    switch (severity) {
      case 'CRITICAL': return 'severity-critical';
      case 'WARNING': return 'severity-warning';
      default: return 'severity-info';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'HIGH_ERROR_RATE':
        return (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
          </svg>
        );
      case 'KEYWORD_MATCH':
        return (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.35-4.35" />
          </svg>
        );
      default:
        return (
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" />
            <line x1="12" y1="9" x2="12" y2="13" />
            <line x1="12" y1="17" x2="12.01" y2="17" />
          </svg>
        );
    }
  };

  const formatTime = (isoString: string) => {
    return new Date(isoString).toLocaleString();
  };

  if (loading) {
    return (
      <div className="dashboard-loading">
        <div className="loading-spinner" />
        <p>Loading alerts...</p>
      </div>
    );
  }

  return (
    <div className="dashboard-container">
      <header className="dashboard-header">
        <div className="header-left">
          <div className="logo">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 2L2 7l10 5 10-5-10-5z" />
              <path d="M2 17l10 5 10-5" />
              <path d="M2 12l10 5 10-5" />
            </svg>
          </div>
          <h1>LogAnomaly</h1>
        </div>
        <div className="header-right">
          <span className="user-info">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            {user?.username}
          </span>
          <button className="logout-btn" onClick={handleLogout}>
            Sign Out
          </button>
        </div>
      </header>

      <main className="dashboard-main">
        {error && (
          <div className="error-banner">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            {error}
          </div>
        )}

        <section className="stats-section">
          <div className="stat-card">
            <div className="stat-icon total">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z" />
                <polyline points="14,2 14,8 20,8" />
              </svg>
            </div>
            <div className="stat-content">
              <span className="stat-value">{stats?.totalAlerts ?? 0}</span>
              <span className="stat-label">Total Alerts</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon pending">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10" />
                <polyline points="12,6 12,12 16,14" />
              </svg>
            </div>
            <div className="stat-content">
              <span className="stat-value">{stats?.unacknowledgedAlerts ?? 0}</span>
              <span className="stat-label">Pending Review</span>
            </div>
          </div>

          <div className="stat-card">
            <div className="stat-icon resolved">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22,4 12,14.01 9,11.01" />
              </svg>
            </div>
            <div className="stat-content">
              <span className="stat-value">
                {(stats?.totalAlerts ?? 0) - (stats?.unacknowledgedAlerts ?? 0)}
              </span>
              <span className="stat-label">Acknowledged</span>
            </div>
          </div>
        </section>

        <section className="alerts-section">
          <div className="alerts-header">
            <h2>Alerts</h2>
            <div className="filter-tabs">
              <button
                className={`filter-tab ${filter === 'all' ? 'active' : ''}`}
                onClick={() => setFilter('all')}
              >
                All
              </button>
              <button
                className={`filter-tab ${filter === 'unacknowledged' ? 'active' : ''}`}
                onClick={() => setFilter('unacknowledged')}
              >
                Unacknowledged
              </button>
            </div>
            <button className="refresh-btn" onClick={loadData}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M23 4v6h-6" />
                <path d="M1 20v-6h6" />
                <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" />
              </svg>
              Refresh
            </button>
          </div>

          {filteredAlerts.length === 0 ? (
            <div className="empty-state">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <h3>No alerts to display</h3>
              <p>Everything looks good! No anomalies detected.</p>
            </div>
          ) : (
            <div className="alerts-table-wrapper">
              <table className="alerts-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Severity</th>
                    <th>Service</th>
                    <th>Message</th>
                    <th>Detected At</th>
                    <th>Status</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAlerts.map((alert) => (
                    <tr key={alert.id} className={alert.acknowledged ? 'acknowledged' : ''}>
                      <td>
                        <div className="type-cell">
                          <span className="type-icon">{getTypeIcon(alert.type)}</span>
                          <span>{alert.type.replace(/_/g, ' ')}</span>
                        </div>
                      </td>
                      <td>
                        <span className={`severity-badge ${getSeverityClass(alert.severity)}`}>
                          {alert.severity}
                        </span>
                      </td>
                      <td>
                        <code className="service-name">{alert.service}</code>
                      </td>
                      <td className="message-cell">{alert.message}</td>
                      <td className="time-cell">{formatTime(alert.detectedAt)}</td>
                      <td>
                        {alert.acknowledged ? (
                          <span className="status-acknowledged">
                            ✓ {alert.acknowledgedBy}
                          </span>
                        ) : (
                          <span className="status-pending">Pending</span>
                        )}
                      </td>
                      <td>
                        {!alert.acknowledged && (
                          <button
                            className="acknowledge-btn"
                            onClick={() => handleAcknowledge(alert.id)}
                          >
                            Acknowledge
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}


