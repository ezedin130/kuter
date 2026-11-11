import React from 'react';
import { StatusBadge } from './StatusBadge';
import { HealthResponse } from '../api/types';
import { RefreshCw, Shield, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

interface HeaderProps {
  title: string;
  health: HealthResponse | null;
  loading: boolean;
  error: string | null;
  onRefreshHealth: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  health,
  loading,
  error,
  onRefreshHealth,
}) => {
  const { user, logout } = useAuth();

  return (
    <header className="admin-topbar">
      <div className="topbar-title-wrap">
        <span className="topbar-breadcrumb">Platform Administration</span>
        <h1 className="topbar-title">{title}</h1>
      </div>

      <div className="topbar-meta">
        {/* Backend Connectivity Status */}
        <div className="topbar-health-pill" title="Live Spring Boot backend status">
          {loading ? (
            <StatusBadge status="PENDING" label="Checking API..." size="sm" />
          ) : error ? (
            <StatusBadge status="FAILED" label="API Offline" size="sm" />
          ) : health ? (
            <StatusBadge
              status={health.status === 'UP' ? 'ACTIVE' : 'WARNING'}
              label={`API ${health.status} (${health.environment})`}
              size="sm"
            />
          ) : (
            <StatusBadge status="PENDING" label="Connecting..." size="sm" />
          )}

          <button
            type="button"
            onClick={onRefreshHealth}
            className="btn-icon-subtle"
            title="Refresh backend connectivity check"
            disabled={loading}
          >
            <RefreshCw size={14} className={loading ? 'spin' : ''} />
          </button>
        </div>

        {/* Admin User Badge */}
        {user && (
          <div className="topbar-admin-badge" title="Authenticated platform superuser">
            <Shield size={14} style={{ color: 'var(--teal)' }} />
            <span>Admin: {user.fullName.split(' ')[0]}</span>
          </div>
        )}

        <button
          type="button"
          onClick={() => logout()}
          className="btn btn-secondary btn-sm"
          title="Sign out"
        >
          <LogOut size={14} />
          <span className="hidden-mobile">Sign Out</span>
        </button>
      </div>
    </header>
  );
};
