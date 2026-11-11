import React from 'react';
import {
  Boxes,
  PackageOpen,
  ArrowDownLeft,
  ArrowUpRight,
  TrendingUp,
  AlertTriangle,
} from 'lucide-react';

export const ShopDashboardPage: React.FC = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      {/* Header Info */}
      <div className="card" style={{ background: 'linear-gradient(135deg, #102a3a 0%, #183e51 100%)', color: 'white' }}>
        <h2 style={{ fontSize: '20px', color: 'white', marginBottom: '8px' }}>
          Al-Madina Materials &bull; Yard Operations
        </h2>
        <p style={{ color: '#a0b6c2', fontSize: '13.5px', margin: 0, maxWidth: '750px', lineHeight: 1.6 }}>
          Shop Tenant Portal foundation. In upcoming phases, this workspace handles counter sales, stock receiving from suppliers, damage adjustments, and dual-unit conversions.
        </p>
      </div>

      {/* KPI Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        <div className="card">
          <div style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Active Products</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--ink)', marginTop: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>348 SKUs</span>
            <Boxes size={22} style={{ color: 'var(--teal)' }} />
          </div>
          <div style={{ fontSize: '12px', color: 'var(--success)', marginTop: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
            <TrendingUp size={14} />
            <span>12 added this week</span>
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Stock Reorder Alerts</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--warning)', marginTop: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>6 Items Low</span>
            <AlertTriangle size={22} style={{ color: 'var(--warning)' }} />
          </div>
          <div style={{ fontSize: '12px', color: 'var(--muted)', marginTop: '4px' }}>
            Rebar 10mm &bull; Cement White
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Recent Receipts</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--ink)', marginTop: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>3 Deliveries</span>
            <ArrowDownLeft size={22} style={{ color: 'var(--teal)' }} />
          </div>
          <div style={{ fontSize: '12px', color: 'var(--muted)', marginTop: '4px' }}>
            Dangote &bull; National Gypsum
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '12px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>Today's Sales Value</div>
          <div style={{ fontSize: '24px', fontWeight: 800, color: 'var(--ink)', marginTop: '6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <span>$4,890.00</span>
            <ArrowUpRight size={22} style={{ color: 'var(--success)' }} />
          </div>
          <div style={{ fontSize: '12px', color: 'var(--muted)', marginTop: '4px' }}>
            24 Counter transactions
          </div>
        </div>
      </div>

      {/* Building Materials Sample Inventory Table */}
      <div className="card">
        <h3 style={{ fontSize: '16px', color: 'var(--ink)', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <PackageOpen size={18} style={{ color: 'var(--teal)' }} />
          Sample Building Materials Catalog (Dual-Unit System)
        </h3>

        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '13.5px' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid var(--border)', color: 'var(--muted)', fontSize: '11.5px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                <th style={{ padding: '10px 12px' }}>SKU</th>
                <th style={{ padding: '10px 12px' }}>Material Name</th>
                <th style={{ padding: '10px 12px' }}>Category</th>
                <th style={{ padding: '10px 12px' }}>Primary Unit Stock</th>
                <th style={{ padding: '10px 12px' }}>Secondary Bulk Unit</th>
                <th style={{ padding: '10px 12px' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {[
                { sku: 'CEM-DNG-50KG', name: 'Dangote 42.5R Ordinary Portland Cement', cat: 'Cement & Aggregates', qty: '1,420 Bags', conv: '71.0 Tonnes (20 bags/t)', status: 'In Stock' },
                { sku: 'ST-REB-12MM', name: 'High-Tensile Steel Rebar 12mm x 12m', cat: 'Structural Steel', qty: '850 Pcs', conv: '70 Bundles (12 pcs/bdl)', status: 'In Stock' },
                { sku: 'PLY-MAR-18MM', name: 'Hardwood Marine Plywood 18mm 4x8ft', cat: 'Timber & Boards', qty: '42 Sheets', conv: 'Crater Pallet (50/plt)', status: 'Low Stock' },
                { sku: 'PNT-EXT-20L', name: 'Weather-Shield Acrylic Exterior White 20L', cat: 'Paints & Finishes', qty: '115 Drums', conv: '4 Pallets (24/plt)', status: 'In Stock' },
              ].map((item) => (
                <tr key={item.sku} style={{ borderBottom: '1px solid var(--border)' }}>
                  <td style={{ padding: '12px', fontFamily: 'monospace', color: 'var(--muted)', fontSize: '12px' }}>{item.sku}</td>
                  <td style={{ padding: '12px', fontWeight: 600, color: 'var(--ink)' }}>{item.name}</td>
                  <td style={{ padding: '12px', color: 'var(--muted)' }}>{item.cat}</td>
                  <td style={{ padding: '12px', fontWeight: 700 }}>{item.qty}</td>
                  <td style={{ padding: '12px', color: 'var(--teal)', fontSize: '12.5px' }}>{item.conv}</td>
                  <td style={{ padding: '12px' }}>
                    <span className={item.status === 'In Stock' ? 'badge badge-success' : 'badge badge-warning'}>
                      <span className="badge-dot" />
                      {item.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
