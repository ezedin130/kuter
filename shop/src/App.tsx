import { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LanguageProvider, useLanguage } from './i18n/LanguageContext';
import { ShopSidebar, ShopTab } from './components/ShopSidebar';
import { ShopTopbar } from './components/ShopTopbar';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { SalesPosPage } from './pages/SalesPosPage';
import { SalesHistoryPage } from './pages/SalesHistoryPage';
import { ProductsPage } from './pages/ProductsPage';
import { CategoriesPage } from './pages/CategoriesPage';
import { ReceiveStockPage } from './pages/ReceiveStockPage';
import { AdjustmentsPage } from './pages/AdjustmentsPage';
import { LendingPage } from './pages/LendingPage';
import { StockLedgerPage } from './pages/StockLedgerPage';
import { ReportsPage } from './pages/ReportsPage';
import { SubscriptionPage } from './pages/SubscriptionPage';
import { ShopSettingsPage } from './pages/ShopSettingsPage';

function ShopPortalContent() {
  const { isAuthenticated, isLoading } = useAuth();
  const { t } = useLanguage();
  const [currentTab, setCurrentTab] = useState<ShopTab>('dashboard');
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = useState<boolean>(false);

  if (isLoading) {
    return (
      <div
        style={{
          height: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'var(--canvas)',
          color: 'var(--muted)',
          gap: '12px',
        }}
      >
        <div
          style={{
            width: '40px',
            height: '40px',
            border: '3px solid var(--border)',
            borderTopColor: 'var(--teal)',
            borderRadius: '50%',
            animation: 'spin 0.8s linear infinite',
          }}
        />
        <span style={{ fontSize: '14px', fontWeight: 600 }}>{t('common.loadingPortal')}</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const getPageTitle = (tab: ShopTab): string => {
    switch (tab) {
      case 'dashboard':
        return t('nav.dashboard');
      case 'sales-pos':
        return t('nav.pos');
      case 'sales-history':
        return t('nav.salesHistory');
      case 'products':
        return t('nav.products');
      case 'categories':
        return t('nav.categories');
      case 'receive-stock':
        return t('nav.receiveStock');
      case 'adjustments':
        return t('nav.adjustments');
      case 'lending':
        return t('nav.lending');
      case 'movements':
        return t('nav.movements');
      case 'reports':
        return t('nav.reports');
      case 'subscription':
        return t('nav.subscription');
      case 'settings':
        return t('nav.settings');
      default:
        return t('dashboard.title');
    }
  };

  return (
    <div className="shop-layout">
      <ShopSidebar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        isOpen={isMobileSidebarOpen}
        onClose={() => setIsMobileSidebarOpen(false)}
      />

      <div className="shop-main">
        <ShopTopbar
          pageTitle={getPageTitle(currentTab)}
          onToggleSidebar={() => setIsMobileSidebarOpen((prev) => !prev)}
        />

        <main className="shop-content">
          {currentTab === 'dashboard' && <DashboardPage onNavigate={setCurrentTab} />}
          {currentTab === 'sales-pos' && <SalesPosPage />}
          {currentTab === 'sales-history' && <SalesHistoryPage />}
          {currentTab === 'products' && <ProductsPage />}
          {currentTab === 'categories' && <CategoriesPage />}
          {currentTab === 'receive-stock' && <ReceiveStockPage />}
          {currentTab === 'adjustments' && <AdjustmentsPage />}
          {currentTab === 'lending' && <LendingPage />}
          {currentTab === 'movements' && <StockLedgerPage />}
          {currentTab === 'reports' && <ReportsPage />}
          {currentTab === 'subscription' && <SubscriptionPage />}
          {currentTab === 'settings' && <ShopSettingsPage />}
        </main>
      </div>
    </div>
  );
}

export function App() {
  return (
    <LanguageProvider>
      <AuthProvider>
        <ShopPortalContent />
      </AuthProvider>
    </LanguageProvider>
  );
}

export default App;
