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
import { useLanguage } from '../i18n/LanguageContext';

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
  isOpen?: boolean;
  onClose?: () => void;
}

export const ShopSidebar: React.FC<ShopSidebarProps> = ({ currentTab, onSelectTab, isOpen, onClose }) => {
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
  const { t, language } = useLanguage();

  const handleSelect = (tab: ShopTab) => {
    onSelectTab(tab);
    if (onClose) {
      onClose();
    }
  };

  const resolveRole = (r?: string | null) => {
    if (!r) return '';
    if (language === 'am') {
      switch (r.toUpperCase()) {
        case 'OWNER':
          return t('topbar.roleOwner');
        case 'MANAGER':
          return t('topbar.roleManager');
        case 'CASHIER':
          return t('topbar.roleCashier');
        default:
          return t('topbar.roleStaff');
      }
    }
    return r;
  };

  return (
    <>
      <div
        className={`sidebar-backdrop ${isOpen ? 'open' : ''}`}
        onClick={onClose}
        aria-hidden="true"
      />
      <aside className={`shop-sidebar ${isOpen ? 'open' : ''}`}>
        <div className="sidebar-header">
          <div className="sidebar-logo-mark">K</div>
          <div>
            <div className="sidebar-brand-name">
              {language === 'am' ? 'ኩተር ሱቅ' : 'Kuter Shop'}
            </div>
          </div>
        </div>

        <div className="sidebar-shop-pill">
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Building2 size={14} color="var(--teal)" />
            <span className="shop-pill-name">{activeTenant?.tenantName || (language === 'am' ? 'የሱቅ ፖርታል' : 'Shop Portal')}</span>
          </div>
          <div className="shop-pill-role">
            <ShieldCheck size={12} color="#9eb2bc" />
            <span>{resolveRole(role)}</span>
          </div>
        </div>

        <nav className="sidebar-nav">
          <div className="sidebar-nav-section-title">{t('nav.overview')}</div>
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'dashboard' ? 'active' : ''}`}
            onClick={() => handleSelect('dashboard')}
          >
            <LayoutDashboard size={18} />
            <span>{t('nav.dashboard')}</span>
          </button>

          <div className="sidebar-nav-section-title">{t('nav.salesCheckout')}</div>
          {canProcessSales && (
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'sales-pos' ? 'active' : ''}`}
              onClick={() => handleSelect('sales-pos')}
            >
              <ShoppingCart size={18} />
              <span>{t('nav.pos')}</span>
            </button>
          )}
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'sales-history' ? 'active' : ''}`}
            onClick={() => handleSelect('sales-history')}
          >
            <Receipt size={18} />
            <span>{t('nav.salesHistory')}</span>
          </button>

          <div className="sidebar-nav-section-title">{t('nav.inventoryStock')}</div>
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'products' ? 'active' : ''}`}
            onClick={() => handleSelect('products')}
          >
            <Package size={18} />
            <span>{t('nav.products')}</span>
          </button>

          {canManageCatalog && (
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'categories' ? 'active' : ''}`}
              onClick={() => handleSelect('categories')}
            >
              <FolderTree size={18} />
              <span>{t('nav.categories')}</span>
            </button>
          )}

          {canManageInventory && (
            <>
              <button
                type="button"
                className={`sidebar-nav-item ${currentTab === 'receive-stock' ? 'active' : ''}`}
                onClick={() => handleSelect('receive-stock')}
              >
                <Truck size={18} />
                <span>{t('nav.receiveStock')}</span>
              </button>
              <button
                type="button"
                className={`sidebar-nav-item ${currentTab === 'adjustments' ? 'active' : ''}`}
                onClick={() => handleSelect('adjustments')}
              >
                <SlidersHorizontal size={18} />
                <span>{t('nav.adjustments')}</span>
              </button>
            </>
          )}

          {canViewLending && (
            <button
              type="button"
              className={`sidebar-nav-item ${currentTab === 'lending' ? 'active' : ''}`}
              onClick={() => handleSelect('lending')}
            >
              <ArrowLeftRight size={18} />
              <span>{t('nav.lending')}</span>
            </button>
          )}

          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'movements' ? 'active' : ''}`}
            onClick={() => handleSelect('movements')}
          >
            <History size={18} />
            <span>{t('nav.movements')}</span>
          </button>

          <div className="sidebar-nav-section-title">{t('nav.management')}</div>
          <button
            type="button"
            className={`sidebar-nav-item ${currentTab === 'reports' ? 'active' : ''}`}
            onClick={() => handleSelect('reports')}
          >
            <BarChart3 size={18} />
            <span>{t('nav.reports')}</span>
          </button>

          {canManageSettings && (
            <>
              <button
                type="button"
                className={`sidebar-nav-item ${currentTab === 'subscription' ? 'active' : ''}`}
                onClick={() => handleSelect('subscription')}
              >
                <CreditCard size={18} />
                <span>{t('nav.subscription')}</span>
              </button>
              <button
                type="button"
                className={`sidebar-nav-item ${currentTab === 'settings' ? 'active' : ''}`}
                onClick={() => handleSelect('settings')}
              >
                <Settings size={18} />
                <span>{t('nav.settings')}</span>
              </button>
            </>
          )}
        </nav>

        <div className="sidebar-footer">
          <div className="sidebar-user-info">
            <span className="sidebar-user-name">{user?.fullName || t('topbar.roleStaff')}</span>
            <span className="sidebar-user-email">{user?.email}</span>
          </div>
          <button
            type="button"
            className="btn-icon"
            onClick={logout}
            title={t('common.signOut')}
            style={{ color: '#8fa5ae' }}
          >
            <LogOut size={16} />
          </button>
        </div>
      </aside>
    </>
  );
};
