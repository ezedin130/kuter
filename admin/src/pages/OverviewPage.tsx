import React, { useState, useEffect } from 'react';
import {
  Building2,
  ClipboardCheck,
  CreditCard,
  TrendingUp,
  Server,
  Database,
  Shield,
  Layers,
  ArrowRight,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';
import { apiClient } from '../api/client';
import { PlatformOverviewMetrics, HealthResponse } from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { AdminTab } from '../components/Sidebar';
import { useLanguage } from '../i18n/LanguageContext';

interface OverviewPageProps {
  health: HealthResponse | null;
  loadingHealth: boolean;
  onNavigateTab: (tab: AdminTab) => void;
}

export const OverviewPage: React.FC<OverviewPageProps> = ({
  health,
  loadingHealth,
  onNavigateTab,
}) => {
  const { t, formatCurrency, language } = useLanguage();
  const [metrics, setMetrics] = useState<PlatformOverviewMetrics | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchOverview = async () => {
    setLoading(true);
    setError(null);
    try {
      const metricsData = await apiClient.getPlatformOverview();
      setMetrics(metricsData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('overview.failedMetrics'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOverview();
  }, []);

  return (
    <div>
      <PageHead
        eyebrow={t('overview.eyebrow')}
        title={t('overview.title')}
        description={t('overview.description')}
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchOverview}
              disabled={loading}
            >
              <RefreshCw size={14} className={loading ? 'spin' : ''} />
              <span>{t('overview.refreshMetrics')}</span>
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={() => onNavigateTab('requests')}
            >
              {t('overview.reviewRequests')} <ArrowRight size={14} />
            </button>
          </div>
        }
      />

      {error && (
        <div className="alert-banner alert-danger" style={{ marginBottom: '20px' }}>
          <AlertTriangle size={18} />
          <div style={{ flex: 1 }}>{error}</div>
          <button type="button" className="btn btn-secondary btn-sm" onClick={fetchOverview}>
            {t('common.retry')}
          </button>
        </div>
      )}

      {/* KPI Metric Cards */}
      <div className="metric-grid">
        {/* Total Tenants */}
        <div className="metric-card" onClick={() => onNavigateTab('tenants')} role="button" tabIndex={0}>
          <div className="metric-top-row">
            <span className="metric-label">{t('overview.totalActiveShops')}</span>
            <div className="metric-icon-wrap">
              <Building2 size={20} />
            </div>
          </div>
          <div className="metric-value">
            {loading ? '—' : metrics?.totalTenants ?? 0}
          </div>
          <div className="metric-meta positive">
            <CheckCircle2 size={13} />
            <span>
              {loading
                ? '...'
                : language === 'am'
                ? `${metrics?.activeTenants ?? 0} ንቁ፣ ${metrics?.suspendedTenants ?? 0} የታገዱ`
                : `${metrics?.activeTenants ?? 0} active, ${metrics?.suspendedTenants ?? 0} suspended`}
            </span>
          </div>
        </div>

        {/* Pending Requests */}
        <div className="metric-card" onClick={() => onNavigateTab('requests')} role="button" tabIndex={0}>
          <div className="metric-top-row">
            <span className="metric-label">{t('overview.pendingRegistrations')}</span>
            <div className="metric-icon-wrap" style={{ color: 'var(--warning)', backgroundColor: 'var(--warning-bg)' }}>
              <ClipboardCheck size={20} />
            </div>
          </div>
          <div className="metric-value" style={{ color: (metrics?.pendingOnboardingRequests ?? 0) > 0 ? 'var(--warning)' : 'var(--ink)' }}>
            {loading ? '—' : metrics?.pendingOnboardingRequests ?? 0}
          </div>
          <div className="metric-meta warning">
            <span>
              {loading
                ? '...'
                : (metrics?.pendingOnboardingRequests ?? 0) > 0
                ? language === 'am'
                  ? 'የአስተዳዳሪ ማረጋገጫ የሚጠብቁ'
                  : 'Awaiting administrator review'
                : language === 'am'
                ? 'የሚጠብቅ የለም'
                : 'Queue clear'}
            </span>
          </div>
        </div>

        {/* Active Subscriptions */}
        <div className="metric-card" onClick={() => onNavigateTab('subscriptions')} role="button" tabIndex={0}>
          <div className="metric-top-row">
            <span className="metric-label">{t('overview.activeSubscriptions')}</span>
            <div className="metric-icon-wrap" style={{ color: '#1e557b', backgroundColor: '#e5f1f8' }}>
              <Layers size={20} />
            </div>
          </div>
          <div className="metric-value">
            {loading ? '—' : metrics?.activeSubscriptions ?? 0}
          </div>
          <div className="metric-meta">
            <span>
              {loading
                ? '...'
                : language === 'am'
                ? `${metrics?.trialingSubscriptions ?? 0} በሙከራ ላይ፣ ${metrics?.expiredSubscriptions ?? 0} ጊዜው ያለፈበት`
                : `${metrics?.trialingSubscriptions ?? 0} trialing, ${metrics?.expiredSubscriptions ?? 0} expired`}
            </span>
          </div>
        </div>

        {/* Estimated MRR */}
        <div className="metric-card" onClick={() => onNavigateTab('subscriptions')} role="button" tabIndex={0}>
          <div className="metric-top-row">
            <span className="metric-label">{t('overview.monthlyRevenue')}</span>
            <div className="metric-icon-wrap" style={{ color: 'var(--success)', backgroundColor: 'var(--success-bg)' }}>
              <TrendingUp size={20} />
            </div>
          </div>
          <div className="metric-value">
            {loading ? '—' : formatCurrency(metrics?.estimatedMonthlyRevenue)}
          </div>
          <div className="metric-meta positive">
            <span>{t('overview.monthlyRevenueDesc')}</span>
          </div>
        </div>
      </div>

      {/* Main Two-Column Content Grid */}
      <div className="review-layout">
        {/* Left Column: Backend Health & Quick Nav Cards */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* Backend Connectivity Status */}
          <div className="card">
            <div className="detail-card-head">
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Server size={18} style={{ color: 'var(--teal)' }} />
                {language === 'am' ? 'የስፕሪንግ ቡት ኤፒአይ እና የዳታቤዝ ሁኔታ' : 'Spring Boot REST API & Database State'}
              </h3>
              {loadingHealth ? (
                <StatusBadge status="PENDING" label={t('common.loading')} size="sm" />
              ) : health?.status === 'UP' ? (
                <StatusBadge status="ACTIVE" label={language === 'am' ? 'በጥሩ ሁኔታ ላይ (UP)' : 'Healthy (UP)'} size="sm" />
              ) : (
                <StatusBadge status="FAILED" label={language === 'am' ? 'የተስተጓጎለ' : 'Degraded'} size="sm" />
              )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px' }}>
              <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '12px' }}>
                <div style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                  {language === 'am' ? 'ዋና ዳታቤዝ' : 'Database Engine'}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--ink)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Database size={15} style={{ color: 'var(--success)' }} />
                  {health?.database || 'PostgreSQL 18'}
                </div>
                <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '2px' }}>
                  {language === 'am' ? 'በፍላይዌይ (Flyway) የተደራጀ' : 'Managed via Flyway V1–V7'}
                </div>
              </div>

              <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '12px' }}>
                <div style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                  {language === 'am' ? 'የስራ አካባቢ' : 'Active Profile'}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--ink)', marginTop: '4px' }}>
                  {health?.environment || 'dev'}
                </div>
                <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '2px' }}>
                  Spring Boot 3.4.3 / Java 21
                </div>
              </div>

              <div style={{ border: '1px solid var(--border)', borderRadius: 'var(--radius-sm)', padding: '12px' }}>
                <div style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                  {language === 'am' ? 'የኤፒአይ ስሪት' : 'API Contract State'}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--teal)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Shield size={15} />
                  v1.0.0
                </div>
                <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '2px' }}>
                  {language === 'am' ? '10 የጎራ አገልግሎቶች' : '10 domain endpoint groups'}
                </div>
              </div>
            </div>
          </div>

          {/* Platform Administration Direct Shortcuts */}
          <div className="card">
            <div className="detail-card-head">
              <h3>{t('overview.quickShortcuts')}</h3>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '12px' }}>
              {[
                {
                  tab: 'requests' as AdminTab,
                  title: t('sidebar.requests'),
                  desc: t('requests.description'),
                  icon: <ClipboardCheck size={18} style={{ color: 'var(--warning)' }} />,
                },
                {
                  tab: 'tenants' as AdminTab,
                  title: t('sidebar.tenants'),
                  desc: t('tenants.description'),
                  icon: <Building2 size={18} style={{ color: 'var(--teal)' }} />,
                },
                {
                  tab: 'plans' as AdminTab,
                  title: t('sidebar.plans'),
                  desc: t('plans.description'),
                  icon: <CreditCard size={18} style={{ color: '#1e557b' }} />,
                },
              ].map((item) => (
                <div
                  key={item.tab}
                  onClick={() => onNavigateTab(item.tab)}
                  style={{
                    padding: '14px',
                    border: '1px solid var(--border)',
                    borderRadius: 'var(--radius-sm)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                  onMouseEnter={(e) => {
                    e.currentTarget.style.borderColor = 'var(--teal)';
                    e.currentTarget.style.backgroundColor = '#fafcfc';
                  }}
                  onMouseLeave={(e) => {
                    e.currentTarget.style.borderColor = 'var(--border)';
                    e.currentTarget.style.backgroundColor = 'transparent';
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px' }}>
                    {item.icon}
                    <strong style={{ fontSize: '13.5px', color: 'var(--ink)' }}>{item.title}</strong>
                  </div>
                  <p style={{ margin: 0, fontSize: '12.5px', color: 'var(--muted)', lineHeight: 1.4 }}>
                    {item.desc}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Column: Platform System & Service Status */}
        <div className="card">
          <div className="detail-card-head">
            <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Server size={16} style={{ color: 'var(--teal)' }} />
              {t('overview.infrastructureHealth')}
            </h3>
            <span className={`badge ${health?.status === 'UP' ? 'badge-success' : 'badge-warning'}`}>
              {health?.status === 'UP' ? t('overview.serviceOnline') : t('overview.serviceIssue')}
            </span>
          </div>

          <div style={{ padding: '16px 0', display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', background: 'var(--surface-subtle, #f8fafc)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Database size={18} style={{ color: 'var(--teal)' }} />
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--ink)' }}>{t('overview.postgresDatabase')}</div>
                  <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>PostgreSQL Store</div>
                </div>
              </div>
              <span className="badge badge-success" style={{ fontSize: '11px' }}>
                {health?.database ? t('status.CONNECTED') : t('status.ACTIVE')}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', background: 'var(--surface-subtle, #f8fafc)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Server size={18} style={{ color: '#1e557b' }} />
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--ink)' }}>{t('overview.springBackend')}</div>
                  <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>Spring Boot 3.4 API Engine</div>
                </div>
              </div>
              <span className="badge badge-success" style={{ fontSize: '11px' }}>
                {health?.environment ? t('status.ACTIVE') : t('status.UP')}
              </span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 16px', background: 'var(--surface-subtle, #f8fafc)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border)' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Layers size={18} style={{ color: 'var(--amber)' }} />
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--ink)' }}>{t('overview.recentRequests')}</div>
                  <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>{t('overview.recentRequestsDesc')}</div>
                </div>
              </div>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                style={{ fontSize: '11.5px', minHeight: '26px', padding: '0 8px' }}
                onClick={() => onNavigateTab('requests')}
              >
                {metrics?.pendingOnboardingRequests ?? 0} {t('common.pending')} <ArrowRight size={12} />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
