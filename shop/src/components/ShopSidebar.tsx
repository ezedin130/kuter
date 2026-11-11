import React from 'react';
import {
  LayoutDashboard,
  ShoppingCart,
  Receipt,
  Package,
  FolderTree,
  Truck,
  SlidersHorizontal,
  History,
  BarChart3,
  CreditCard,
  Settings,
  LogOut,
  Building2,
  ShieldCheck,
  ArrowLeftRight,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export type ShopTab =
  | 'dashboard'
  | 'sales-pos'
  | 'sales-history'
  | 'products'
  | 'categories'
  | 'receive-stock'
  | 'adjustments'
  | 'lending'
  | 'movements'
  | 'reports'
  | 'subscription'
  | 'settings';

interface ShopSidebarProps {
  currentTab: ShopTab;
  onSelectTab: (tab: ShopTab) => void;
}

export const ShopSidebar: React.FC<ShopSidebarProps> = ({ currentTab, onSelectTab }) => {
  const {
    user,
    activeTenant,
    role,
    logout,
    canManageCatalog,
    canManageInventory,
    canProcessSales,
    canManageSettings,
    canViewLending,
  } = useAuth();

  return (
    <aside className="shop-sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo-mark">K</div>
        <div>
          <div className="sidebar-brand-name">Kuter Shop</div>
        </div>
      </div>

      <div className="sidebar-shop-pill">
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Building2 size={14} color="var(--teal)" />
          <span className="shop-pill-name">{activeTenant?.tenantName || 'Shop Portal'}</span>
        </div>
        <div className="shop-pill-role">
          <ShieldCheck size={12} color="#9eb2bc" />
          <span>{role || 'STAFF'}</span>
        </div>
      </div>

      <nav className="sidebar-nav">
        <div className="sidebar-nav-section-title">Overview</div>
        <button
          type="button"
          className={`sidebar-nav-item ${currentTab === 'dashboard' ? 'active' : ''}`}
          onClick={() => onSelectTab('dashboard')}
        >
          <LayoutDashboard size={18} />
          <span>Dashboard</span>
        </button>

        <div className="sidebar-nav-section-title">Sales & Checkout</div>
        {canProcessSales && (
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'sales-pos' ? 'active' : ''}`}
            onClick={() => onSelectTab('sales-pos')}
          >
            <ShoppingCart size={18} />
            <span>Counter POS</span>
          </button>
        )}
        <button
          type="button"
          className={`sidebar-nav-item ${currentTab === 'sales-history' ? 'active' : ''}`}
          onClick={() => onSelectTab('sales-history')}
        >
          <Receipt size={18} />
          <span>Sales History</span>
        </button>

        <div className="sidebar-nav-section-title">Catalog & Inventory</div>
        <button
          type="button"
          className={`sidebar-nav-item ${currentTab === 'products' ? 'active' : ''}`}
          onClick={() => onSelectTab('products')}
        >
          <Package size={18} />
          <span>Products</span>
        </button>

        {canManageCatalog && (
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'categories' ? 'active' : ''}`}
            onClick={() => onSelectTab('categories')}
          >
            <FolderTree size={18} />
            <span>Categories</span>
          </button>
        )}

        {canManageInventory && (
          <>
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'receive-stock' ? 'active' : ''}`}
              onClick={() => onSelectTab('receive-stock')}
            >
              <Truck size={18} />
              <span>Receive Stock</span>
            </button>
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'adjustments' ? 'active' : ''}`}
              onClick={() => onSelectTab('adjustments')}
            >
              <SlidersHorizontal size={18} />
              <span>Adjustments</span>
            </button>
          </>
        )}

        {canViewLending && (
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'lending' ? 'active' : ''}`}
            onClick={() => onSelectTab('lending')}
          >
            <ArrowLeftRight size={18} />
            <span>Lending & Borrowing</span>
          </button>
        )}

        <button
          type="button"
          className={`sidebar-nav-item ${currentTab === 'movements' ? 'active' : ''}`}
          onClick={() => onSelectTab('movements')}
        >
          <History size={18} />
          <span>Stock Ledger</span>
        </button>

        <div className="sidebar-nav-section-title">Management</div>
        <button
          type="button"
          className={`sidebar-nav-item ${currentTab === 'reports' ? 'active' : ''}`}
          onClick={() => onSelectTab('reports')}
        >
          <BarChart3 size={18} />
          <span>Reports</span>
        </button>

        {canManageSettings && (
          <>
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'subscription' ? 'active' : ''}`}
              onClick={() => onSelectTab('subscription')}
            >
              <CreditCard size={18} />
              <span>Subscription</span>
            </button>
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'settings' ? 'active' : ''}`}
              onClick={() => onSelectTab('settings')}
            >
              <Settings size={18} />
              <span>Shop Settings</span>
            </button>
          </>
        )}
      </nav>

      <div className="sidebar-footer">
        <div className="sidebar-user-info">
          <span className="sidebar-user-name">{user?.fullName || 'Staff Member'}</span>
          <span className="sidebar-user-email">{user?.email}</span>
        </div>
        <button
          type="button"
          className="btn-icon"
          onClick={logout}
          title="Sign out"
          style={{ color: '#8fa5ae' }}
        >
          <LogOut size={16} />
        </button>
      </div>
    </aside>
  );
};
