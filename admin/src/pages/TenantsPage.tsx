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
import { useLanguage } from '../i18n/LanguageContext';

export const TenantsPage: React.FC = () => {
  const { t, formatDate, language } = useLanguage();
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
    currency: 'ETB',
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
      setError(err instanceof Error ? err.message : (language === 'am' ? 'የሱቆችን መረጃ ማምጣት አልተቻለም።' : 'Failed to retrieve tenants.'));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, page, language]);

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
      setBanner({
        type: 'danger',
        message: language === 'am'
          ? 'የሱቅ ስም፣ የኮድ መለያ እና የባለቤት ኢሜይል ማስገባት ግዴታ ነው።'
          : 'Shop name, code slug, and owner email are required.',
      });
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
        message: language === 'am'
          ? `"${createForm.name}" የተባለው ሱቅ በተሳካ ሁኔታ ተፈጥሯል።`
          : `Tenant "${createForm.name}" created successfully.`,
      });
      setShowCreateModal(false);
      setCreateForm({
        name: '',
        code: '',
        currency: 'ETB',
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
        message: err instanceof Error ? err.message : (language === 'am' ? 'ሱቁን መፍጠር አልተቻለም።' : 'Failed to create tenant shop.'),
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
        message: err instanceof Error ? err.message : (language === 'am' ? 'የተከራዩን ሁኔታ መመርመር አልተቻለም።' : 'Failed to inspect tenant status.'),
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
        message: language === 'am'
          ? `የ"${statusTarget.name}" የስራ ሁኔታ ወደ ${t(`status.${newStatus}`)} ተቀይሯል።`
          : `Tenant "${statusTarget.name}" operational status updated to ${newStatus}.`,
      });
      setStatusTarget(null);
      await fetchTenants();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የተከራዩን ሁኔታ መቀየር አልተቻለም።' : 'Failed to update tenant status.'),
      });
    } finally {
      setUpdatingStatus(false);
    }
  };

  const columns: Column<TenantSummaryResponse>[] = [
    {
      key: 'code',
      header: t('tenants.code'),
      width: '130px',
      render: (item) => <span className="mono-text">{item.code}</span>,
    },
    {
      key: 'name',
      header: t('tenants.shopName'),
      render: (item) => (
        <div>
          <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{item.name}</div>
          <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
            {t('tenants.code')}: {item.code}
          </div>
        </div>
      ),
    },
    {
      key: 'owner',
      header: t('tenants.owner'),
      render: (item) => (
        <div style={{ fontSize: '12.5px' }}>
          <div style={{ fontWeight: 600 }}>{item.ownerFullName || (language === 'am' ? 'ባለቤት' : 'Owner')}</div>
          <div style={{ fontSize: '11.5px', color: 'var(--muted)' }}>{item.ownerEmail || '—'}</div>
        </div>
      ),
    },
    {
      key: 'status',
      header: t('common.status'),
      render: (item) => <StatusBadge status={item.status} />,
    },
    {
      key: 'subscription',
      header: t('tenants.activeSubscription'),
      render: (item) => (
        <div style={{ fontSize: '12px' }}>
          <span style={{ fontWeight: 700, color: 'var(--teal)' }}>
            {item.activePlanCode || 'STARTER'}
          </span>
          {item.subscriptionStatus && (
            <div style={{ marginTop: '2px' }}>
              <StatusBadge status={item.subscriptionStatus} size="sm" />
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'createdAt',
      header: t('tenants.createdDate'),
      render: (item) => <span style={{ fontSize: '12.5px' }}>{formatDate(item.createdAt)}</span>,
    },
    {
      key: 'actions',
      header: t('common.actions'),
      width: '180px',
      render: (item) => (
        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleInspect(item.id)}
            disabled={inspecting}
            title={t('tenants.inspect')}
          >
            {t('tenants.inspect')}
          </button>

          {item.status === 'ACTIVE' ? (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--danger)' }}
              onClick={() => handleOpenStatusModal(item, 'SUSPENDED')}
              title={t('tenants.suspend')}
            >
              <PauseCircle size={14} /> {t('tenants.suspend')}
            </button>
          ) : (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ color: 'var(--success)' }}
              onClick={() => handleOpenStatusModal(item, 'ACTIVE')}
              title={t('tenants.activate')}
            >
              <PlayCircle size={14} /> {t('tenants.activate')}
            </button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHead
        eyebrow={t('tenants.eyebrow')}
        title={t('tenants.title')}
        description={t('tenants.description')}
        actions={
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setShowCreateModal(true)}
          >
            <Plus size={15} /> {t('tenants.createShop')}
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
        searchPlaceholder={t('tenants.searchPlaceholder')}
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
                {st === 'ALL' ? t('tenants.allStatuses') : t(`status.${st}`)}
              </button>
            ))}
          </div>
        }
        emptyTitle={language === 'am' ? 'ምንም ሱቅ አልተገኘም' : 'No tenant shops found'}
        emptyMessage={language === 'am' ? 'ከፍለጋዎ ወይም ከተመረጠው ሁኔታ ጋር የሚዛመድ ሱቅ የለም።' : 'There are no shops matching your search or operational filter.'}
        rowKey={(item) => item.id}
      />

      {/* Modal: Create Tenant Directly */}
      {showCreateModal && (
        <Modal
          title={t('tenants.createTitle')}
          subtitle={t('tenants.createSubtitle')}
          onClose={() => setShowCreateModal(false)}
          maxWidth="620px"
        >
          <form onSubmit={handleCreateTenant}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="tenant-name">
                  {t('tenants.shopName')} <span className="required">*</span>
                </label>
                <input
                  id="tenant-name"
                  type="text"
                  className="form-input"
                  placeholder={language === 'am' ? 'ለምሳሌ፡ አል-መዲና የህንጻ መሳሪያዎች' : 'e.g. Al-Madina Building Materials'}
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
                  {t('tenants.code')} <span className="required">*</span>
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
                  {t('requests.email')} <span className="required">*</span>
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
                  {t('requests.ownerName')} <span className="required">*</span>
                </label>
                <input
                  id="owner-name"
                  type="text"
                  className="form-input"
                  placeholder={language === 'am' ? 'አበበ ተስፋዬ' : 'Abebe Tesfaye'}
                  value={createForm.initialOwnerFullName}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, initialOwnerFullName: e.target.value }))}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="owner-password">
                {t('requests.temporaryPassword')}
              </label>
              <input
                id="owner-password"
                type="password"
                className="form-input"
                placeholder={language === 'am' ? 'ካልተሞላ በራሱ Admin@123456 ይሆናል' : 'Defaults to Admin@123456 if left blank'}
                value={createForm.initialOwnerPassword}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, initialOwnerPassword: e.target.value }))}
              />
              <span style={{ fontSize: '11px', color: 'var(--muted)', marginTop: '4px', display: 'block' }}>
                {language === 'am' ? 'የሱቁ ባለቤት ወደ ሱቅ ፖርታል ለመግባት የሚጠቀምበት የይለፍ ቃል (ቢያንስ 6 ፊደላት)።' : 'Password the shop owner will use to log into the Shop Portal (min 6 characters).'}
              </span>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="tenant-plan">
                  {t('requests.plan')}
                </label>
                <select
                  id="tenant-plan"
                  className="form-select"
                  value={createForm.planCode}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, planCode: e.target.value }))}
                >
                  {availablePlans.map((p) => (
                    <option key={p.id} value={p.code}>
                      {p.name} ({p.price} {t('common.currencySymbol')}/{p.billingInterval.toLowerCase()})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="tenant-currency">
                  {t('tenants.currency')}
                </label>
                <input
                  id="tenant-currency"
                  type="text"
                  className="form-input"
                  value="ETB (የኢትዮጵያ ብር)"
                  disabled
                  readOnly
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
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={creating}
              >
                {creating ? t('tenants.creating') : t('tenants.createShop')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Inspect Decoupled Operational Status & Subscription Lifecycle */}
      {inspectStatus && (
        <Modal
          title={`${t('tenants.inspectTitle')}: ${inspectStatus.name}`}
          subtitle={`${t('tenants.code')}: ${inspectStatus.code}`}
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
                {t('tenants.operationalStatus')}
              </span>
              <div style={{ marginTop: '4px' }}>
                <StatusBadge status={inspectStatus.operationalStatus} />
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                {t('tenants.activeSubscription')}
              </span>
              <div style={{ marginTop: '4px' }}>
                <StatusBadge status={inspectStatus.effectiveStatus || inspectStatus.lifecycleStatus || 'ACTIVE'} />
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase' }}>
                {language === 'am' ? 'የቀሩ ቀናት' : 'Days Remaining'}
              </span>
              <div style={{ fontSize: '18px', fontWeight: 800, color: 'var(--ink)', marginTop: '2px' }}>
                {inspectStatus.daysRemaining ?? '—'}
              </div>
            </div>
          </div>

          <div className="card detail-card" style={{ padding: '16px' }}>
            <div className="detail-card-head" style={{ marginBottom: '12px' }}>
              <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <Layers size={16} style={{ color: 'var(--teal)' }} /> {language === 'am' ? 'የደንበኝነት ዝርዝር መረጃ' : 'Subscription Lifecycle Details'}
              </h3>
            </div>

            <div className="data-kv-grid">
              <div className="data-kv-label">{t('subscriptions.plan')}</div>
              <div className="data-kv-value">
                {inspectStatus.planName || inspectStatus.planCode || 'Standard Tier'}
              </div>

              <div className="data-kv-label">{language === 'am' ? 'የአሁኑ ዑደት' : 'Current Period'}</div>
              <div className="data-kv-value">
                {formatDate(inspectStatus.currentPeriodStart)} — {formatDate(inspectStatus.currentPeriodEnd)}
              </div>

              {inspectStatus.trialStart && (
                <>
                  <div className="data-kv-label">{language === 'am' ? 'የሙከራ ጊዜ' : 'Trial Period'}</div>
                  <div className="data-kv-value">
                    {formatDate(inspectStatus.trialStart)} — {formatDate(inspectStatus.trialEnd)}
                  </div>
                </>
              )}

              <div className="data-kv-label">{language === 'am' ? 'በጊዜ ማብቂያ ላይ ይሰረዝ?' : 'Cancel at End'}</div>
              <div className="data-kv-value">
                {inspectStatus.cancelAtPeriodEnd
                  ? (language === 'am' ? 'አዎ (እንዲሰረዝ ተይዟል)' : 'Yes (Scheduled for cancellation)')
                  : (language === 'am' ? 'አይ (ቀጣይነት ያለው)' : 'No (Continuous)')}
              </div>

              {inspectStatus.onboardingReference && (
                <>
                  <div className="data-kv-label">{t('requests.referenceCode')}</div>
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
              {t('common.close')}
            </button>
          </div>
        </Modal>
      )}

      {/* Modal: Change Operational Status (Suspend / Reactivate) */}
      {statusTarget && (
        <Modal
          title={
            newStatus === 'SUSPENDED'
              ? `${statusTarget.name} ${t('tenants.suspend')}?`
              : `${statusTarget.name} ${t('tenants.activate')}?`
          }
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
                ? (language === 'am'
                    ? 'ይህን ተከራይ ማገድ ሰራተኞች ምርት እንዳይፈጥሩ፣ የእቃ እንቅስቃሴ እንዳይመዘግቡ ወይም ሽያጭ እንዳያከናውኑ ወዲያውኑ ያግዳቸዋል። ነባር መረጃዎች ይጠበቃሉ።'
                    : 'Suspending this tenant will immediately block all staff users from creating products, recording stock movements, or processing sales. Existing data will be preserved.')
                : (language === 'am'
                    ? 'ይህን ተከራይ እንደገና ማግበር ለተፈቀደላቸው የሱቅ ሰራተኞች የስራ መዳረሻን ሙሉ በሙሉ ይመልሳል።'
                    : 'Reactivating this tenant restores operational access to products, receiving, and counter sales for authorized shop staff.')}
            </div>
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="status-reason">
              {t('tenants.statusReason')}
            </label>
            <textarea
              id="status-reason"
              className="form-textarea"
              placeholder={
                newStatus === 'SUSPENDED'
                  ? (language === 'am' ? 'ለምሳሌ፡ የደንብ ጥሰት ወይም የተጠየቀ ጊዜያዊ እገዳ...' : 'e.g. Terms violation, requested seasonal suspension...')
                  : (language === 'am' ? 'ለምሳሌ፡ ከግምገማ በኋላ የተፈቀደ...' : 'e.g. Account reinstated after review...')
              }
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
              {t('common.cancel')}
            </button>
            <button
              type="button"
              className={newStatus === 'SUSPENDED' ? 'btn btn-danger' : 'btn btn-primary'}
              onClick={handleConfirmStatusChange}
              disabled={updatingStatus}
            >
              {updatingStatus
                ? t('tenants.updating')
                : newStatus === 'SUSPENDED'
                ? t('tenants.suspend')
                : t('tenants.activate')}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};
