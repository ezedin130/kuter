import React from 'react';
import { StatusBadge } from './StatusBadge';
import { HealthResponse } from '../api/types';
import { RefreshCw, LogOut, Languages, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';

interface HeaderProps {
  title: string;
  health: HealthResponse | null;
  loading: boolean;
  error: string | null;
  onRefreshHealth: () => void;
  onToggleSidebar?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  title,
  health,
  loading,
  error,
  onRefreshHealth,
  onToggleSidebar,
}) => {
  const { logout } = useAuth();
  const { language, toggleLanguage, t } = useLanguage();

  return (
    <header className="admin-topbar">
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        {onToggleSidebar && (
          <button
            type="button"
            className="mobile-menu-btn"
            onClick={onToggleSidebar}
            title={language === 'am' ? 'የአሰሳ ማውጫውን ክፈት' : 'Open Navigation Menu'}
            aria-label={language === 'am' ? 'የአሰሳ ማውጫውን ክፈት' : 'Open Navigation Menu'}
          >
            <Menu size={20} />
          </button>
        )}
        <div className="topbar-title-wrap">
          <span className="topbar-breadcrumb">{t('header.platformAdministration')}</span>
          <h1 className="topbar-title">{title}</h1>
        </div>
      </div>

      <div className="topbar-meta">
        {/* Backend Connectivity Status */}
        <div className="topbar-health-pill" title={t('header.refreshBackend')}>
          {loading ? (
            <StatusBadge status="PENDING" label={t('header.checkingApi')} size="sm" />
          ) : error ? (
            <StatusBadge status="FAILED" label={t('header.apiOffline')} size="sm" />
          ) : health ? (
            <StatusBadge
              status={health.status === 'UP' ? 'ACTIVE' : 'WARNING'}
              label={`API ${health.status} (${health.environment})`}
              size="sm"
            />
          ) : (
            <StatusBadge status="PENDING" label={t('header.connecting')} size="sm" />
          )}

          <button
            type="button"
            onClick={onRefreshHealth}
            className="btn-icon-subtle"
            title={t('header.refreshBackend')}
            disabled={loading}
          >
            <RefreshCw size={14} className={loading ? 'spin' : ''} />
          </button>
        </div>

        {/* Localization Switcher Button (Replaces old Admin Platform Badge) */}
        <button
          type="button"
          onClick={toggleLanguage}
          className="topbar-lang-btn"
          title={language === 'am' ? 'Switch interface to English' : 'የአስተዳዳሪ በይነገጹን ወደ አማርኛ ቀይር'}
          aria-label={t('header.switchLanguage')}
        >
          <Languages size={15} className="lang-icon" />
          <span className="lang-label">{language === 'am' ? 'አማርኛ' : 'English'}</span>
          <span className="lang-code-pill">{language === 'am' ? 'AM' : 'EN'}</span>
        </button>

        {/* Sign Out Button */}
        <button
          type="button"
          onClick={() => logout()}
          className="btn btn-secondary btn-sm"
          title={t('header.signOut')}
        >
          <LogOut size={14} />
          <span className="hidden-mobile">{t('header.signOut')}</span>
        </button>
      </div>
    </header>
  );
};
