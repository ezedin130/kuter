import { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  Clock,
  History,
  AlertTriangle,
  PauseCircle,
  PlayCircle,
  XCircle,
  CheckCircle2,
} from 'lucide-react';
import { apiClient } from '../api/client';
import {
  AdminSubscriptionDetailDto,
  SubscriptionStatus,
  SubscriptionSummaryDto,
  SubscriptionHistoryDto,
  SubscriptionPlanDto,
  TenantSummaryResponse,
} from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { DataTable, Column } from '../components/DataTable';

export const SubscriptionsPage: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<AdminSubscriptionDetailDto[]>([]);
  const [summary, setSummary] = useState<SubscriptionSummaryDto | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Available options
  const [availablePlans, setAvailablePlans] = useState<SubscriptionPlanDto[]>([]);
  const [availableTenants, setAvailableTenants] = useState<TenantSummaryResponse[]>([]);

  // Action modals
  const [assignModal, setAssignModal] = useState<boolean>(false);
  const [assignTenantId, setAssignTenantId] = useState<string>('');
  const [assignPlanId, setAssignPlanId] = useState<string>('');
  const [assignTrial, setAssignTrial] = useState<boolean>(true);
  const [assignTrialDays, setAssignTrialDays] = useState<number>(14);

  const [extendTarget, setExtendTarget] = useState<AdminSubscriptionDetailDto | null>(null);
  const [extendDays, setExtendDays] = useState<number>(30);
  const [extendReason, setExtendReason] = useState<string>('');

  const [changePlanTarget, setChangePlanTarget] = useState<AdminSubscriptionDetailDto | null>(null);
  const [newPlanId, setNewPlanId] = useState<string>('');
  const [changePlanReason, setChangePlanReason] = useState<string>('');

  const [statusTarget, setStatusTarget] = useState<{
    sub: AdminSubscriptionDetailDto;
    action: 'suspend' | 'reactivate' | 'cancel';
  } | null>(null);
  const [actionReason, setActionReason] = useState<string>('');
  const [cancelAtPeriodEnd, setCancelAtPeriodEnd] = useState<boolean>(false);

  const [historyTarget, setHistoryTarget] = useState<AdminSubscriptionDetailDto | null>(null);
  const [historyList, setHistoryList] = useState<SubscriptionHistoryDto[]>([]);
  const [loadingHistory, setLoadingHistory] = useState<boolean>(false);

  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [banner, setBanner] = useState<{ type: 'success' | 'danger' | 'info'; message: string } | null>(null);

  const fetchSubscriptions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [subPage, sumData] = await Promise.all([
        apiClient.listSubscriptions({
          status: statusFilter === 'ALL' ? undefined : statusFilter,
          search: search.trim() || undefined,
          page,
          size: 10,
        }),
        apiClient.getSubscriptionSummary(),
      ]);
      setSubscriptions(subPage.content || []);
      setTotalPages(subPage.totalPages || 1);
      setTotalElements(subPage.totalElements || 0);
      setSummary(sumData);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to retrieve subscriptions.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, page]);

  useEffect(() => {
    fetchSubscriptions();
  }, [fetchSubscriptions]);

  // Load auxiliary lists for modals
  useEffect(() => {
    apiClient.listPlans({ isActive: true }).then((res) => {
      setAvailablePlans(res.content || []);
      if (res.content?.[0]) {
        setAssignPlanId(res.content[0].id);
        setNewPlanId(res.content[0].id);
      }
    }).catch(() => {});

    apiClient.listTenants({ size: 100 }).then((res) => {
      setAvailableTenants(res.content || []);
      if (res.content?.[0]) setAssignTenantId(res.content[0].id);
    }).catch(() => {});
  }, []);

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!assignTenantId || !assignPlanId) return;

    setActionLoading(true);
    try {
      await apiClient.assignSubscription({
        tenantId: assignTenantId,
        planId: assignPlanId,
        startTrial: assignTrial,
        trialDays: assignTrial ? assignTrialDays : undefined,
      });
      setBanner({ type: 'success', message: 'Subscription successfully assigned to shop tenant.' });
      setAssignModal(false);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({ type: 'danger', message: err instanceof Error ? err.message : 'Failed to assign subscription.' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleExtend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!extendTarget || !extendReason.trim()) return;

    setActionLoading(true);
    try {
      await apiClient.extendSubscription(extendTarget.id, {
        days: Number(extendDays),
        reason: extendReason.trim(),
      });
      setBanner({
        type: 'success',
        message: `Subscription for "${extendTarget.tenantName}" extended by ${extendDays} days.`,
      });
      setExtendTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({ type: 'danger', message: err instanceof Error ? err.message : 'Failed to extend subscription.' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleChangePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!changePlanTarget || !newPlanId) return;

    setActionLoading(true);
    try {
      await apiClient.changeSubscriptionPlan(changePlanTarget.id, {
        newPlanId,
        reason: changePlanReason.trim() || undefined,
      });
      setBanner({
        type: 'success',
        message: `Subscription plan updated for "${changePlanTarget.tenantName}".`,
      });
      setChangePlanTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({ type: 'danger', message: err instanceof Error ? err.message : 'Failed to change subscription plan.' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmLifecycleAction = async () => {
    if (!statusTarget) return;
    const { sub, action } = statusTarget;

    setActionLoading(true);
    try {
      if (action === 'suspend') {
        await apiClient.suspendSubscription(sub.id, { reason: actionReason.trim() || 'Administrative suspension' });
        setBanner({ type: 'success', message: `Subscription for "${sub.tenantName}" has been suspended.` });
      } else if (action === 'reactivate') {
        await apiClient.reactivateSubscription(sub.id, actionReason.trim() || undefined);
        setBanner({ type: 'success', message: `Subscription for "${sub.tenantName}" has been reactivated.` });
      } else if (action === 'cancel') {
        await apiClient.cancelSubscription(sub.id, {
          cancelAtPeriodEnd,
          reason: actionReason.trim() || 'Administrative cancellation',
        });
        setBanner({ type: 'success', message: `Subscription for "${sub.tenantName}" has been cancelled.` });
      }
      setStatusTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({ type: 'danger', message: err instanceof Error ? err.message : 'Failed to update subscription status.' });
    } finally {
      setActionLoading(false);
    }
  };

  const handleViewHistory = async (sub: AdminSubscriptionDetailDto) => {
    setHistoryTarget(sub);
    setLoadingHistory(true);
    try {
      const logs = await apiClient.getSubscriptionHistory(sub.id);
      setHistoryList(logs);
    } catch {
      setHistoryList([]);
    } finally {
      setLoadingHistory(false);
    }
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    } catch {
      return isoStr;
    }
  };

  const formatPrice = (price: number, _currency: string, interval: string) => {
    return `Br ${Number(price).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 })} / ${interval.toLowerCase()}`;
  };

  const columns: Column<AdminSubscriptionDetailDto>[] = [
    {
      key: 'tenant',
      header: 'Tenant Shop',
      render: (s) => (
        <div>
          <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{s.tenantName}</div>
          <div className="mono-text" style={{ fontSize: '11.5px', color: 'var(--muted)' }}>
            {s.tenantCode}
          </div>
        </div>
      ),
    },
    {
      key: 'plan',
      header: 'Assigned Tier',
      render: (s) => (
        <div>
          <span style={{ fontWeight: 700, color: 'var(--teal)' }}>{s.planName}</span>
          <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>
            {formatPrice(s.price, s.currency, s.billingInterval)}
          </div>
        </div>
      ),
    },
    {
      key: 'quotas',
      header: 'Capacity Quotas & Resource Consumption',
      render: (s) => {
        const prodCount = s.activeProductsCount ?? s.usage?.productCount ?? 0;
        const prodLimit = s.productLimit ?? s.limits?.maxProducts;
        const userCount = s.activeUsersCount ?? s.usage?.userCount ?? 1;
        const userLimit = s.userLimit ?? s.limits?.maxUsers;

        const prodPercent = prodLimit ? Math.min(100, (prodCount / prodLimit) * 100) : 0;
        const userPercent = userLimit ? Math.min(100, (userCount / userLimit) * 100) : 0;

        const prodReached = prodLimit ? prodCount >= prodLimit : false;
        const userReached = userLimit ? userCount >= userLimit : false;
        const prodWarning = prodLimit ? prodPercent >= 80 && !prodReached : false;
        const userWarning = userLimit ? userPercent >= 80 && !userReached : false;

        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', minWidth: '190px' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', marginBottom: '2px' }}>
                <span style={{ fontWeight: 600 }}>Products:</span>
                <span>
                  <strong>{prodCount}</strong> / {prodLimit ? `${prodLimit} used` : 'Unlimited'}
                </span>
              </div>
              {prodLimit && (
                <div style={{ height: '4px', background: '#e2e8f0', borderRadius: '2px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      background: prodReached ? 'var(--danger)' : prodWarning ? 'var(--warning)' : 'var(--teal)',
                      width: `${prodPercent}%`,
                    }}
                  />
                </div>
              )}
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '11px', marginBottom: '2px' }}>
                <span style={{ fontWeight: 600 }}>Staff Accounts:</span>
                <span>
                  <strong>{userCount}</strong> / {userLimit ? `${userLimit} used` : 'Unlimited'}
                </span>
              </div>
              {userLimit && (
                <div style={{ height: '4px', background: '#e2e8f0', borderRadius: '2px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      background: userReached ? 'var(--danger)' : userWarning ? 'var(--warning)' : 'var(--blue)',
                      width: `${userPercent}%`,
                    }}
                  />
                </div>
              )}
            </div>
          </div>
        );
      },
    },
    {
      key: 'status',
      header: 'Effective Status',
      render: (s) => (
        <div>
          <StatusBadge status={s.effectiveStatus || s.status} />
          {s.cancelAtPeriodEnd && (
            <div style={{ fontSize: '10.5px', color: 'var(--warning)', fontWeight: 600, marginTop: '2px' }}>
              Cancelling at period end
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'daysRemaining',
      header: 'Days Left',
      render: (s) => (
        <span
          style={{
            fontWeight: 700,
            fontSize: '13px',
            color: s.daysRemaining <= 7 ? 'var(--warning)' : 'var(--ink)',
          }}
        >
          {s.daysRemaining} days
        </span>
      ),
    },
    {
      key: 'period',
      header: 'Current Period',
      render: (s) => (
        <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
          <div>Start: {formatDate(s.currentPeriodStart)}</div>
          <div>End: {formatDate(s.currentPeriodEnd)}</div>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      width: '260px',
      render: (s) => (
        <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setExtendTarget(s);
              setExtendDays(30);
              setExtendReason('');
            }}
            title="Extend subscription validity"
          >
            Extend
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setChangePlanTarget(s);
              setChangePlanReason('');
            }}
            title="Change plan tier"
          >
            Switch Plan
          </button>

          {s.status === 'ACTIVE' || s.status === 'TRIALING' ? (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--danger)' }}
              onClick={() => {
                setStatusTarget({ sub: s, action: 'suspend' });
                setActionReason('');
              }}
              title="Suspend subscription"
            >
              <PauseCircle size={13} />
            </button>
          ) : s.status === 'SUSPENDED' ? (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--success)' }}
              onClick={() => {
                setStatusTarget({ sub: s, action: 'reactivate' });
                setActionReason('');
              }}
              title="Reactivate subscription"
            >
              <PlayCircle size={13} />
            </button>
          ) : null}

          {s.status !== 'CANCELLED' && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--danger)' }}
              onClick={() => {
                setStatusTarget({ sub: s, action: 'cancel' });
                setActionReason('');
                setCancelAtPeriodEnd(true);
              }}
              title="Cancel subscription"
            >
              <XCircle size={13} />
            </button>
          )}

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleViewHistory(s)}
            title="View lifecycle history log"
          >
            <History size={13} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHead
        eyebrow="Commercial Oversight"
        title="Tenant Subscriptions"
        description="Monitor tenant plan assignments, lifecycle transitions, extensions, and quota entitlements in real time."
        actions={
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setAssignModal(true)}
          >
            <Plus size={15} /> Assign Plan to Shop
          </button>
        }
      />

      {banner && (
        <Alert
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      {/* Subscription Summary KPI Cards */}
      {summary && (
        <div className="metric-grid" style={{ marginBottom: '20px' }}>
          <div className="metric-card" onClick={() => setStatusFilter('ACTIVE')}>
            <span className="metric-label">Active Subscriptions</span>
            <div className="metric-value">{summary.activeSubscriptions}</div>
            <div className="metric-meta positive">
              <CheckCircle2 size={13} />
              <span>{summary.totalSubscriptions} total assigned</span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('TRIALING')}>
            <span className="metric-label">Free Trials</span>
            <div className="metric-value" style={{ color: '#1e557b' }}>
              {summary.trialingSubscriptions}
            </div>
            <div className="metric-meta">
              <span>Trialing accounts</span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('EXPIRED')}>
            <span className="metric-label">Expired</span>
            <div className="metric-value" style={{ color: summary.expiredSubscriptions > 0 ? 'var(--warning)' : 'var(--ink)' }}>
              {summary.expiredSubscriptions}
            </div>
            <div className="metric-meta warning">
              <span>Requires extension or renewal</span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('SUSPENDED')}>
            <span className="metric-label">Suspended</span>
            <div className="metric-value" style={{ color: summary.suspendedSubscriptions > 0 ? 'var(--danger)' : 'var(--ink)' }}>
              {summary.suspendedSubscriptions}
            </div>
            <div className="metric-meta">
              <span>Admin frozen</span>
            </div>
          </div>
        </div>
      )}

      {/* Main Table */}
      <DataTable
        columns={columns}
        data={subscriptions}
        loading={loading}
        error={error}
        onRetry={fetchSubscriptions}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        searchPlaceholder="Search by tenant name, code, or plan..."
        searchValue={search}
        onSearchChange={(val) => {
          setSearch(val);
          setPage(0);
        }}
        filterComponent={
          <div className="filter-pill-tabs">
            {(['ALL', 'ACTIVE', 'TRIALING', 'EXPIRED', 'SUSPENDED', 'CANCELLED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                className={`filter-pill-btn ${statusFilter === st ? 'active' : ''}`}
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
              >
                {st === 'ALL' ? 'All Subscriptions' : st.charAt(0) + st.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        }
        emptyTitle="No tenant subscriptions found"
        emptyMessage="There are currently no active or historical subscriptions matching your filter."
        rowKey={(s) => s.id}
      />

      {/* Modal: Assign Plan */}
      {assignModal && (
        <Modal
          title="Assign Subscription Plan"
          subtitle="Assign or replace commercial plan for a tenant shop"
          onClose={() => setAssignModal(false)}
        >
          <form onSubmit={handleAssign}>
            <div className="form-group">
              <label className="form-label" htmlFor="assign-tenant">
                Select Tenant Shop <span className="required">*</span>
              </label>
              <select
                id="assign-tenant"
                className="form-select"
                value={assignTenantId}
                onChange={(e) => setAssignTenantId(e.target.value)}
                required
              >
                {availableTenants.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name} ({t.code})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="assign-plan">
                Subscription Tier <span className="required">*</span>
              </label>
              <select
                id="assign-plan"
                className="form-select"
                value={assignPlanId}
                onChange={(e) => setAssignPlanId(e.target.value)}
                required
              >
                {availablePlans.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} (Br {p.price}/{p.billingInterval.toLowerCase()})
                  </option>
                ))}
              </select>
            </div>

            <div style={{ margin: '14px 0' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={assignTrial}
                  onChange={(e) => setAssignTrial(e.target.checked)}
                />
                <span>Start as Commercial Free Trial</span>
              </label>
            </div>

            {assignTrial && (
              <div className="form-group">
                <label className="form-label" htmlFor="assign-days">
                  Trial Duration (Days)
                </label>
                <input
                  id="assign-days"
                  type="number"
                  min="1"
                  max="90"
                  className="form-input"
                  value={assignTrialDays}
                  onChange={(e) => setAssignTrialDays(parseInt(e.target.value) || 14)}
                />
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setAssignModal(false)}
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? 'Assigning...' : 'Confirm Plan Assignment'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Extend Subscription */}
      {extendTarget && (
        <Modal
          title={`Extend Subscription: ${extendTarget.tenantName}`}
          subtitle={`Plan: ${extendTarget.planName} · Current Expiry: ${formatDate(extendTarget.currentPeriodEnd)}`}
          onClose={() => setExtendTarget(null)}
        >
          <form onSubmit={handleExtend}>
            <div className="form-group">
              <label className="form-label" htmlFor="extend-days">
                Additional Days <span className="required">*</span>
              </label>
              <input
                id="extend-days"
                type="number"
                min="1"
                max="365"
                className="form-input"
                value={extendDays}
                onChange={(e) => setExtendDays(parseInt(e.target.value) || 30)}
                required
              />
              <p className="form-help">Advances the current period end date and restores expired subscriptions to ACTIVE.</p>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="extend-reason">
                Reason for Extension (Mandatory Record) <span className="required">*</span>
              </label>
              <textarea
                id="extend-reason"
                className="form-textarea"
                placeholder="e.g. Promotional extension, offline payment receipt confirmed..."
                value={extendReason}
                onChange={(e) => setExtendReason(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setExtendTarget(null)}
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading || !extendReason.trim()}
              >
                {actionLoading ? 'Extending...' : 'Confirm Extension'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Switch Plan */}
      {changePlanTarget && (
        <Modal
          title={`Switch Subscription Plan: ${changePlanTarget.tenantName}`}
          subtitle={`Current Plan: ${changePlanTarget.planName}`}
          onClose={() => setChangePlanTarget(null)}
        >
          <form onSubmit={handleChangePlan}>
            <div className="form-group">
              <label className="form-label" htmlFor="switch-plan">
                New Plan Tier <span className="required">*</span>
              </label>
              <select
                id="switch-plan"
                className="form-select"
                value={newPlanId}
                onChange={(e) => setNewPlanId(e.target.value)}
                required
              >
                {availablePlans.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} (Br {p.price}/{p.billingInterval.toLowerCase()})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="switch-reason">
                Reason for Plan Change
              </label>
              <textarea
                id="switch-reason"
                className="form-textarea"
                placeholder="e.g. Upgraded to Enterprise tier per shop owner request..."
                value={changePlanReason}
                onChange={(e) => setChangePlanReason(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setChangePlanTarget(null)}
                disabled={actionLoading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? 'Updating Plan...' : 'Confirm Plan Switch'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Lifecycle Actions (Suspend, Reactivate, Cancel) */}
      {statusTarget && (
        <Modal
          title={`${statusTarget.action.charAt(0).toUpperCase() + statusTarget.action.slice(1)} Subscription`}
          subtitle={`Tenant Shop: ${statusTarget.sub.tenantName}`}
          onClose={() => setStatusTarget(null)}
        >
          <div
            style={{
              padding: '12px 14px',
              backgroundColor: statusTarget.action === 'reactivate' ? 'var(--success-bg)' : 'var(--danger-bg)',
              color: statusTarget.action === 'reactivate' ? 'var(--success)' : 'var(--danger)',
              borderRadius: 'var(--radius-sm)',
              fontSize: '13px',
              lineHeight: 1.5,
              marginBottom: '16px',
              display: 'flex',
              gap: '10px',
            }}
          >
            <AlertTriangle size={18} style={{ flexShrink: 0, marginTop: '2px' }} />
            <div>
              {statusTarget.action === 'suspend' && (
                <>Suspending access disables product mutations and sales processing for this tenant until reactivated.</>
              )}
              {statusTarget.action === 'reactivate' && (
                <>Reactivating restores the subscription to active status and enables tenant workspace operations.</>
              )}
              {statusTarget.action === 'cancel' && (
                <>Cancelling will transition the subscription to CANCELLED. Data is preserved but further operations will be gated.</>
              )}
            </div>
          </div>

          {statusTarget.action === 'cancel' && (
            <div style={{ margin: '14px 0' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={cancelAtPeriodEnd}
                  onChange={(e) => setCancelAtPeriodEnd(e.target.checked)}
                />
                <span>Cancel at Current Period End (Preserve access until period expiry)</span>
              </label>
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="lifecycle-reason">
              Reason / Reviewer Notes <span className="required">*</span>
            </label>
            <textarea
              id="lifecycle-reason"
              className="form-textarea"
              placeholder="State the justification for this subscription status change..."
              value={actionReason}
              onChange={(e) => setActionReason(e.target.value)}
              required
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setStatusTarget(null)}
              disabled={actionLoading}
            >
              Cancel
            </button>
            <button
              type="button"
              className={statusTarget.action === 'reactivate' ? 'btn btn-primary' : 'btn btn-danger'}
              onClick={handleConfirmLifecycleAction}
              disabled={actionLoading || !actionReason.trim()}
            >
              {actionLoading ? 'Processing...' : `Confirm ${statusTarget.action}`}
            </button>
          </div>
        </Modal>
      )}

      {/* Modal: View Subscription History Log */}
      {historyTarget && (
        <Modal
          title={`Subscription History: ${historyTarget.tenantName}`}
          subtitle={`Plan: ${historyTarget.planName} · ID: ${historyTarget.id}`}
          onClose={() => setHistoryTarget(null)}
          maxWidth="640px"
        >
          {loadingHistory ? (
            <div style={{ padding: '32px', textAlign: 'center', color: 'var(--muted)' }}>
              Loading lifecycle history records...
            </div>
          ) : historyList.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: 'var(--muted)' }}>
              No previous history transitions recorded.
            </div>
          ) : (
            <div className="action-feed-list">
              {historyList.map((h) => (
                <div key={h.id} className="action-feed-item">
                  <div className="feed-icon-wrap">
                    <Clock size={14} />
                  </div>
                  <div className="feed-content">
                    <div className="feed-title">{h.action}</div>
                    <div className="feed-meta">
                      Transition: {h.fromStatus ? `${h.fromStatus} → ` : ''}
                      <strong>{h.toStatus}</strong>
                      {h.reason && ` · Reason: "${h.reason}"`}
                    </div>
                    {h.actorEmail && (
                      <div style={{ fontSize: '11px', color: 'var(--faint)', marginTop: '2px' }}>
                        By: {h.actorEmail}
                      </div>
                    )}
                  </div>
                  <div className="feed-time">{formatDate(h.createdAt)}</div>
                </div>
              ))}
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setHistoryTarget(null)}
            >
              Close
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};
