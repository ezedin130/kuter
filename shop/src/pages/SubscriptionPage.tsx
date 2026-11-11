import React, { useState, useEffect } from 'react';
import {
  CheckCircle,
  RefreshCw,
  Package,
  Users,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import { TenantSubscriptionInfo } from '../api/types';

export const SubscriptionPage: React.FC = () => {
  const [sub, setSub] = useState<TenantSubscriptionInfo | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSubscription = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await shopApi.getSubscription();
      setSub(res.data);
    } catch (err: any) {
      setError(err.message || 'Failed to load subscription details.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscription();
  }, []);

  return (
    <div>
      <PageHead
        eyebrow="License & Quotas"
        title="Shop Subscription & Quotas"
        subtitle="Self-inspection of your current subscription tier, capacity limits, and operational feature entitlements"
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchSubscription}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--muted)' }}>
          Loading subscription status and quotas...
        </div>
      ) : sub ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Main Tier Overview Card */}
          <div
            className="card"
            style={{
              background: 'linear-gradient(135deg, #102a3a 0%, #173f54 100%)',
              color: 'white',
              border: 'none',
              padding: '32px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 800,
                    textTransform: 'uppercase',
                    letterSpacing: '1px',
                    color: 'var(--teal-soft)',
                  }}
                >
                  Current Active Plan
                </span>
                <h2 style={{ fontSize: '32px', fontWeight: 800, color: 'white', marginTop: '4px' }}>
                  {sub.planName}
                </h2>
                <p style={{ color: '#b2c8d2', fontSize: '14px', marginTop: '4px' }}>
                  Plan Code: <code>{sub.planCode}</code>
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                <span
                  style={{
                    background: 'rgba(22, 117, 108, 0.3)',
                    color: '#7be3d8',
                    border: '1px solid rgba(123, 227, 216, 0.4)',
                    padding: '6px 14px',
                    borderRadius: '99px',
                    fontSize: '12px',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                  }}
                >
                  {sub.status}
                </span>
                <span style={{ fontSize: '13px', color: '#b2c8d2' }}>
                  {sub.daysRemaining != null ? `${sub.daysRemaining} days remaining in current billing cycle` : ''}
                </span>
              </div>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '20px',
                marginTop: '32px',
                paddingTop: '24px',
                borderTop: '1px solid rgba(255, 255, 255, 0.15)',
              }}
            >
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>Period Start</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {new Date(sub.currentPeriodStart).toLocaleDateString()}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>Renewal / End Date</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {new Date(sub.currentPeriodEnd).toLocaleDateString()}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>Cancellation Status</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {sub.cancelAtPeriodEnd ? 'Scheduled to cancel at period end' : 'Auto-renewing active contract'}
                </div>
              </div>
            </div>
          </div>

          {/* Quota Limits Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '20px' }}>
              Capacity Quotas & Resource Consumption
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
              {/* Products Quota */}
              <div
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius)',
                  padding: '20px',
                  background: '#fcfdfd',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                  <Package size={20} color="var(--teal)" />
                  <span style={{ fontWeight: 700, fontSize: '15px' }}>Catalog Products</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--muted)', marginBottom: '8px' }}>
                  <span>Used: <strong>{sub.usage?.productCount || 0}</strong> products</span>
                  <span>Limit: <strong>{sub.limits?.maxProducts}</strong> allowed</span>
                </div>
                <div style={{ height: '8px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      background: 'var(--teal)',
                      width: `${Math.min(100, ((sub.usage?.productCount || 0) / (sub.limits?.maxProducts || 1)) * 100)}%`,
                    }}
                  />
                </div>
                <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '8px' }}>
                  {Math.max(0, (sub.limits?.maxProducts || 0) - (sub.usage?.productCount || 0))} product slots remaining
                </div>
              </div>

              {/* Staff Users Quota */}
              <div
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius)',
                  padding: '20px',
                  background: '#fcfdfd',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                  <Users size={20} color="var(--blue)" />
                  <span style={{ fontWeight: 700, fontSize: '15px' }}>Staff Accounts</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--muted)', marginBottom: '8px' }}>
                  <span>Active: <strong>{sub.usage?.userCount || 1}</strong> users</span>
                  <span>Limit: <strong>{sub.limits?.maxUsers}</strong> seats</span>
                </div>
                <div style={{ height: '8px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      background: 'var(--blue)',
                      width: `${Math.min(100, ((sub.usage?.userCount || 1) / (sub.limits?.maxUsers || 1)) * 100)}%`,
                    }}
                  />
                </div>
                <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '8px' }}>
                  {Math.max(0, (sub.limits?.maxUsers || 0) - (sub.usage?.userCount || 1))} user seats remaining
                </div>
              </div>
            </div>
          </div>

          {/* Entitlements Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Plan Entitlements & Features
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>Dual-UOM Conversion Engine</div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    Multi-unit retail sales and packaging conversions (Pieces, Bundles, Tons).
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>Moving Weighted-Average Costing</div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    Continuous COGS tracking across deliveries and retail inventory balances.
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>Immutable Movement Ledger</div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    Tamper-proof audit logs for receipts, sales, voids, and shrinkage adjustments.
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>Role-Based Access Control</div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    Owner, Manager, Storekeeper, and Cashier segregation with financial redaction.
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
