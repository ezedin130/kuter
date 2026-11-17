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
import { useLanguage } from '../i18n/LanguageContext';

export const SubscriptionsPage: React.FC = () => {
  const { t, formatDate, language } = useLanguage();
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
      setError(err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነት መረጃዎችን ማምጣት አልተቻለም።' : 'Failed to retrieve subscriptions.'));
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, page, language]);

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
      setBanner({
        type: 'success',
        message: language === 'am' ? 'የደንበኝነት እቅዱ ለተከራዩ ሱቅ በተሳካ ሁኔታ ተመድቧል።' : 'Subscription successfully assigned to shop tenant.',
      });
      setAssignModal(false);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'እቅዱን መመደብ አልተቻለም።' : 'Failed to assign subscription.'),
      });
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
        message: language === 'am'
          ? `የ"${extendTarget.tenantName}" ደንበኝነት በ ${extendDays} ቀናት ተራዝሟል።`
          : `Subscription for "${extendTarget.tenantName}" extended by ${extendDays} days.`,
      });
      setExtendTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'ደንበኝነቱን ማራዘም አልተቻለም።' : 'Failed to extend subscription.'),
      });
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
        message: language === 'am'
          ? `የ"${changePlanTarget.tenantName}" የደንበኝነት እቅድ በተሳካ ሁኔታ ተቀይሯል።`
          : `Subscription plan updated for "${changePlanTarget.tenantName}".`,
      });
      setChangePlanTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነት እቅዱን መቀየር አልተቻለም።' : 'Failed to change subscription plan.'),
      });
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
        setBanner({
          type: 'success',
          message: language === 'am' ? `የ"${sub.tenantName}" ደንበኝነት ታግዷል።` : `Subscription for "${sub.tenantName}" has been suspended.`,
        });
      } else if (action === 'reactivate') {
        await apiClient.reactivateSubscription(sub.id, actionReason.trim() || undefined);
        setBanner({
          type: 'success',
          message: language === 'am' ? `የ"${sub.tenantName}" ደንበኝነት እንደገና ነቅቷል።` : `Subscription for "${sub.tenantName}" has been reactivated.`,
        });
      } else if (action === 'cancel') {
        await apiClient.cancelSubscription(sub.id, {
          cancelAtPeriodEnd,
          reason: actionReason.trim() || 'Administrative cancellation',
        });
        setBanner({
          type: 'success',
          message: language === 'am' ? `የ"${sub.tenantName}" ደንበኝነት ተሰርዟል።` : `Subscription for "${sub.tenantName}" has been cancelled.`,
        });
      }
      setStatusTarget(null);
      await fetchSubscriptions();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : (language === 'am' ? 'የደንበኝነቱን ሁኔታ ማሻሻል አልተቻለም።' : 'Failed to update subscription status.'),
      });
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

  const formatPrice = (price: number, _currency: string, interval: string) => {
    const formatted = Number(price).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    const intervalLabel = interval.toUpperCase() === 'MONTHLY'
      ? (language === 'am' ? 'በወር' : 'month')
      : (language === 'am' ? 'በዓመት' : 'year');
    return `${formatted} ${t('common.currencySymbol')} / ${intervalLabel}`;
  };

  const columns: Column<AdminSubscriptionDetailDto>[] = [
    {
      key: 'tenant',
      header: t('subscriptions.shop'),
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
      header: t('subscriptions.plan'),
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
      header: language === 'am' ? 'የአቅም ገደቦች እና አጠቃቀም' : 'Capacity Quotas & Resource Consumption',
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
                <span style={{ fontWeight: 600 }}>{language === 'am' ? 'ምርቶች:' : 'Products:'}</span>
                <span>
                  <strong>{prodCount}</strong> / {prodLimit ? `${prodLimit}` : (language === 'am' ? 'ያልተገደበ' : 'Unlimited')}
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
                <span style={{ fontWeight: 600 }}>{language === 'am' ? 'ሰራተኞች:' : 'Staff Accounts:'}</span>
                <span>
                  <strong>{userCount}</strong> / {userLimit ? `${userLimit}` : (language === 'am' ? 'ያልተገደበ' : 'Unlimited')}
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
      header: t('common.status'),
      render: (s) => (
        <div>
          <StatusBadge status={s.effectiveStatus || s.status} />
          {s.cancelAtPeriodEnd && (
            <div style={{ fontSize: '10.5px', color: 'var(--warning)', fontWeight: 600, marginTop: '2px' }}>
              {language === 'am' ? 'በጊዜ ማብቂያ ላይ ይሰረዛል' : 'Cancelling at period end'}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'daysRemaining',
      header: language === 'am' ? 'የቀሩ ቀናት' : 'Days Left',
      render: (s) => (
        <span
          style={{
            fontWeight: 700,
            fontSize: '13px',
            color: s.daysRemaining <= 7 ? 'var(--warning)' : 'var(--ink)',
          }}
        >
          {s.daysRemaining} {language === 'am' ? 'ቀናት' : 'days'}
        </span>
      ),
    },
    {
      key: 'period',
      header: language === 'am' ? 'የአሁኑ ዑደት' : 'Current Period',
      render: (s) => (
        <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
          <div>{t('subscriptions.periodStart')}: {formatDate(s.currentPeriodStart)}</div>
          <div>{t('subscriptions.periodEnd')}: {formatDate(s.currentPeriodEnd)}</div>
        </div>
      ),
    },
    {
      key: 'actions',
      header: t('common.actions'),
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
            title={t('subscriptions.extend')}
          >
            {t('subscriptions.extend')}
          </button>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              setChangePlanTarget(s);
              setChangePlanReason('');
            }}
            title={t('subscriptions.changePlan')}
          >
            {t('subscriptions.changePlan')}
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
              title={t('subscriptions.suspend')}
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
              title={t('subscriptions.reactivate')}
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
              title={t('subscriptions.cancel')}
            >
              <XCircle size={13} />
            </button>
          )}

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleViewHistory(s)}
            title={t('subscriptions.history')}
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
        eyebrow={t('subscriptions.eyebrow')}
        title={t('subscriptions.title')}
        description={t('subscriptions.description')}
        actions={
          <button
            type="button"
            className="btn btn-primary btn-sm"
            onClick={() => setAssignModal(true)}
          >
            <Plus size={15} /> {t('subscriptions.assignPlan')}
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
            <span className="metric-label">{t('overview.activeSubscriptions')}</span>
            <div className="metric-value">{summary.activeSubscriptions}</div>
            <div className="metric-meta positive">
              <CheckCircle2 size={13} />
              <span>
                {language === 'am' ? `ጠቅላላ ${summary.totalSubscriptions} የተመደቡ` : `${summary.totalSubscriptions} total assigned`}
              </span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('TRIALING')}>
            <span className="metric-label">{language === 'am' ? 'ነጻ የሙከራ ጊዜያት' : 'Free Trials'}</span>
            <div className="metric-value" style={{ color: '#1e557b' }}>
              {summary.trialingSubscriptions}
            </div>
            <div className="metric-meta">
              <span>{language === 'am' ? 'በሙከራ ላይ ያሉ መለያዎች' : 'Trialing accounts'}</span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('EXPIRED')}>
            <span className="metric-label">{t('status.EXPIRED')}</span>
            <div className="metric-value" style={{ color: summary.expiredSubscriptions > 0 ? 'var(--warning)' : 'var(--ink)' }}>
              {summary.expiredSubscriptions}
            </div>
            <div className="metric-meta warning">
              <span>{language === 'am' ? 'ማራዘሚያ ወይም እድሳት ያስፈልጋቸዋል' : 'Requires extension or renewal'}</span>
            </div>
          </div>

          <div className="metric-card" onClick={() => setStatusFilter('SUSPENDED')}>
            <span className="metric-label">{t('status.SUSPENDED')}</span>
            <div className="metric-value" style={{ color: summary.suspendedSubscriptions > 0 ? 'var(--danger)' : 'var(--ink)' }}>
              {summary.suspendedSubscriptions}
            </div>
            <div className="metric-meta">
              <span>{language === 'am' ? 'በአስተዳዳሪ የታገዱ' : 'Admin frozen'}</span>
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
        searchPlaceholder={t('subscriptions.searchPlaceholder')}
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
                {st === 'ALL'
                  ? t('common.all')
                  : t(`status.${st}`)}
              </button>
            ))}
          </div>
        }
        emptyTitle={language === 'am' ? 'ምንም የሱቅ ደንበኝነት አልተገኘም' : 'No tenant subscriptions found'}
        emptyMessage={language === 'am' ? 'ከፍለጋ መስፈርትዎ ጋር የሚዛመድ ምንም ንቁ ወይም ታሪካዊ ደንበኝነት የለም።' : 'There are currently no active or historical subscriptions matching your filter.'}
        rowKey={(s) => s.id}
      />

      {/* Modal: Assign Plan */}
      {assignModal && (
        <Modal
          title={t('subscriptions.assignTitle')}
          subtitle={t('subscriptions.assignSubtitle')}
          onClose={() => setAssignModal(false)}
        >
          <form onSubmit={handleAssign}>
            <div className="form-group">
              <label className="form-label" htmlFor="assign-tenant">
                {t('subscriptions.selectShop')} <span className="required">*</span>
              </label>
              <select
                id="assign-tenant"
                className="form-select"
                value={assignTenantId}
                onChange={(e) => setAssignTenantId(e.target.value)}
                required
              >
                {availableTenants.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name} ({item.code})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="assign-plan">
                {t('subscriptions.selectPlan')} <span className="required">*</span>
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
                    {p.name} ({p.price} {t('common.currencySymbol')}/{p.billingInterval.toLowerCase()})
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
                <span>{t('subscriptions.startWithTrial')}</span>
              </label>
            </div>

            {assignTrial && (
              <div className="form-group">
                <label className="form-label" htmlFor="assign-days">
                  {t('subscriptions.trialDaysCount')}
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
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? t('subscriptions.assigning') : t('subscriptions.assignPlan')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Extend Subscription */}
      {extendTarget && (
        <Modal
          title={`${t('subscriptions.extendTitle')}: ${extendTarget.tenantName}`}
          subtitle={`${t('subscriptions.plan')}: ${extendTarget.planName} · ${t('subscriptions.periodEnd')}: ${formatDate(extendTarget.currentPeriodEnd)}`}
          onClose={() => setExtendTarget(null)}
        >
          <form onSubmit={handleExtend}>
            <div className="form-group">
              <label className="form-label" htmlFor="extend-days">
                {t('subscriptions.extensionDays')} <span className="required">*</span>
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
              <p className="form-help">
                {language === 'am'
                  ? 'የአሁኑን የደንበኝነት ጊዜ ያራዝማል እንዲሁም ጊዜያቸው ያለፈባቸውን ወደ ንቁ ሁኔታ ይመልሳል።'
                  : 'Advances the current period end date and restores expired subscriptions to ACTIVE.'}
              </p>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="extend-reason">
                {t('subscriptions.reason')} <span className="required">*</span>
              </label>
              <textarea
                id="extend-reason"
                className="form-textarea"
                placeholder={language === 'am' ? 'ለምሳሌ፡ የክፍያ ማረጋገጫ ደረሰኝ ስለቀረበ...' : 'e.g. Promotional extension, offline payment receipt confirmed...'}
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
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading || !extendReason.trim()}
              >
                {actionLoading ? t('common.loading') : t('subscriptions.extend')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Switch Plan */}
      {changePlanTarget && (
        <Modal
          title={`${t('subscriptions.changeTitle')}: ${changePlanTarget.tenantName}`}
          subtitle={`${t('subscriptions.plan')}: ${changePlanTarget.planName}`}
          onClose={() => setChangePlanTarget(null)}
        >
          <form onSubmit={handleChangePlan}>
            <div className="form-group">
              <label className="form-label" htmlFor="switch-plan">
                {t('subscriptions.targetPlan')} <span className="required">*</span>
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
                    {p.name} ({p.price} {t('common.currencySymbol')}/{p.billingInterval.toLowerCase()})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="switch-reason">
                {t('subscriptions.reason')}
              </label>
              <textarea
                id="switch-reason"
                className="form-textarea"
                placeholder={language === 'am' ? 'ለምሳሌ፡ በባለቤቱ ጥያቄ መሰረት ወደ ከፍተኛ ደረጃ የተቀየረ...' : 'e.g. Upgraded to Enterprise tier per shop owner request...'}
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
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={actionLoading}
              >
                {actionLoading ? t('common.loading') : t('subscriptions.changePlan')}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* Modal: Lifecycle Actions (Suspend, Reactivate, Cancel) */}
      {statusTarget && (
        <Modal
          title={
            statusTarget.action === 'suspend'
              ? `${statusTarget.sub.tenantName} ${t('subscriptions.suspend')}?`
              : statusTarget.action === 'reactivate'
              ? `${statusTarget.sub.tenantName} ${t('subscriptions.reactivate')}?`
              : `${statusTarget.sub.tenantName} ${t('subscriptions.cancel')}?`
          }
          subtitle={`${t('subscriptions.shop')}: ${statusTarget.sub.tenantName}`}
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
                <>{language === 'am' ? 'ይህን ደንበኝነት ማገድ ተከራዩ እንደገና እስኪነቃ ድረስ አዳዲስ ምርቶችን እና ሽያጮችን እንዳያከናውን ያግደዋል።' : 'Suspending access disables product mutations and sales processing for this tenant until reactivated.'}</>
              )}
              {statusTarget.action === 'reactivate' && (
                <>{language === 'am' ? 'እንደገና ማግበር የደንበኝነት እቅዱን ወደ ንቁ ሁኔታ ይመልሳል እንዲሁም የሱቅ ስራዎችን ያስጀምራል።' : 'Reactivating restores the subscription to active status and enables tenant workspace operations.'}</>
              )}
              {statusTarget.action === 'cancel' && (
                <>{language === 'am' ? 'መሰረዝ የደንበኝነት ሁኔታውን ወደ "የተሰረዘ" ይቀይረዋል። መረጃዎች ይጠበቃሉ ነገር ግን ተጨማሪ ስራዎች ይታገዳሉ።' : 'Cancelling will transition the subscription to CANCELLED. Data is preserved but further operations will be gated.'}</>
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
                <span>{language === 'am' ? 'በአሁኑ የክፍያ ጊዜ ማብቂያ ላይ ይሰረዝ (እስከ ማብቂያው ድረስ አገልግሎቱ ይቀጥላል)' : 'Cancel at Current Period End (Preserve access until period expiry)'}</span>
              </label>
            </div>
          )}

          <div className="form-group">
            <label className="form-label" htmlFor="lifecycle-reason">
              {t('subscriptions.reason')} <span className="required">*</span>
            </label>
            <textarea
              id="lifecycle-reason"
              className="form-textarea"
              placeholder={language === 'am' ? 'የዚህን ሁኔታ ለውጥ ምክንያት ያስገቡ...' : 'State the justification for this subscription status change...'}
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
              {t('common.cancel')}
            </button>
            <button
              type="button"
              className={statusTarget.action === 'reactivate' ? 'btn btn-primary' : 'btn btn-danger'}
              onClick={handleConfirmLifecycleAction}
              disabled={actionLoading || !actionReason.trim()}
            >
              {actionLoading
                ? t('common.loading')
                : statusTarget.action === 'reactivate'
                ? t('subscriptions.reactivate')
                : statusTarget.action === 'suspend'
                ? t('subscriptions.suspend')
                : t('subscriptions.cancel')}
            </button>
          </div>
        </Modal>
      )}

      {/* Modal: View Subscription History Log */}
      {historyTarget && (
        <Modal
          title={`${t('subscriptions.historyTitle')}: ${historyTarget.tenantName}`}
          subtitle={`${t('subscriptions.plan')}: ${historyTarget.planName}`}
          onClose={() => setHistoryTarget(null)}
          maxWidth="640px"
        >
          {loadingHistory ? (
            <div style={{ padding: '32px', textAlign: 'center', color: 'var(--muted)' }}>
              {t('common.loading')}
            </div>
          ) : historyList.length === 0 ? (
            <div style={{ padding: '32px', textAlign: 'center', color: 'var(--muted)' }}>
              {t('subscriptions.noHistory')}
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
                      {language === 'am' ? 'የሁኔታ ለውጥ: ' : 'Transition: '}
                      {h.fromStatus ? `${h.fromStatus} → ` : ''}
                      <strong>{h.toStatus}</strong>
                      {h.reason && ` · ${t('subscriptions.reason')}: "${h.reason}"`}
                    </div>
                    {h.actorEmail && (
                      <div style={{ fontSize: '11px', color: 'var(--faint)', marginTop: '2px' }}>
                        {language === 'am' ? 'የፈጸመው: ' : 'By: '}{h.actorEmail}
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
              {t('common.close')}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};
