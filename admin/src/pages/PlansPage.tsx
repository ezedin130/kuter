import { useState, useEffect, useCallback } from 'react';
import {
  Plus,
  Edit2,
  Trash2,
  CheckCircle2,
  XCircle,
  Eye,
  EyeOff,
  Boxes,
  Users,
  Calendar,
} from 'lucide-react';
import { apiClient } from '../api/client';
import {
  SubscriptionPlanDto,
  CreateSubscriptionPlanDto,
  UpdateSubscriptionPlanDto,
  BillingInterval,
} from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Alert } from '../components/Alert';
import { DataTable, Column } from '../components/DataTable';
import { useLanguage } from '../i18n/LanguageContext';

export const PlansPage: React.FC = () => {
  const { t, language } = useLanguage();
  const [plans, setPlans] = useState<SubscriptionPlanDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [activeFilter, setActiveFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [editingPlan, setEditingPlan] = useState<SubscriptionPlanDto | null>(null);
  const [deletingPlan, setDeletingPlan] = useState<SubscriptionPlanDto | null>(null);
  const [actionLoading, setActionLoading] = useState<boolean>(false);

  // Create form
  const [createForm, setCreateForm] = useState<CreateSubscriptionPlanDto>({
    code: '',
    name: '',
    description: '',
    price: 49.0,
    currency: 'ETB',
    billingInterval: 'MONTHLY',
    productLimit: 500,
    userLimit: 3,
    branchLimit: 1,
    trialDays: 14,
    isPublic: true,
    isActive: true,
    featuresSummary: 'Dual-UOM stock conversion, inventory ledger, POS checkout',
  });

  // Edit form
  const [editForm, setEditForm] = useState<UpdateSubscriptionPlanDto>({
    name: '',
    description: '',
    price: 49.0,
    currency: 'ETB',
    billingInterval: 'MONTHLY',
    productLimit: 500,
    userLimit: 3,
    branchLimit: 1,
    trialDays: 14,
    isPublic: true,
    isActive: true,
    featuresSummary: '',
  });

  // Notifications
  const [banner, setBanner] = useState<{ type: 'success' | 'danger' | 'info'; message: string } | null>(null);

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.listPlans({
        isActive: activeFilter === 'ALL' ? undefined : activeFilter === 'ACTIVE',
        search: search.trim() || undefined,
        page,
        size: 10,
      });
      setPlans(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነት እቅዶችን ማምጣት አልተቻለም።' : 'Failed to retrieve subscription plans.'));
    } finally {
      setLoading(false);
    }
  }, [activeFilter, search, page, language]);

  useEffect(() => {
    fetchPlans();
  }, [fetchPlans]);

  const handleCreatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.code.trim() || !createForm.name.trim()) {
      setBanner({
        type: 'danger',
        message: language === 'am' ? 'የእቅድ ኮድ እና ስም ማስገባት ግዴታ ነው።' : 'Plan code and name are required.',
      });
      return;
    }

    setActionLoading(true);
    try {
      await apiClient.createPlan({
        ...createForm,
        code: createForm.code.trim().toLowerCase(),
        price: Number(createForm.price),
        productLimit: createForm.productLimit ? Number(createForm.productLimit) : undefined,
        userLimit: createForm.userLimit ? Number(createForm.userLimit) : undefined,
        branchLimit: createForm.branchLimit ? Number(createForm.branchLimit) : undefined,
      });
      setBanner({
        type: 'success',
        message: language === 'am' ? `"${createForm.name}" የተባለው እቅድ በተሳካ ሁኔታ ተፈጥሯል።` : `Plan "${createForm.name}" created successfully.`,
      });
      setShowCreateModal(false);
      await fetchPlans();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነት እቅዱን መፍጠር አልተቻለም።' : 'Failed to create subscription plan.'),
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartEdit = (plan: SubscriptionPlanDto) => {
    setEditingPlan(plan);
    setEditForm({
      name: plan.name,
      description: plan.description || '',
      price: plan.price,
      currency: plan.currency,
      billingInterval: plan.billingInterval,
      productLimit: plan.productLimit,
      userLimit: plan.userLimit,
      branchLimit: plan.branchLimit,
      trialDays: plan.trialDays,
      isPublic: plan.isPublic,
      isActive: plan.isActive,
      featuresSummary: plan.featuresSummary || '',
    });
  };

  const handleUpdatePlan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingPlan) return;

    setActionLoading(true);
    try {
      await apiClient.updatePlan(editingPlan.id, {
        ...editForm,
        price: Number(editForm.price),
        productLimit: editForm.productLimit ? Number(editForm.productLimit) : undefined,
        userLimit: editForm.userLimit ? Number(editForm.userLimit) : undefined,
        branchLimit: editForm.branchLimit ? Number(editForm.branchLimit) : undefined,
      });
      setBanner({
        type: 'success',
        message: language === 'am' ? `"${editForm.name}" የተባለው እቅድ በተሳካ ሁኔታ ተሻሽሏል።` : `Plan "${editForm.name}" updated successfully.`,
      });
      setEditingPlan(null);
      await fetchPlans();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነት እቅዱን ማሻሻል አልተቻለም።' : 'Failed to update subscription plan.'),
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleStatus = async (plan: SubscriptionPlanDto) => {
    setActionLoading(true);
    try {
      await apiClient.updatePlanStatus(plan.id, !plan.isActive);
      setBanner({
        type: 'success',
        message: language === 'am'
          ? `የ"${plan.name}" ሁኔታ ወደ ${!plan.isActive ? 'ንቁ' : 'ቦዝኗል'} ተቀይሯል።`
          : `Plan "${plan.name}" status switched to ${!plan.isActive ? 'Active' : 'Inactive'}.`,
      });
      await fetchPlans();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የእቅዱን ሁኔታ መቀየር አልተቻለም።' : 'Failed to toggle plan status.'),
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeletePlan = async () => {
    if (!deletingPlan) return;
    setActionLoading(true);
    try {
      await apiClient.deletePlan(deletingPlan.id);
      setBanner({
        type: 'success',
        message: language === 'am' ? `"${deletingPlan.name}" የተባለው እቅድ ተሰርዟል።` : `Plan "${deletingPlan.name}" removed successfully.`,
      });
      setDeletingPlan(null);
      await fetchPlans();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'እቅዱን መሰረዝ አልተቻለም። በስራ ላይ ያሉ እቅዶች ሊሰረዙ አይችሉም።' : 'Failed to delete plan. Plans in use cannot be deleted.'),
      });
      setDeletingPlan(null);
    } finally {
      setActionLoading(false);
    }
  };

  const formatPrice = (price: number, _currency: string, interval: BillingInterval) => {
    const formatted = Number(price).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    const intervalLabel = interval === 'MONTHLY'
      ? (language === 'am' ? 'በወር' : 'month')
      : (language === 'am' ? 'በዓመት' : 'year');
    return `${formatted} ${t('common.currencySymbol')} / ${intervalLabel}`;
  };

  const columns: Column<SubscriptionPlanDto>[] = [
    {
      key: 'code',
      header: t('plans.code'),
      width: '120px',
      render: (p) => <span className="mono-text">{p.code}</span>,
    },
    {
      key: 'name',
      header: t('plans.planName'),
      render: (p) => (
        <div>
          <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{p.name}</div>
          <div style={{ fontSize: '12px', color: 'var(--muted)', maxWidth: '280px' }}>
            {p.description || p.featuresSummary || (language === 'am' ? 'ምንም ማጠቃለያ የለም' : 'No summary')}
          </div>
        </div>
      ),
    },
    {
      key: 'pricing',
      header: `${t('plans.price')} / ${t('plans.billingInterval')}`,
      render: (p) => (
        <span style={{ fontWeight: 700, color: 'var(--ink)' }}>
          {formatPrice(p.price, p.currency, p.billingInterval)}
        </span>
      ),
    },
    {
      key: 'quotas',
      header: language === 'am' ? 'ገደቦች እና ኮታዎች' : 'Entitlements & Quotas',
      render: (p) => (
        <div style={{ fontSize: '12px', color: 'var(--muted)', display: 'flex', flexDirection: 'column', gap: '2px' }}>
          <span>
            <Boxes size={12} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
            {p.productLimit ? `${p.productLimit} ${language === 'am' ? 'ምርቶች' : 'Products'}` : (language === 'am' ? 'ያልተገደበ ካታሎግ' : 'Unlimited Catalog')}
          </span>
          <span>
            <Users size={12} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
            {p.userLimit ? `${p.userLimit} ${language === 'am' ? 'የሰራተኞች መቀመጫ' : 'Staff Seats'}` : (language === 'am' ? 'ያልተገደበ መቀመጫ' : 'Unlimited Seats')}
          </span>
        </div>
      ),
    },
    {
      key: 'trialDays',
      header: t('plans.trialDays'),
      render: (p) => (
        <span style={{ fontSize: '12.5px' }}>
          <Calendar size={12} style={{ verticalAlign: 'middle', marginRight: '4px', color: 'var(--muted)' }} />
          {p.trialDays} {language === 'am' ? 'ቀናት' : 'Days'}
        </span>
      ),
    },
    {
      key: 'visibility',
      header: t('plans.visibility'),
      render: (p) => (
        <span style={{ fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}>
          {p.isPublic ? (
            <>
              <Eye size={13} style={{ color: 'var(--teal)' }} /> {t('plans.publicPlan')}
            </>
          ) : (
            <>
              <EyeOff size={13} style={{ color: 'var(--faint)' }} /> {t('plans.privatePlan')}
            </>
          )}
        </span>
      ),
    },
    {
      key: 'status',
      header: t('common.status'),
      render: (p) => (
        <StatusBadge status={p.isActive ? 'ACTIVE' : 'INACTIVE'} />
      ),
    },
    {
      key: 'actions',
      header: t('common.actions'),
      width: '140px',
      render: (p) => (
        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleStartEdit(p)}
            title={t('plans.edit')}
          >
            <Edit2 size={13} />
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleToggleStatus(p)}
            title={p.isActive ? t('tenants.suspend') : t('tenants.activate')}
            disabled={actionLoading}
          >
            {p.isActive ? (
              <XCircle size={13} style={{ color: 'var(--warning)' }} />
            ) : (
              <CheckCircle2 size={13} style={{ color: 'var(--success)' }} />
            )}
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            style={{ color: 'var(--danger)' }}
            onClick={() => setDeletingPlan(p)}
            title={t('plans.delete')}
            disabled={actionLoading}
          >
            <Trash2 size={13} />
          </button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHead
        eyebrow={t('plans.eyebrow')}
        title={t('plans.title')}
        description={t('plans.description')}
        actions={
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setShowCreateModal(true)}
          >
            <Plus size={15} /> {t('plans.createPlan')}
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

      {/* Main Plans Data Table */}
      <DataTable
        columns={columns}
        data={plans}
        loading={loading}
        error={error}
        onRetry={fetchPlans}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        searchPlaceholder={t('plans.searchPlaceholder')}
        searchValue={search}
        onSearchChange={(val) => {
          setSearch(val);
          setPage(0);
        }}
        filterComponent={
          <div className="filter-pill-tabs">
            {(['ALL', 'ACTIVE', 'INACTIVE'] as const).map((st) => (
              <button
                key={st}
                type="button"
                className={`filter-pill-btn ${activeFilter === st ? 'active' : ''}`}
                onClick={() => {
                  setActiveFilter(st);
                  setPage(0);
                }}
              >
                {st === 'ALL'
                  ? t('common.all')
                  : st === 'ACTIVE'
                  ? t('common.active')
                  : t('common.inactive')}
              </button>
            ))}
          </div>
        }
        emptyTitle={language === 'am' ? 'ምንም የደንበኝነት እቅድ አልተገኘም' : 'No subscription plans found'}
        emptyMessage={language === 'am' ? 'ከፍለጋ መስፈርትዎ ጋር የሚዛመድ ምንም እቅድ የለም።' : 'There are currently no commercial plans matching your search criteria.'}
        rowKey={(p) => p.id}
      />

      {/* Modal: Create Plan */}
      {showCreateModal && (
        <Modal
          title={t('plans.createTitle')}
          subtitle={t('plans.createSubtitle')}
          onClose={() => setShowCreateModal(false)}
          maxWidth="640px"
        >
          <form onSubmit={handleCreatePlan}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="plan-code">
                  {t('plans.code')} <span className="required">*</span>
                </label>
                <input
                  id="plan-code"
                  type="text"
                  className="form-input"
                  placeholder="e.g. enterprise-growth"
                  value={createForm.code}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, code: e.target.value }))}
                  required
                />
                <p className="form-help">{language === 'am' ? 'ልዩ መለያ በትንንሽ እንግሊዝኛ ፊደላት እና ሰረዝ (ከ 2–50 ፊደላት)።' : 'Unique identifier lowercase, digits, hyphens (2–50 chars).'}</p>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="plan-name">
                  {t('plans.planName')} <span className="required">*</span>
                </label>
                <input
                  id="plan-name"
                  type="text"
                  className="form-input"
                  placeholder={language === 'am' ? 'ለምሳሌ፡ የንግድ ደረጃ' : 'e.g. Enterprise Tier'}
                  value={createForm.name}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, name: e.target.value }))}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="plan-price">
                  {t('plans.price')} ({t('common.currencySymbol')}) <span className="required">*</span>
                </label>
                <input
                  id="plan-price"
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  value={createForm.price}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="plan-interval">
                  {t('plans.billingInterval')} <span className="required">*</span>
                </label>
                <select
                  id="plan-interval"
                  className="form-select"
                  value={createForm.billingInterval}
                  onChange={(e) =>
                    setCreateForm((prev) => ({ ...prev, billingInterval: e.target.value as BillingInterval }))
                  }
                >
                  <option value="MONTHLY">{t('plans.monthly')}</option>
                  <option value="ANNUAL">{t('plans.annual')}</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="plan-prod-limit">
                  {t('plans.productLimit')}
                </label>
                <input
                  id="plan-prod-limit"
                  type="number"
                  className="form-input"
                  placeholder={language === 'am' ? 'ለምሳሌ፡ 1000 (ያልተገደበ እንዲሆን ባዶ ይተውት)' : 'e.g. 1000 (leave empty for unlimited)'}
                  value={createForm.productLimit ?? ''}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      productLimit: e.target.value ? parseInt(e.target.value) : undefined,
                    }))
                  }
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="plan-user-limit">
                  {t('plans.userLimit')}
                </label>
                <input
                  id="plan-user-limit"
                  type="number"
                  className="form-input"
                  placeholder={language === 'am' ? 'ለምሳሌ፡ 5 (ያልተገደበ እንዲሆን ባዶ ይተውት)' : 'e.g. 5 (leave empty for unlimited)'}
                  value={createForm.userLimit ?? ''}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      userLimit: e.target.value ? parseInt(e.target.value) : undefined,
                    }))
                  }
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="plan-trial-days">
                  {t('plans.trialDays')}
                </label>
                <input
                  id="plan-trial-days"
                  type="number"
                  className="form-input"
                  value={createForm.trialDays ?? 14}
                  onChange={(e) =>
                    setCreateForm((prev) => ({
                      ...prev,
                      trialDays: parseInt(e.target.value) || 0,
                    }))
                  }
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="plan-desc">
                {language === 'am' ? 'የእቅዱ ማብራሪያ' : 'Description / Pitch'}
              </label>
              <textarea
                id="plan-desc"
                className="form-textarea"
                placeholder={language === 'am' ? 'እያደጉ ላሉ የንግድ ተቋማት ተስማሚ የሆነ ጥቅል...' : 'Comprehensive tier suited for growing supply depots...'}
                value={createForm.description}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, description: e.target.value }))}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="plan-features">
                {t('plans.formFeatures')}
              </label>
              <input
                id="plan-features"
                type="text"
                className="form-input"
                placeholder="Dual-UOM stock conversion, inventory ledger, POS checkout, daily reports"
                value={createForm.featuresSummary}
                onChange={(e) => setCreateForm((prev) => ({ ...prev, featuresSummary: e.target.value }))}
              />
            </div>

            <div style={{ display: 'flex', gap: '20px', margin: '14px 0' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={createForm.isPublic}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, isPublic: e.target.checked }))}
                />
                <span>{language === 'am' ? 'በይፋዊ የዋጋ ዝርዝር ላይ ይታይ' : 'Publish to Public Pricing Directory'}</span>
              </label>

              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={createForm.isActive}
                  onChange={(e) => setCreateForm((prev) => ({ ...prev, isActive: e.target.checked }))}
                />
                <span>{language === 'am' ? 'ለንግድ ተከራይ ምደባ ንቁ ይሁን' : 'Active for Tenant Assignment'}</span>
              </label>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowCreateModal(false)}
                disabled={actionLoading}
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? t('plans.saving') : t('plans.createPlan')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Edit Plan */}
      {editingPlan && (
        <Modal
          title={`${t('plans.editTitle')}: ${editingPlan.name}`}
          subtitle={`${t('plans.code')}: ${editingPlan.code}`}
          onClose={() => setEditingPlan(null)}
          maxWidth="640px"
        >
          <form onSubmit={handleUpdatePlan}>
            <div className="form-group">
              <label className="form-label" htmlFor="edit-plan-name">
                {t('plans.planName')} <span className="required">*</span>
              </label>
              <input
                id="edit-plan-name"
                type="text"
                className="form-input"
                value={editForm.name}
                onChange={(e) => setEditForm((prev) => ({ ...prev, name: e.target.value }))}
                required
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="edit-plan-price">
                  {t('plans.price')} ({t('common.currencySymbol')}) <span className="required">*</span>
                </label>
                <input
                  id="edit-plan-price"
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  value={editForm.price}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, price: parseFloat(e.target.value) || 0 }))}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-plan-interval">
                  {t('plans.billingInterval')} <span className="required">*</span>
                </label>
                <select
                  id="edit-plan-interval"
                  className="form-select"
                  value={editForm.billingInterval}
                  onChange={(e) =>
                    setEditForm((prev) => ({ ...prev, billingInterval: e.target.value as BillingInterval }))
                  }
                >
                  <option value="MONTHLY">{t('plans.monthly')}</option>
                  <option value="ANNUAL">{t('plans.annual')}</option>
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label" htmlFor="edit-prod-limit">
                  {t('plans.productLimit')}
                </label>
                <input
                  id="edit-prod-limit"
                  type="number"
                  className="form-input"
                  value={editForm.productLimit ?? ''}
                  onChange={(e) =>
                    setEditForm((prev) => ({
                      ...prev,
                      productLimit: e.target.value ? parseInt(e.target.value) : undefined,
                    }))
                  }
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-user-limit">
                  {t('plans.userLimit')}
                </label>
                <input
                  id="edit-user-limit"
                  type="number"
                  className="form-input"
                  value={editForm.userLimit ?? ''}
                  onChange={(e) =>
                    setEditForm((prev) => ({
                      ...prev,
                      userLimit: e.target.value ? parseInt(e.target.value) : undefined,
                    }))
                  }
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="edit-plan-desc">
                {language === 'am' ? 'የእቅዱ ማብራሪያ' : 'Description'}
              </label>
              <textarea
                id="edit-plan-desc"
                className="form-textarea"
                value={editForm.description}
                onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="edit-plan-features">
                {t('plans.formFeatures')}
              </label>
              <input
                id="edit-plan-features"
                type="text"
                className="form-input"
                value={editForm.featuresSummary}
                onChange={(e) => setEditForm((prev) => ({ ...prev, featuresSummary: e.target.value }))}
              />
            </div>

            <div style={{ display: 'flex', gap: '20px', margin: '14px 0' }}>
              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={editForm.isPublic}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, isPublic: e.target.checked }))}
                />
                <span>{language === 'am' ? 'በይፋዊ የዋጋ ዝርዝር ላይ ይታይ' : 'Publicly Visible'}</span>
              </label>

              <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', cursor: 'pointer' }}>
                <input
                  type="checkbox"
                  checked={editForm.isActive}
                  onChange={(e) => setEditForm((prev) => ({ ...prev, isActive: e.target.checked }))}
                />
                <span>{language === 'am' ? 'ንቁ ይሁን' : 'Active'}</span>
              </label>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setEditingPlan(null)}
                disabled={actionLoading}
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? t('plans.saving') : t('common.save')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Confirmation Dialog: Delete Plan */}
      <ConfirmDialog
        isOpen={!!deletingPlan}
        title={`${t('plans.delete')}: ${deletingPlan?.name}?`}
        message={t('plans.deleteConfirmMsg')}
        tone="danger"
        confirmLabel={t('plans.delete')}
        isLoading={actionLoading}
        onConfirm={handleDeletePlan}
        onCancel={() => setDeletingPlan(null)}
      />
    </div>
  );
};
