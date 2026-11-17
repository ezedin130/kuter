import React, { useEffect, useState } from 'react';
import {
  Package,
  AlertTriangle,
  Receipt,
  CreditCard,
  ShoppingCart,
  Truck,
  Plus,
  ArrowRight,
  RefreshCw,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { shopApi } from '../api/client';
import {
  StockBalanceResponse,
  SaleResponseDto,
  StockMovementResponse,
  TenantSubscriptionInfo,
} from '../api/types';
import { useAuth } from '../context/AuthContext';
import { ShopTab } from '../components/ShopSidebar';
import { useLanguage } from '../i18n/LanguageContext';

interface DashboardPageProps {
  onNavigate: (tab: ShopTab) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const { activeTenant, canProcessSales, canManageInventory, canManageCatalog } = useAuth();
  const { t, formatCurrency, formatDate, language } = useLanguage();

  const [isLoading, setIsLoading] = useState(true);
  const [totalProducts, setTotalProducts] = useState<number>(0);
  const [lowStockItems, setLowStockItems] = useState<StockBalanceResponse[]>([]);
  const [lowStockCount, setLowStockCount] = useState<number>(0);
  const [recentSales, setRecentSales] = useState<SaleResponseDto[]>([]);
  const [recentMovements, setRecentMovements] = useState<StockMovementResponse[]>([]);
  const [todayRevenue, setTodayRevenue] = useState<number>(0);
  const [subscription, setSubscription] = useState<TenantSubscriptionInfo | null>(null);

  const loadDashboardData = async () => {
    setIsLoading(true);
    try {
      const todayStart = new Date();
      todayStart.setHours(0, 0, 0, 0);
      const todayEnd = new Date();
      todayEnd.setHours(23, 59, 59, 999);

      const [prodRes, lowStockRes, salesRes, movRes, subRes, summaryRes] = await Promise.allSettled([
        shopApi.listProducts({ size: 1 }),
        shopApi.getLowStockReport(),
        shopApi.listSales({ size: 10 }),
        shopApi.listStockMovements({ size: 8 }),
        shopApi.getSubscription(),
        shopApi.getSalesSummary(todayStart.toISOString(), todayEnd.toISOString()),
      ]);

      if (prodRes.status === 'fulfilled') {
        setTotalProducts(prodRes.value.data?.totalElements || 0);
      }
      if (lowStockRes.status === 'fulfilled') {
        const resData = lowStockRes.value.data as any;
        const items = Array.isArray(resData) ? resData : (resData?.content || []);
        const total = Array.isArray(resData) ? resData.length : (resData?.totalElements ?? items.length);
        setLowStockItems(items);
        setLowStockCount(total);
      }
      if (salesRes.status === 'fulfilled') {
        const sales = salesRes.value.data?.content || [];
        setRecentSales(sales);
      }
      if (summaryRes.status === 'fulfilled' && summaryRes.value.data) {
        const rev = Number(summaryRes.value.data.totalRevenue ?? summaryRes.value.data.totalSalesAmount ?? 0);
        setTodayRevenue(rev);
      } else if (salesRes.status === 'fulfilled') {
        const sales = salesRes.value.data?.content || [];
        const sum = sales.reduce((acc, s) => acc + (s.status === 'COMPLETED' ? Number(s.totalAmount) : 0), 0);
        setTodayRevenue(sum);
      }
      if (movRes.status === 'fulfilled') {
        const movs = movRes.value.data?.content || [];
        setRecentMovements(movs);
      }
      if (subRes.status === 'fulfilled') {
        setSubscription(subRes.value.data || null);
      }
    } catch {
      // Error handling
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, [activeTenant?.tenantId]);

  return (
    <div>
      <PageHead
        eyebrow={language === 'am' ? 'የስራ ዳሽቦርድ' : 'Operational Dashboard'}
        title={`${activeTenant?.tenantName || t('dashboard.title')}`}
        subtitle={t('dashboard.subtitle')}
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadDashboardData}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>{t('common.refresh')}</span>
            </button>
            {canProcessSales && (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => onNavigate('sales-pos')}
              >
                <ShoppingCart size={15} />
                <span>{t('dashboard.recordSale')}</span>
              </button>
            )}
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="stats-grid" style={{ marginBottom: '24px' }}>
        <div className="stat-card" onClick={() => onNavigate('products')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-header">
            <span className="stat-label">{t('dashboard.totalProducts')}</span>
            <div className="stat-icon-wrap" style={{ color: 'var(--teal)', background: 'var(--teal-soft)' }}>
              <Package size={20} />
            </div>
          </div>
          <div className="stat-value">{isLoading ? '—' : totalProducts}</div>
          <div className="stat-meta">
            <span>{t('dashboard.totalProductsDesc')}</span>
          </div>
        </div>

        <div className="stat-card" onClick={() => onNavigate('products')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-header">
            <span className="stat-label">{t('dashboard.lowStockAlerts')}</span>
            <div
              className="stat-icon-wrap"
              style={{
                color: lowStockCount > 0 ? 'var(--danger)' : 'var(--success)',
                background: lowStockCount > 0 ? 'var(--danger-bg)' : 'var(--success-bg)',
              }}
            >
              <AlertTriangle size={20} />
            </div>
          </div>
          <div className="stat-value" style={{ color: lowStockCount > 0 ? 'var(--danger)' : 'var(--ink)' }}>
            {isLoading ? '—' : lowStockCount}
          </div>
          <div className="stat-meta">
            <span>{lowStockCount > 0 ? t('dashboard.lowStockAlertsDesc') : (language === 'am' ? 'ሁሉም እቃዎች በቂ ናቸው' : 'All stocks healthy')}</span>
          </div>
        </div>

        <div className="stat-card" onClick={() => onNavigate('sales-history')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-header">
            <span className="stat-label">{t('dashboard.todaySales')}</span>
            <div className="stat-icon-wrap" style={{ color: 'var(--success)', background: 'var(--success-bg)' }}>
              <Receipt size={20} />
            </div>
          </div>
          <div className="stat-value">{isLoading ? '—' : formatCurrency(todayRevenue)}</div>
          <div className="stat-meta">
            <span>{t('dashboard.todaySalesDesc')}</span>
          </div>
        </div>

        <div className="stat-card" onClick={() => onNavigate('subscription')} style={{ cursor: 'pointer' }}>
          <div className="stat-card-header">
            <span className="stat-label">{t('dashboard.activePlan')}</span>
            <div className="stat-icon-wrap" style={{ color: 'var(--blue)', background: '#eaf1f6' }}>
              <CreditCard size={20} />
            </div>
          </div>
          <div className="stat-value" style={{ fontSize: '20px' }}>
            {isLoading ? '—' : subscription?.planCode || subscription?.planName || 'STANDARD'}
          </div>
          <div className="stat-meta">
            <span>
              {subscription?.status ? <StatusBadge status={subscription.status} /> : (language === 'am' ? 'ንቁ ደንበኝነት' : 'Active subscription')}
            </span>
          </div>
        </div>
      </div>

      {/* Main Grid: Quick Actions + Recent Sales + Recent Movements */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '24px' }}>
        {/* Quick Operations Card */}
        <div className="card">
          <div className="card-header">
            <h3>{t('dashboard.quickActions')}</h3>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {canProcessSales && (
              <button
                type="button"
                className="btn btn-secondary"
                style={{ justifyContent: 'space-between', padding: '14px 18px', textAlign: 'left', minHeight: 'auto' }}
                onClick={() => onNavigate('sales-pos')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <ShoppingCart size={18} color="var(--teal)" />
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{t('dashboard.recordSale')}</div>
                    <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                      {language === 'am' ? 'የህንጻ እቃዎችን ፈጣን ሽያጭ መመዝገቢያ' : 'Sell building materials & generate receipt'}
                    </div>
                  </div>
                </div>
                <ArrowRight size={16} color="var(--muted)" />
              </button>
            )}

            {canManageInventory && (
              <button
                type="button"
                className="btn btn-secondary"
                style={{ justifyContent: 'space-between', padding: '14px 18px', textAlign: 'left', minHeight: 'auto' }}
                onClick={() => onNavigate('receive-stock')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Truck size={18} color="var(--teal)" />
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{t('dashboard.receiveStock')}</div>
                    <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                      {language === 'am' ? 'ከአቅራቢዎች የገቡ እቃዎችን ወደ ክምችት ማስገቢያ' : 'Record supplier delivery with cost valuation'}
                    </div>
                  </div>
                </div>
                <ArrowRight size={16} color="var(--muted)" />
              </button>
            )}

            {canManageInventory && (
              <button
                type="button"
                className="btn btn-secondary"
                style={{ justifyContent: 'space-between', padding: '14px 18px', textAlign: 'left', minHeight: 'auto' }}
                onClick={() => onNavigate('adjustments')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <AlertTriangle size={18} color="var(--warning)" />
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{t('dashboard.adjustInventory')}</div>
                    <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                      {language === 'am' ? 'የእቃ ቆጠራ ልዩነት ወይም ብልሽት ማስተካከያ' : 'Correct discrepancies, damage, or shrinkage'}
                    </div>
                  </div>
                </div>
                <ArrowRight size={16} color="var(--muted)" />
              </button>
            )}

            {canManageCatalog && (
              <button
                type="button"
                className="btn btn-secondary"
                style={{ justifyContent: 'space-between', padding: '14px 18px', textAlign: 'left', minHeight: 'auto' }}
                onClick={() => onNavigate('products')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Plus size={18} color="var(--teal)" />
                  <div>
                    <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{t('products.addProduct')}</div>
                    <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                      {language === 'am' ? 'አዳዲስ የህንጻ እቃዎችን ወደ ካታሎግ መጨመሪያ' : 'Define new inventory items & dual-UOM conversions'}
                    </div>
                  </div>
                </div>
                <ArrowRight size={16} color="var(--muted)" />
              </button>
            )}
          </div>
        </div>

        {/* Low Stock Warning Card */}
        {lowStockItems.length > 0 && (
          <div className="card">
            <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--danger)' }}>
                <AlertTriangle size={18} />
                <span>{t('dashboard.lowStockAlerts')} ({lowStockCount})</span>
              </h3>
              <button
                type="button"
                className="btn btn-link btn-sm"
                onClick={() => onNavigate('products')}
              >
                {t('dashboard.viewAll')}
              </button>
            </div>
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('products.name')}</th>
                    <th>{t('dashboard.quantity')}</th>
                    <th>{t('common.status')}</th>
                  </tr>
                </thead>
                <tbody>
                  {lowStockItems.slice(0, 5).map((item) => (
                    <tr key={item.productId}>
                      <td>
                        <div style={{ fontWeight: 600 }}>{item.productName}</div>
                        <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                      </td>
                      <td style={{ fontWeight: 700, color: 'var(--danger)' }}>
                        {item.quantityOnHand} {item.baseUnitCode}
                      </td>
                      <td>
                        <StatusBadge status="LOW_STOCK" variant="stock" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* Recent Sales Table */}
        <div className="card">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>{t('dashboard.recentSales')}</h3>
            <button
              type="button"
              className="btn btn-link btn-sm"
              onClick={() => onNavigate('sales-history')}
            >
              {t('dashboard.viewAll')}
            </button>
          </div>
          {recentSales.length === 0 ? (
            <div style={{ padding: '24px', textAlign: 'center', color: 'var(--muted)' }}>
              {t('dashboard.noRecentSales')}
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('dashboard.receiptNumber')}</th>
                    <th>{t('dashboard.customer')}</th>
                    <th>{t('dashboard.totalAmount')}</th>
                    <th>{t('common.status')}</th>
                  </tr>
                </thead>
                <tbody>
                  {recentSales.slice(0, 5).map((s) => (
                    <tr key={s.id}>
                      <td className="mono-text" style={{ fontWeight: 600 }}>
                        {s.invoiceNumber || s.id.slice(0, 8)}
                      </td>
                      <td>{s.customerName || (language === 'am' ? 'ደንበኛ' : 'Walk-in')}</td>
                      <td style={{ fontWeight: 700 }}>{formatCurrency(Number(s.totalAmount))}</td>
                      <td>
                        <StatusBadge status={s.status} variant="sale" />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Recent Stock Movements Table */}
        <div className="card">
          <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3>{t('dashboard.recentMovements')}</h3>
            <button
              type="button"
              className="btn btn-link btn-sm"
              onClick={() => onNavigate('movements')}
            >
              {t('dashboard.viewAll')}
            </button>
          </div>
          {recentMovements.length === 0 ? (
            <div style={{ padding: '24px', textAlign: 'center', color: 'var(--muted)' }}>
              {t('dashboard.noRecentMovements')}
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table">
                <thead>
                  <tr>
                    <th>{t('dashboard.movementType')}</th>
                    <th>{t('dashboard.quantity')}</th>
                    <th>{t('common.date')}</th>
                  </tr>
                </thead>
                <tbody>
                  {recentMovements.slice(0, 5).map((m) => (
                    <tr key={m.id}>
                      <td>
                        <span style={{ fontWeight: 600 }}>{m.movementType}</span>
                        {m.productName && (
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>{m.productName}</div>
                        )}
                      </td>
                      <td
                        style={{
                          fontWeight: 700,
                          color: Number(m.quantityChange) >= 0 ? 'var(--success)' : 'var(--danger)',
                        }}
                      >
                        {Number(m.quantityChange) > 0 ? `+${m.quantityChange}` : m.quantityChange} {m.baseUnitCode}
                      </td>
                      <td style={{ fontSize: '12px', color: 'var(--muted)' }}>{formatDate(m.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
