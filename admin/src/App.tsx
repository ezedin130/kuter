import { useState, useEffect, useCallback } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Sidebar, AdminTab } from './components/Sidebar';
import { Header } from './components/Header';
import { LoginPage } from './pages/LoginPage';
import { OverviewPage } from './pages/OverviewPage';
import { RequestsPage } from './pages/RequestsPage';
import { TenantsPage } from './pages/TenantsPage';
import { PlansPage } from './pages/PlansPage';
import { SubscriptionsPage } from './pages/SubscriptionsPage';
import { apiClient } from './api/client';
import { HealthResponse } from './api/types';

function AdminShell() {
  const { isAuthenticated, isLoading } = useAuth();
  const [currentTab, setCurrentTab] = useState<AdminTab>('overview');
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loadingHealth, setLoadingHealth] = useState<boolean>(true);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [pendingRequestsCount, setPendingRequestsCount] = useState<number>(0);

  const fetchHealth = useCallback(async () => {
    setLoadingHealth(true);
    setHealthError(null);
    try {
      const data = await apiClient.getHealth();
      setHealth(data);
    } catch (err: unknown) {
      setHealthError(err instanceof Error ? err.message : 'Unknown network error');
    } finally {
      setLoadingHealth(false);
    }
  }, []);

  const refreshPendingCount = useCallback(async () => {
    if (!isAuthenticated) return;
    try {
      const res = await apiClient.listOnboardingRequests({ status: 'PENDING', size: 1 });
      setPendingRequestsCount(res.totalElements || 0);
    } catch {
      // Ignore background badge error
    }
  }, [isAuthenticated]);

  useEffect(() => {
    fetchHealth();
  }, [fetchHealth]);

  useEffect(() => {
    if (isAuthenticated) {
      refreshPendingCount();
    }
  }, [isAuthenticated, refreshPendingCount]);

  if (isLoading) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'var(--canvas)',
          gap: '16px',
        }}
      >
        <div className="kuter-logo-mark" style={{ width: '40px', height: '40px' }}>
          <span />
          <span />
          <span />
        </div>
        <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--muted)' }}>
          Loading Kuter Platform Administration...
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const getPageTitle = (tab: AdminTab): string => {
    switch (tab) {
      case 'overview':
        return 'Platform Overview';
      case 'requests':
        return 'Registration Requests';
      case 'tenants':
        return 'Shops & Tenants Directory';
      case 'plans':
        return 'Subscription Plans & Tiers';
      case 'subscriptions':
        return 'Tenant Subscriptions';
      default:
        return 'Platform Administration';
    }
  };

  return (
    <div className="admin-layout">
      <Sidebar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        pendingCount={pendingRequestsCount}
      />

      <div className="admin-main">
        <Header
          title={getPageTitle(currentTab)}
          health={health}
          loading={loadingHealth}
          error={healthError}
          onRefreshHealth={fetchHealth}
        />

        <main className="admin-content">
          {currentTab === 'overview' && (
            <OverviewPage
              health={health}
              loadingHealth={loadingHealth}
              onNavigateTab={setCurrentTab}
            />
          )}

          {currentTab === 'requests' && (
            <RequestsPage onRequestsUpdated={refreshPendingCount} />
          )}

          {currentTab === 'tenants' && <TenantsPage />}

          {currentTab === 'plans' && <PlansPage />}

          {currentTab === 'subscriptions' && <SubscriptionsPage />}
        </main>
      </div>
    </div>
  );
}

export function App() {
  return (
    <AuthProvider>
      <AdminShell />
    </AuthProvider>
  );
}

export default App;
