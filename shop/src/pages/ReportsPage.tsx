import React, { useState, useEffect, useMemo } from 'react';
import {
  TrendingUp,
  Scale,
  Calendar,
  Layers,
  RefreshCw,
  ShieldAlert,
  Download,
  Printer,
  Search,
  Coins,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { shopApi } from '../api/client';
import {
  SalesSummaryReportDto,
  DailySalesSummaryDto,
  InventoryValuationReport,
  StockBalanceResponse,
  CategoryResponse,
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
  const [balances, setBalances] = useState<StockBalanceResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);

  // Inventory Valuation Filters
  const [valSearch, setValSearch] = useState<string>('');
  const [valCategory, setValCategory] = useState<string>('');
  const [valStatus, setValStatus] = useState<'ALL' | 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'>('ALL');

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
        const [valRes, balRes, catRes] = await Promise.all([
          shopApi.getInventoryValuationReport(),
          shopApi.listStockBalances({ size: 500 }),
          shopApi.listCategories(),
        ]);
        setValuationReport(valRes.data);
        setBalances(balRes.data?.content || []);
        setCategories(catRes.data?.content || []);
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
    return `Br ${Number(val).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  // Filtered product balances for valuation table
  const filteredBalances = useMemo(() => {
    return balances.filter((b) => {
      if (valSearch) {
        const query = valSearch.toLowerCase();
        const matchesSku = (b.productSku || b.sku)?.toLowerCase().includes(query);
        const matchesName = b.productName?.toLowerCase().includes(query);
        if (!matchesSku && !matchesName) return false;
      }
      if (valCategory && b.categoryId !== valCategory) {
        return false;
      }
      if (valStatus === 'OUT_OF_STOCK' && Number(b.quantityOnHand || 0) > 0) {
        return false;
      }
      if (valStatus === 'LOW_STOCK' && !b.isLowStock) {
        return false;
      }
      if (valStatus === 'IN_STOCK' && (Number(b.quantityOnHand || 0) <= 0 || b.isLowStock)) {
        return false;
      }
      return true;
    });
  }, [balances, valSearch, valCategory, valStatus]);

  // Export filtered valuation table as CSV
  const handleExportCsv = () => {
    const headers = [
      'Product Name',
      'SKU',
      'Category',
      'Available Quantity',
      'Base Unit',
      'Average Purchase Cost (Br)',
      'Total Stock Value (Br)',
      'Stock Status',
    ];

    const rows = filteredBalances.map((b) => {
      const qty = Number(b.quantityOnHand || 0);
      const avgCost = Number(b.averageCost || 0);
      const totalVal = Number(b.totalValuation || qty * avgCost);
      const statusText = qty <= 0 ? 'Out of Stock' : b.isLowStock ? 'Low Stock' : 'In Stock';

      return [
        `"${b.productName.replace(/"/g, '""')}"`,
        `"${b.productSku || b.sku}"`,
        `"${b.categoryName || 'Uncategorized'}"`,
        qty,
        `"${b.baseUnitCode}"`,
        avgCost.toFixed(2),
        totalVal.toFixed(2),
        `"${statusText}"`,
      ].join(',');
    });

    const csvContent = 'data:text/csv;charset=utf-8,' + [headers.join(','), ...rows].join('\n');
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', `Inventory_Valuation_Report_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handlePrint = () => {
    window.print();
  };

  return (
    <div>
      <PageHead
        eyebrow="Business Intelligence"
        title="Operational & Financial Reports"
        subtitle="Sales revenue analytics, daily intervals, profit margins, and total inventory stock value"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            {activeTab === 'inventory' && (
              <>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleExportCsv}
                  title="Export valuation report as CSV"
                >
                  <Download size={14} />
                  <span>Export CSV</span>
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handlePrint}
                  title="Print valuation report"
                >
                  <Printer size={14} />
                  <span>Print Report</span>
                </button>
              </>
            )}
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
            Cashier view active: Product cost of items sold, profit margins, and inventory stock valuations are
            confidential and redacted.
          </span>
        </div>
      )}

      {/* Report Switcher Tabs */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px', flexWrap: 'wrap', gap: '12px' }}>
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
            <span>Total Stock Valuation</span>
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
                  <Coins size={20} />
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
                    <span>Cost of Goods Sold</span>
                    <div className="stat-card-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
                      <Scale size={20} />
                    </div>
                  </div>
                  <div className="stat-card-value">{formatCurrency(salesSummary?.totalCost || 0)}</div>
                  <div className="stat-card-footer">
                    <span>Average purchase cost of items sold</span>
                  </div>
                </div>
              </>
            )}
          </div>

          {/* Daily Breakdown Table */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Daily Sales Interval Breakdown
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
                        <th style={{ textAlign: 'right' }}>Cost of Goods Sold</th>
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
              <div className="stat-card-value">
                {valuationReport?.totalProducts || valuationReport?.totalSkus || balances.length}
              </div>
              <div className="stat-card-footer">
                <span>SKUs tracked in shop inventory</span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card-header">
                <span>Total Base Units on Hand</span>
                <div className="stat-card-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)' }}>
                  <Scale size={20} />
                </div>
              </div>
              <div className="stat-card-value">
                {valuationReport?.totalStockUnits ||
                  balances.reduce((acc, b) => acc + Number(b.quantityOnHand || 0), 0).toFixed(0)}
              </div>
              <div className="stat-card-footer">
                <span>Aggregated physical quantity</span>
              </div>
            </div>

            {!isCashier && (
              <div className="stat-card" style={{ gridColumn: 'span 2' }}>
                <div className="stat-card-header">
                  <span>Total Warehouse Stock Value</span>
                  <div className="stat-card-icon" style={{ background: '#e8f5ee', color: 'var(--success)' }}>
                    <Coins size={20} />
                  </div>
                </div>
                <div className="stat-card-value" style={{ color: 'var(--teal)' }}>
                  {formatCurrency(
                    valuationReport?.totalValuation ??
                      balances.reduce((acc, b) => acc + Number(b.totalValuation || 0), 0)
                  )}
                </div>
                <div className="stat-card-footer">
                  <span>Calculated via moving average purchase cost per product</span>
                </div>
              </div>
            )}
          </div>

          {/* Category Valuation Summary Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Valuation Breakdown by Category
            </h2>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Category</th>
                    <th style={{ textAlign: 'center' }}>Product Count</th>
                    <th style={{ textAlign: 'center' }}>Total Quantity</th>
                    {!isCashier && <th style={{ textAlign: 'right' }}>Total Stock Value (Br)</th>}
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
                        <td style={{ textAlign: 'center' }}>{Number(cat.totalQuantity).toFixed(2)}</td>
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

          {/* Product-Level Inventory Valuation Table */}
          <div className="card">
            <div className="card-header" style={{ flexWrap: 'wrap', gap: '12px' }}>
              <div>
                <h2 className="card-title">Product Inventory Valuation</h2>
                <div style={{ fontSize: '13px', color: 'var(--muted)' }}>
                  Product-level available quantities, average purchase costs, and valuation breakdown
                </div>
              </div>

              {/* Filters */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative' }}>
                  <Search size={14} style={{ position: 'absolute', left: '10px', top: '10px', color: 'var(--muted)' }} />
                  <input
                    type="text"
                    className="form-input"
                    style={{ height: '34px', paddingLeft: '30px', width: '180px', fontSize: '12.5px' }}
                    placeholder="Search product / SKU..."
                    value={valSearch}
                    onChange={(e) => setValSearch(e.target.value)}
                  />
                </div>

                <select
                  className="select-filter"
                  style={{ height: '34px', fontSize: '12.5px' }}
                  value={valCategory}
                  onChange={(e) => setValCategory(e.target.value)}
                >
                  <option value="">All Categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>

                <select
                  className="select-filter"
                  style={{ height: '34px', fontSize: '12.5px' }}
                  value={valStatus}
                  onChange={(e) => setValStatus(e.target.value as any)}
                >
                  <option value="ALL">All Stock Statuses</option>
                  <option value="IN_STOCK">In Stock Only</option>
                  <option value="LOW_STOCK">Low Stock Only</option>
                  <option value="OUT_OF_STOCK">Out of Stock Only</option>
                </select>
              </div>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product Name</th>
                    <th>Category</th>
                    <th style={{ textAlign: 'right' }}>Available Quantity</th>
                    <th>Base Unit</th>
                    {!isCashier && (
                      <>
                        <th style={{ textAlign: 'right' }}>Average Purchase Cost</th>
                        <th style={{ textAlign: 'right' }}>Total Stock Value</th>
                      </>
                    )}
                    <th style={{ textAlign: 'center' }}>Stock Status</th>
                  </tr>
                </thead>
                <tbody>
                  {isLoading ? (
                    <tr>
                      <td colSpan={isCashier ? 5 : 7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        Loading inventory balances...
                      </td>
                    </tr>
                  ) : filteredBalances.length === 0 ? (
                    <tr>
                      <td colSpan={isCashier ? 5 : 7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        No inventory records match the selected filters.
                      </td>
                    </tr>
                  ) : (
                    filteredBalances.map((item) => {
                      const qty = Number(item.quantityOnHand || 0);
                      const avgCost = Number(item.averageCost || 0);
                      const totalVal = Number(item.totalValuation ?? (qty * avgCost));
                      const isOutOfStock = qty <= 0;
                      const isLow = !isOutOfStock && item.isLowStock;

                      return (
                        <tr key={item.productId}>
                          <td>
                            <div style={{ fontWeight: 700 }}>{item.productName}</div>
                            <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku || item.sku}</div>
                          </td>
                          <td>{item.categoryName || 'General'}</td>
                          <td style={{ textAlign: 'right', fontWeight: 700 }}>
                            {qty.toFixed(2)}
                          </td>
                          <td>
                            <span className="uom-badge">{item.baseUnitCode}</span>
                          </td>
                          {!isCashier && (
                            <>
                              <td style={{ textAlign: 'right' }}>
                                {avgCost > 0 ? (
                                  formatCurrency(avgCost)
                                ) : (
                                  <span style={{ color: 'var(--muted)', fontSize: '11.5px' }}>
                                    Br 0.00 (Cost not set)
                                  </span>
                                )}
                              </td>
                              <td style={{ textAlign: 'right', fontWeight: 700 }}>
                                {formatCurrency(totalVal)}
                              </td>
                            </>
                          )}
                          <td style={{ textAlign: 'center' }}>
                            {isOutOfStock ? (
                              <span className="badge badge-danger">Out of Stock</span>
                            ) : isLow ? (
                              <span className="badge badge-danger">Low Stock</span>
                            ) : (
                              <span className="badge badge-success">In Stock</span>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {filteredBalances.length > 0 && !isCashier && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: '16px',
                  paddingTop: '12px',
                  borderTop: '1px solid var(--border)',
                  fontSize: '13px',
                }}
              >
                <div style={{ color: 'var(--muted)' }}>
                  Showing <strong>{filteredBalances.length}</strong> of <strong>{balances.length}</strong> products
                </div>
                <div style={{ fontWeight: 800, fontSize: '15px', color: 'var(--teal-dark)' }}>
                  Filtered Total Stock Value:{' '}
                  {formatCurrency(
                    filteredBalances.reduce(
                      (acc, b) => acc + Number(b.totalValuation ?? (Number(b.quantityOnHand || 0) * Number(b.averageCost || 0))),
                      0
                    )
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
