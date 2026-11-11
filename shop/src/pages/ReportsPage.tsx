import React, { useState, useEffect } from 'react';
import {
  TrendingUp,
  DollarSign,
  Scale,
  Calendar,
  Layers,
  RefreshCw,
  ShieldAlert,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { shopApi } from '../api/client';
import {
  SalesSummaryReportDto,
  DailySalesSummaryDto,
  InventoryValuationReport,
} from '../api/types';
import { useAuth } from '../context/AuthContext';

export const ReportsPage: React.FC = () => {
  const { role } = useAuth();
  const isCashier = role === 'CASHIER';

  const [activeTab, setActiveTab] = useState<'sales' | 'inventory'>('sales');
  const [dateRange, setDateRange] = useState<'7' | '30' | '90'>('30');
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Report state
  const [salesSummary, setSalesSummary] = useState<SalesSummaryReportDto | null>(null);
  const [dailySales, setDailySales] = useState<DailySalesSummaryDto[]>([]);
  const [valuationReport, setValuationReport] = useState<InventoryValuationReport | null>(null);

  const fetchReports = async () => {
    setIsLoading(true);
    try {
      const now = new Date();
      const past = new Date();
      past.setDate(past.getDate() - parseInt(dateRange, 10));

      const startDate = past.toISOString();
      const endDate = now.toISOString();

      if (activeTab === 'sales') {
        const [sumRes, dailyRes] = await Promise.all([
          shopApi.getSalesSummary(startDate, endDate),
          shopApi.getDailySales(startDate, endDate),
        ]);
        setSalesSummary(sumRes.data);
        setDailySales(dailyRes.data || []);
      } else {
        const valRes = await shopApi.getInventoryValuationReport();
        setValuationReport(valRes.data);
      }
    } catch (err: any) {
      console.error('Failed to load reports:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, [activeTab, dateRange]);

  const formatCurrency = (val?: number) => {
    if (val == null) return '—';
    return Number(val).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <div>
      <PageHead
        eyebrow="Business Intelligence"
        title="Operational & Financial Reports"
        subtitle="Sales revenue analytics, daily intervals, moving-average cost margins, and inventory valuation"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchReports}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
          </div>
        }
      />

      {/* Role Redaction Notice for Cashiers */}
      {isCashier && (
        <div
          style={{
            background: '#fff9e6',
            border: '1px solid #ffe399',
            borderRadius: 'var(--radius-sm)',
            padding: '12px 16px',
            marginBottom: '20px',
            fontSize: '13px',
            color: '#875a00',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}
        >
          <ShieldAlert size={18} />
          <span>
            Cashier view active: Product cost of goods sold (COGS), gross margins, and inventory valuations are
            confidential and redacted.
          </span>
        </div>
      )}

      {/* Report Switcher Tabs */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            type="button"
            className={`btn btn-sm ${activeTab === 'sales' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('sales')}
          >
            <TrendingUp size={14} />
            <span>Sales & Revenue Summary</span>
          </button>
          <button
            type="button"
            className={`btn btn-sm ${activeTab === 'inventory' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('inventory')}
          >
            <Scale size={14} />
            <span>Inventory Valuation</span>
          </button>
        </div>

        {activeTab === 'sales' && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Calendar size={16} color="var(--muted)" />
            <select
              className="select-filter"
              style={{ height: '34px', fontSize: '12.5px' }}
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value as any)}
            >
              <option value="7">Last 7 Days</option>
              <option value="30">Last 30 Days</option>
              <option value="90">Last 90 Days</option>
            </select>
          </div>
        )}
      </div>

      {activeTab === 'sales' ? (
        <div>
          {/* Sales Summary KPIs */}
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-card-header">
                <span>Total Revenue</span>
                <div className="stat-card-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)' }}>
                  <DollarSign size={20} />
                </div>
              </div>
              <div className="stat-card-value">{formatCurrency(salesSummary?.totalRevenue || 0)}</div>
              <div className="stat-card-footer">
                <span>Total finalized customer sales</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card-header">
                <span>Sales Completed</span>
                <div className="stat-card-icon" style={{ background: '#eef6f9', color: 'var(--blue)' }}>
                  <TrendingUp size={20} />
                </div>
              </div>
              <div className="stat-card-value">{salesSummary?.totalSalesCount || 0}</div>
              <div className="stat-card-footer">
                <span>Avg Basket: {formatCurrency(salesSummary?.averageBasketValue || 0)}</span>
              </div>
            </div>

            {!isCashier && (
              <>
                <div className="stat-card">
                  <div className="stat-card-header">
                    <span>Gross Profit</span>
                    <div className="stat-card-icon" style={{ background: '#e8f5ee', color: 'var(--success)' }}>
                      <TrendingUp size={20} />
                    </div>
                  </div>
                  <div className="stat-card-value" style={{ color: 'var(--success)' }}>
                    {formatCurrency(salesSummary?.grossProfit || 0)}
                  </div>
                  <div className="stat-card-footer">
                    <span>Margin: {salesSummary?.grossMarginPercentage?.toFixed(1) || '0.0'}%</span>
                  </div>
                </div>

                <div className="stat-card">
                  <div className="stat-card-header">
                    <span>Cost of Goods Sold (COGS)</span>
                    <div className="stat-card-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
                      <Scale size={20} />
                    </div>
                  </div>
                  <div className="stat-card-value">{formatCurrency(salesSummary?.totalCost || 0)}</div>
                  <div className="stat-card-footer">
                    <span>Moving weighted-average cost</span>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Daily Breakdown Table */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Daily Interval Breakdown
            </h2>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th style={{ textAlign: 'center' }}>Transactions</th>
                    <th style={{ textAlign: 'right' }}>Revenue</th>
                    {!isCashier && (
                      <>
                        <th style={{ textAlign: 'right' }}>Cost</th>
                        <th style={{ textAlign: 'right' }}>Gross Profit</th>
                      </>
                    )}
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr>
                      <td colSpan={isCashier ? 3 : 5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        Calculating sales metrics...
                      </td>
                    </tr>
                  ) : dailySales.length === 0 ? (
                    <tr>
                      <td colSpan={isCashier ? 3 : 5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        No sales recorded during this date window.
                      </td>
                    </tr>
                  ) : (
                    dailySales.map((day, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 700 }}>{day.date}</td>
                        <td style={{ textAlign: 'center' }}>{day.salesCount}</td>
                        <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(day.revenue)}</td>
                        {!isCashier && (
                          <>
                            <td style={{ textAlign: 'right', color: 'var(--muted)' }}>{formatCurrency(day.cost)}</td>
                            <td style={{ textAlign: 'right', fontWeight: 700, color: 'var(--success)' }}>
                              {formatCurrency(day.grossProfit)}
                            </td>
                          </>
                        )}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      ) : (
        /* Inventory Valuation Report */
        <div>
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-card-header">
                <span>Total Catalog Products</span>
                <div className="stat-card-icon" style={{ background: '#eef6f9', color: 'var(--blue)' }}>
                  <Layers size={20} />
                </div>
              </div>
              <div className="stat-card-value">{valuationReport?.totalProducts || 0}</div>
              <div className="stat-card-footer">
                <span>SKUs tracked in inventory</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card-header">
                <span>Total Base Units on Hand</span>
                <div className="stat-card-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)' }}>
                  <Scale size={20} />
                </div>
              </div>
              <div className="stat-card-value">{valuationReport?.totalStockUnits || 0}</div>
              <div className="stat-card-footer">
                <span>Aggregated physical quantity</span>
              </div>
            </div>

            {!isCashier && (
              <div className="stat-card" style={{ gridColumn: 'span 2' }}>
                <div className="stat-card-header">
                  <span>Total Warehouse Valuation</span>
                  <div className="stat-card-icon" style={{ background: '#e8f5ee', color: 'var(--success)' }}>
                    <DollarSign size={20} />
                  </div>
                </div>
                <div className="stat-card-value" style={{ color: 'var(--teal)' }}>
                  {formatCurrency(valuationReport?.totalValuation || 0)}
                </div>
                <div className="stat-card-footer">
                  <span>Calculated via moving weighted-average costing</span>
                </div>
              </div>
            )}
          </div>

          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Valuation Breakdown by Category
            </h2>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th style={{ textAlign: 'center' }}>SKU Count</th>
                    <th style={{ textAlign: 'center' }}>Total Units</th>
                    {!isCashier && <th style={{ textAlign: 'right' }}>Total Valuation</th>}
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr>
                      <td colSpan={isCashier ? 3 : 4} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        Computing valuation breakdown...
                      </td>
                    </tr>
                  ) : !valuationReport?.categories || valuationReport.categories.length === 0 ? (
                    <tr>
                      <td colSpan={isCashier ? 3 : 4} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        No category valuation data available.
                      </td>
                    </tr>
                  ) : (
                    valuationReport.categories.map((cat, idx) => (
                      <tr key={idx}>
                        <td style={{ fontWeight: 700 }}>{cat.categoryName}</td>
                        <td style={{ textAlign: 'center' }}>{cat.productCount}</td>
                        <td style={{ textAlign: 'center' }}>{cat.totalQuantity}</td>
                        {!isCashier && (
                          <td style={{ textAlign: 'right', fontWeight: 700 }}>
                            {formatCurrency(cat.totalValuation)}
                          </td>
                        )}
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
