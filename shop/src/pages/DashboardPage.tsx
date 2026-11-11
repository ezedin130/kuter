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

interface DashboardPageProps {
  onNavigate: (tab: ShopTab) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const { activeTenant, canProcessSales, canManageInventory, canManageCatalog } = useAuth();

  const [isLoading, setIsLoading] = useState(true);
  const [totalProducts, setTotalProducts] = useState<number>(0);
  const [lowStockItems, setLowStockItems] = useState<StockBalanceResponse[]>([]);
  const [recentSales, setRecentSales] = useState<SaleResponseDto[]>([]);
  const [recentMovements, setRecentMovements] = useState<StockMovementResponse[]>([]);
  const [todayRevenue, setTodayRevenue] = useState<number>(0);
  const [subscription, setSubscription] = useState<TenantSubscriptionInfo | null>(null);

  const loadDashboardData = async () => {
    setIsLoading(true);
    try {
      const [prodRes, lowStockRes, salesRes, movRes, subRes] = await Promise.allSettled([
        shopApi.listProducts({ size: 1 }),
        shopApi.getLowStockReport(),
        shopApi.listSales({ size: 5 }),
        shopApi.listStockMovements({ size: 5 }),
        shopApi.getSubscription(),
      ]);

      if (prodRes.status === 'fulfilled') {
        setTotalProducts(prodRes.value.data?.totalElements || 0);
      }
      if (lowStockRes.status === 'fulfilled') {
        setLowStockItems(lowStockRes.value.data || []);
      }
      if (salesRes.status === 'fulfilled') {
        const sales = salesRes.value.data?.content || [];
        setRecentSales(sales);
        const sum = sales.reduce((acc, s) => acc + (s.status === 'COMPLETED' ? Number(s.totalAmount) : 0), 0);
        setTodayRevenue(sum);
      }
      if (movRes.status === 'fulfilled') {
        setRecentMovements(movRes.value.data?.content || []);
      }
      if (subRes.status === 'fulfilled') {
        setSubscription(subRes.value.data || null);
      }
    } catch (err) {
      console.error('Error loading dashboard data:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, [activeTenant?.tenantId]);

  const formatCurrency = (val: number) => {
    return Number(val || 0).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <div>
      <PageHead
        eyebrow="Shop Overview"
        title={activeTenant?.tenantName || 'Shop Dashboard'}
        subtitle={`Live operations, inventory status, and sales metrics for ${activeTenant?.tenantCode || 'tenant'}`}
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadDashboardData}
              disabled={isLoading}
              title="Refresh Metrics"
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            {canProcessSales && (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => onNavigate('sales-pos')}
              >
                <ShoppingCart size={14} />
                <span>New Sale (POS)</span>
              </button>
            )}
          </div>
        }
      />

      {/* KPI Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-card-header">
            <span>Total Catalog Products</span>
            <div className="stat-card-icon" style={{ background: '#eef6f9', color: 'var(--blue)' }}>
              <Package size={20} />
            </div>
          </div>
          <div className="stat-card-value">{isLoading ? '...' : totalProducts}</div>
          <div className="stat-card-footer">
            <span>Building materials & hardware items</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span>Low Stock Alerts</span>
            <div
              className="stat-card-icon"
              style={{
                background: lowStockItems.length > 0 ? 'var(--warning-bg)' : 'var(--success-bg)',
                color: lowStockItems.length > 0 ? 'var(--warning)' : 'var(--success)',
              }}
            >
              <AlertTriangle size={20} />
            </div>
          </div>
          <div
            className="stat-card-value"
            style={{ color: lowStockItems.length > 0 ? 'var(--warning)' : 'var(--ink)' }}
          >
            {isLoading ? '...' : lowStockItems.length}
          </div>
          <div className="stat-card-footer">
            <span>At or below reorder threshold</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span>Recent Sales Volume</span>
            <div className="stat-card-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)' }}>
              <Receipt size={20} />
            </div>
          </div>
          <div className="stat-card-value">{isLoading ? '...' : formatCurrency(todayRevenue)}</div>
          <div className="stat-card-footer">
            <span>{recentSales.length} recent transactions recorded</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-card-header">
            <span>Subscription Tier</span>
            <div className="stat-card-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
              <CreditCard size={20} />
            </div>
          </div>
          <div className="stat-card-value" style={{ fontSize: '20px', textTransform: 'capitalize' }}>
            {isLoading ? '...' : subscription?.planName || 'Active Plan'}
          </div>
          <div className="stat-card-footer">
            <span>
              {subscription?.daysRemaining != null
                ? `${subscription.daysRemaining} days remaining in period`
                : 'Active multi-tenant license'}
            </span>
          </div>
        </div>
      </div>

      {/* Subscription Quota Usage Card */}
      {subscription && subscription.limits && (
        <div
          className="card"
          style={{
            padding: '16px 20px',
            background: 'linear-gradient(to right, #ffffff, #f7faf9)',
            marginBottom: '24px',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <CreditCard size={16} color="var(--teal)" />
              <strong style={{ fontSize: '13.5px', color: 'var(--ink)' }}>Plan Quota Usage: {subscription.planName}</strong>
            </div>
            <span className="badge badge-success">{subscription.status}</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginTop: '10px' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>
                <span>Products Allowed</span>
                <span>{subscription.usage?.productCount || 0} / {subscription.limits.maxProducts} items</span>
              </div>
              <div style={{ height: '6px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    background: 'var(--teal)',
                    width: `${Math.min(100, ((subscription.usage?.productCount || 0) / subscription.limits.maxProducts) * 100)}%`,
                  }}
                />
              </div>
            </div>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>
                <span>Staff Users</span>
                <span>{subscription.usage?.userCount || 1} / {subscription.limits.maxUsers} seats</span>
              </div>
              <div style={{ height: '6px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    background: 'var(--blue)',
                    width: `${Math.min(100, ((subscription.usage?.userCount || 1) / subscription.limits.maxUsers) * 100)}%`,
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Quick Action Shortcuts */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {canProcessSales && (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => onNavigate('sales-pos')}
            style={{ height: '48px', justifyContent: 'flex-start', padding: '0 16px' }}
          >
            <ShoppingCart size={18} color="var(--teal)" />
            <div style={{ textAlign: 'left', marginLeft: '6px' }}>
              <div style={{ fontSize: '13px', fontWeight: 700 }}>Open Counter POS</div>
              <div style={{ fontSize: '11px', color: 'var(--muted)' }}>Fast cashier sales cart</div>
            </div>
          </button>
        )}

        {canManageInventory && (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => onNavigate('receive-stock')}
            style={{ height: '48px', justifyContent: 'flex-start', padding: '0 16px' }}
          >
            <Truck size={18} color="var(--blue)" />
            <div style={{ textAlign: 'left', marginLeft: '6px' }}>
              <div style={{ fontSize: '13px', fontWeight: 700 }}>Receive Inbound Stock</div>
              <div style={{ fontSize: '11px', color: 'var(--muted)' }}>Supplier delivery notes</div>
            </div>
          </button>
        )}

        {canManageCatalog && (
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => onNavigate('products')}
            style={{ height: '48px', justifyContent: 'flex-start', padding: '0 16px' }}
          >
            <Plus size={18} color="var(--success)" />
            <div style={{ textAlign: 'left', marginLeft: '6px' }}>
              <div style={{ fontSize: '13px', fontWeight: 700 }}>Manage Catalog</div>
              <div style={{ fontSize: '11px', color: 'var(--muted)' }}>Add items & conversions</div>
            </div>
          </button>
        )}
      </div>

      {/* Two Column Grid: Recent Sales & Recent Movements */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '20px' }}>
        {/* Recent Sales */}
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Recent Sales</h2>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigate('sales-history')}
            >
              <span>View All</span>
              <ArrowRight size={14} />
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Invoice #</th>
                  <th>Date</th>
                  <th>Customer</th>
                  <th>Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {recentSales.length === 0 ? (
                  <tr>
                    <td colSpan={5} style={{ textAlign: 'center', padding: '24px', color: 'var(--muted)' }}>
                      No sales recorded yet. Use Counter POS to record the first sale.
                    </td>
                  </tr>
                ) : (
                  recentSales.map((sale) => (
                    <tr key={sale.id}>
                      <td style={{ fontWeight: 700 }}>{sale.invoiceNumber}</td>
                      <td>{new Date(sale.soldAt).toLocaleDateString()}</td>
                      <td>{sale.customerName || 'Walk-in Customer'}</td>
                      <td style={{ fontWeight: 700 }}>{formatCurrency(sale.totalAmount)}</td>
                      <td>
                        <StatusBadge status={sale.status} variant="sale" />
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent Movements */}
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Recent Stock Movements</h2>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => onNavigate('movements')}
            >
              <span>View Ledger</span>
              <ArrowRight size={14} />
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Change</th>
                  <th>Balance</th>
                </tr>
              </thead>
              <tbody>
                {recentMovements.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', padding: '24px', color: 'var(--muted)' }}>
                      No stock movements recorded yet.
                    </td>
                  </tr>
                ) : (
                  recentMovements.map((mov) => {
                    const isPositive = Number(mov.quantityChange) > 0;
                    return (
                      <tr key={mov.id}>
                        <td>
                          <div style={{ fontWeight: 700 }}>{mov.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {mov.productSku}</div>
                        </td>
                        <td>
                          <span className="badge badge-neutral">{mov.movementType}</span>
                        </td>
                        <td
                          style={{
                            fontWeight: 700,
                            color: isPositive ? 'var(--success)' : 'var(--danger)',
                          }}
                        >
                          {isPositive ? `+${mov.quantityChange}` : mov.quantityChange} {mov.baseUnitCode}
                        </td>
                        <td>
                          {mov.balanceAfter} {mov.baseUnitCode}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};
