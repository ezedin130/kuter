import { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
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
  const [currentTab, setCurrentTab] = useState<ShopTab>('dashboard');

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
        <span style={{ fontSize: '14px', fontWeight: 600 }}>Loading Kuter Shop Portal...</span>
      </div>
    );
  }

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const getPageTitle = (tab: ShopTab): string => {
    switch (tab) {
      case 'dashboard':
        return 'Shop Dashboard';
      case 'sales-pos':
        return 'Counter Point of Sale';
      case 'sales-history':
        return 'Sales History & Records';
      case 'products':
        return 'Products & Inventory Catalog';
      case 'categories':
        return 'Product Categories';
      case 'receive-stock':
        return 'Stock Receiving';
      case 'adjustments':
        return 'Stock Adjustments';
      case 'lending':
        return 'Inter-Shop Lending & Borrowing';
      case 'movements':
        return 'Stock Movement Ledger';
      case 'reports':
        return 'Operational Reports';
      case 'subscription':
        return 'Subscription & Quotas';
      case 'settings':
        return 'Shop Settings';
      default:
        return 'Shop Portal';
    }
  };

  return (
    <div className="shop-layout">
      <ShopSidebar currentTab={currentTab} onSelectTab={setCurrentTab} />

      <div className="shop-main">
        <ShopTopbar pageTitle={getPageTitle(currentTab)} />

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
    <AuthProvider>
      <ShopPortalContent />
    </AuthProvider>
  );
}

export default App;
