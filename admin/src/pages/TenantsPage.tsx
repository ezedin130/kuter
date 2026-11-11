import { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  AlertTriangle,
  Layers,
  PauseCircle,
  PlayCircle,
} from 'lucide-react';
import { apiClient } from '../api/client';
import {
  TenantSummaryResponse,
  TenantStatus,
  TenantStatusDetailDto,
  CreateTenantDto,
  SubscriptionPlanDto,
} from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { DataTable, Column } from '../components/DataTable';

export const TenantsPage: React.FC = () => {
  const [tenants, setTenants] = useState<TenantSummaryResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<TenantStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Available plans for creation dropdown
  const [availablePlans, setAvailablePlans] = useState<SubscriptionPlanDto[]>([]);

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [creating, setCreating] = useState<boolean>(false);
  const [createForm, setCreateForm] = useState<CreateTenantDto>({
    name: '',
    code: '',
    currency: 'USD',
    timezone: 'UTC',
    initialOwnerEmail: '',
    initialOwnerFullName: '',
    initialOwnerPhone: '',
    initialOwnerPassword: '',
    planCode: '',
  });

  // Inspection modal
  const [inspectStatus, setInspectStatus] = useState<TenantStatusDetailDto | null>(null);
  const [inspecting, setInspecting] = useState<boolean>(false);

  // Status transition modal
  const [statusTarget, setStatusTarget] = useState<TenantSummaryResponse | null>(null);
  const [newStatus, setNewStatus] = useState<TenantStatus>('ACTIVE');
  const [statusReason, setStatusReason] = useState<string>('');
  const [updatingStatus, setUpdatingStatus] = useState<boolean>(false);

  // Notifications
  const [banner, setBanner] = useState<{ type: 'success' | 'danger' | 'info'; message: string } | null>(null);

  const fetchTenants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.listTenants({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: search.trim() || undefined,
        page,
        size: 10,
      });
      setTenants(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to retrieve tenants.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, page]);

  useEffect(() => {
    fetchTenants();
  }, [fetchTenants]);

  // Load plans for creation modal
  useEffect(() => {
    apiClient.listPlans({ isActive: true }).then((res) => {
      setAvailablePlans(res.content || []);
      if (res.content && res.content.length > 0) {
        setCreateForm((prev) => ({ ...prev, planCode: res.content[0].code }));
      }
    }).catch(() => {
      // Fallback if plans fetch fails
    });
  }, []);

  const handleCreateTenant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.name.trim() || !createForm.code.trim() || !createForm.initialOwnerEmail.trim()) {
      setBanner({ type: 'danger', message: 'Shop name, code slug, and owner email are required.' });
      return;
    }

    setCreating(true);
    try {
      await apiClient.createTenant({
        ...createForm,
        code: createForm.code.trim().toLowerCase(),
        initialOwnerEmail: createForm.initialOwnerEmail.trim(),
        initialOwnerFullName: createForm.initialOwnerFullName.trim() || 'Shop Owner',
      });
      setBanner({
        type: 'success',
        message: `Tenant "${createForm.name}" created successfully.`,
      });
      setShowCreateModal(false);
      setCreateForm({
        name: '',
        code: '',
        currency: 'USD',
        timezone: 'UTC',
        initialOwnerEmail: '',
        initialOwnerFullName: '',
        initialOwnerPhone: '',
        initialOwnerPassword: '',
        planCode: availablePlans[0]?.code || '',
      });
      await fetchTenants();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : 'Failed to create tenant shop.',
      });
    } finally {
      setCreating(false);
    }
  };

  const handleInspect = async (tenantId: string) => {
    setInspecting(true);
    try {
      const data = await apiClient.getTenantStatus(tenantId);
      setInspectStatus(data);
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : 'Failed to inspect tenant status.',
      });
    } finally {
      setInspecting(false);
    }
  };

  const handleOpenStatusModal = (tenant: TenantSummaryResponse, targetStatus: TenantStatus) => {
    setStatusTarget(tenant);
    setNewStatus(targetStatus);
    setStatusReason('');
  };

  const handleConfirmStatusChange = async () => {
    if (!statusTarget) return;
    setUpdatingStatus(true);
    try {
      await apiClient.updateTenantStatus(statusTarget.id, {
        status: newStatus,
        reason: statusReason.trim() || undefined,
      });
      setBanner({
        type: 'success',
        message: `Tenant "${statusTarget.name}" operational status updated to ${newStatus}.`,
      });
      setStatusTarget(null);
      await fetchTenants();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : 'Failed to update tenant status.',
      });
    } finally {
      setUpdatingStatus(false);
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

  const columns: Column<TenantSummaryResponse>[] = [
    {
      key: 'code',
      header: 'Tenant Slug',
      width: '130px',
      render: (t) => <span className="mono-text">{t.code}</span>,
    },
    {
      key: 'name',
      header: 'Shop Name',
      render: (t) => (
        <div>
          <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{t.name}</div>
          <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
            Currency: {t.currency} · Zone: {t.timezone}
          </div>
        </div>
      ),
    },
    {
      key: 'owner',
      header: 'Primary Owner',
      render: (t) => (
        <div style={{ fontSize: '12.5px' }}>
          <div style={{ fontWeight: 600 }}>{t.ownerFullName || 'Owner'}</div>
          <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>{t.ownerEmail || '—'}</div>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Operational Status',
      render: (t) => <StatusBadge status={t.status} />,
    },
    {
      key: 'subscription',
      header: 'Subscription',
      render: (t) => (
        <div style={{ fontSize: '12px' }}>
          <span style={{ fontWeight: 700, color: 'var(--teal)' }}>
            {t.activePlanCode || 'STARTER'}
          </span>
          {t.subscriptionStatus && (
            <div style={{ marginTop: '2px' }}>
              <StatusBadge status={t.subscriptionStatus} size="sm" />
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'createdAt',
      header: 'Created',
      render: (t) => <span style={{ fontSize: '12.5px' }}>{formatDate(t.createdAt)}</span>,
    },
    {
      key: 'actions',
      header: 'Actions',
      width: '180px',
      render: (t) => (
        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleInspect(t.id)}
            disabled={inspecting}
            title="Inspect operational status and subscription breakdown"
          >
            Inspect
          </button>

          {t.status === 'ACTIVE' ? (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--danger)' }}
              onClick={() => handleOpenStatusModal(t, 'SUSPENDED')}
              title="Suspend shop tenant"
            >
              <PauseCircle size={14} /> Suspend
            </button>
          ) : (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--success)' }}
              onClick={() => handleOpenStatusModal(t, 'ACTIVE')}
              title="Reactivate shop tenant"
            >
              <PlayCircle size={14} /> Activate
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHead
        eyebrow="Merchant Directory"
        title="Shops &amp; Tenants Directory"
        description="Direct provisioning, operational status administration, and decoupled subscription lifecycle monitoring."
        actions={
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setShowCreateModal(true)}
          >
            <Plus size={15} /> Provision New Shop
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

      {/* Main Data Table */}
      <DataTable
        columns={columns}
        data={tenants}
        loading={loading}
        error={error}
        onRetry={fetchTenants}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        searchPlaceholder="Search shop name, code, or owner..."
        searchValue={search}
        onSearchChange={(val) => {
          setSearch(val);
          setPage(0);
        }}
        filterComponent={
          <div className="filter-pill-tabs">
            {(['ALL', 'ACTIVE', 'SUSPENDED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                className={`filter-pill-btn ${statusFilter === st ? 'active' : ''}`}
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
              >
                {st === 'ALL' ? 'All Shops' : st.charAt(0) + st.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        }
        emptyTitle="No tenant shops found"
        emptyMessage="There are no shops matching your search or operational filter."
        rowKey={(t) => t.id}
      />

      {/* Modal: Create Tenant Directly */}
      {showCreateModal && (
        <Modal
          title="Direct Provision Tenant Shop"
          subtitle="Provision a merchant yard and primary owner without public onboarding review"
          onClose={() => setShowCreateModal(false)}
          maxWidth="620px"
        >
          <form onSubmit={handleCreateTenant}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="tenant-name">
                  Shop Name <span className="required">*</span>
                </label>
                <input
                  id="tenant-name"
                  type="text"
                  className="form-input"
                  placeholder="e.g. Al-Madina Building Materials"
                  value={createForm.name}
                  onChange={(e) => {
                    const name = e.target.value;
                    const autoSlug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
                    setCreateForm((prev) => ({
                      ...prev,
                      name,
                      code: prev.code || autoSlug,
                    }));
                  }}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="tenant-code">
                  Unique Slug Code <span className="required">*</span>
                </label>
                <input
                  id="tenant-code"
                  type="text"
                  className="form-input"
                  placeholder="al-madina-materials"
                  value={createForm.code}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, code: e.target.value }))}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="owner-email">
                  Owner Email Address <span className="required">*</span>
                </label>
                <input
                  id="owner-email"
                  type="email"
                  className="form-input"
                  placeholder="owner@materials-yard.com"
                  value={createForm.initialOwnerEmail}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, initialOwnerEmail: e.target.value }))}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="owner-name">
                  Owner Full Name <span className="required">*</span>
                </label>
                <input
                  id="owner-name"
                  type="text"
                  className="form-input"
                  placeholder="Rashid Al-Khatib"
                  value={createForm.initialOwnerFullName}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, initialOwnerFullName: e.target.value }))}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="owner-password">
                Initial Owner Password
              </label>
              <input
                id="owner-password"
                type="password"
                className="form-input"
                placeholder="Defaults to Admin@123456 if left blank"
                value={createForm.initialOwnerPassword}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, initialOwnerPassword: e.target.value }))}
              />
              <span style={{ fontSize: '11px', color: 'var(--muted)', marginTop: '4px', display: 'block' }}>
                Password the shop owner will use to log into the Shop Portal (min 6 characters).
              </span>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="tenant-plan">
                  Commercial Plan Tier
                </label>
                <select
                  id="tenant-plan"
                  className="form-select"
                  value={createForm.planCode}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, planCode: e.target.value }))}
                >
                  {availablePlans.map((p) => (
                    <option key={p.id} value={p.code}>
                      {p.name} ({p.currency} {p.price}/{p.billingInterval.toLowerCase()})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="tenant-currency">
                  Operating Currency
                </label>
                <input
                  id="tenant-currency"
                  type="text"
                  className="form-input"
                  value={createForm.currency}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, currency: e.target.value }))}
                />
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowCreateModal(false)}
                disabled={creating}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={creating}
              >
                {creating ? 'Creating Shop...' : 'Create Tenant Shop'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Inspect Decoupled Operational Status & Subscription Lifecycle */}
      {inspectStatus && (
        <Modal
          title={`Status Breakdown: ${inspectStatus.name}`}
          subtitle={`Identifier: ${inspectStatus.code}`}
          onClose={() => setInspectStatus(null)}
          maxWidth="640px"
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '16px',
              backgroundColor: 'var(--canvas)',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border)',
              marginBottom: '20px',
            }}
          >
            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Operational Status
              </span>
              <div style={{ marginTop: '4px' }}>
                <StatusBadge status={inspectStatus.operationalStatus} />
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Subscription Lifecycle
              </span>
              <div style={{ marginTop: '4px' }}>
                <StatusBadge status={inspectStatus.effectiveStatus || inspectStatus.lifecycleStatus || 'ACTIVE'} />
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                Days Remaining
              </span>
              <div style={{ fontSize: '18px', fontWeight: 800, color: 'var(--ink)', marginTop: '2px' }}>
                {inspectStatus.daysRemaining ?? '—'}
              </div>
            </div>
          </div>

          <div className="card detail-card" style={{ padding: '16px' }}>
            <div className="detail-card-head" style={{ marginBottom: '12px' }}>
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers size={16} style={{ color: 'var(--teal)' }} /> Subscription Lifecycle Details
              </h3>
            </div>

            <div className="data-kv-grid">
              <div className="data-kv-label">Plan Tier</div>
              <div className="data-kv-value">
                {inspectStatus.planName || inspectStatus.planCode || 'Standard Tier'}
              </div>

              <div className="data-kv-label">Current Period</div>
              <div className="data-kv-value">
                {formatDate(inspectStatus.currentPeriodStart)} — {formatDate(inspectStatus.currentPeriodEnd)}
              </div>

              {inspectStatus.trialStart && (
                <>
                  <div className="data-kv-label">Trial Period</div>
                  <div className="data-kv-value">
                    {formatDate(inspectStatus.trialStart)} — {formatDate(inspectStatus.trialEnd)}
                  </div>
                </>
              )}

              <div className="data-kv-label">Cancel at End</div>
              <div className="data-kv-value">
                {inspectStatus.cancelAtPeriodEnd ? 'Yes (Scheduled for cancellation)' : 'No (Continuous)'}
              </div>

              {inspectStatus.onboardingReference && (
                <>
                  <div className="data-kv-label">Onboarding Ref</div>
                  <div className="data-kv-value mono-text">{inspectStatus.onboardingReference}</div>
                </>
              )}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setInspectStatus(null)}
            >
              Close
            </button>
          </div>
        </Modal>
      )}

      {/* Modal: Change Operational Status (Suspend / Reactivate) */}
      {statusTarget && (
        <Modal
          title={newStatus === 'SUSPENDED' ? `Suspend ${statusTarget.name}?` : `Reactivate ${statusTarget.name}?`}
          onClose={() => setStatusTarget(null)}
          maxWidth="500px"
        >
          <div
            style={{
              padding: '12px 14px',
              backgroundColor: newStatus === 'SUSPENDED' ? 'var(--danger-bg)' : 'var(--success-bg)',
              color: newStatus === 'SUSPENDED' ? 'var(--danger)' : 'var(--success)',
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
              {newStatus === 'SUSPENDED'
                ? 'Suspending this tenant will immediately block all staff users from creating products, recording stock movements, or processing sales. Existing data will be preserved.'
                : 'Reactivating this tenant restores operational access to products, receiving, and counter sales for authorized shop staff.'}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="status-reason">
              Reason for Status Change (Audit Record)
            </label>
            <textarea
              id="status-reason"
              className="form-textarea"
              placeholder={newStatus === 'SUSPENDED' ? 'e.g. Terms violation, requested seasonal suspension...' : 'e.g. Account reinstated after review...'}
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              disabled={updatingStatus}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setStatusTarget(null)}
              disabled={updatingStatus}
            >
              Cancel
            </button>
            <button
              type="button"
              className={newStatus === 'SUSPENDED' ? 'btn btn-danger' : 'btn btn-primary'}
              onClick={handleConfirmStatusChange}
              disabled={updatingStatus}
            >
              {updatingStatus ? 'Updating Status...' : newStatus === 'SUSPENDED' ? 'Confirm Suspension' : 'Confirm Reactivation'}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};
