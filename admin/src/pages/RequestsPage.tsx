import { useState, useEffect, useCallback } from 'react';
import {
  Check,
  X,
  ArrowLeft,
  ShieldCheck,
  AlertTriangle,
  Building2,
  Mail,
  FileText,
  Calendar,
  CheckCircle2,
} from 'lucide-react';
import { apiClient } from '../api/client';
import {
  OnboardingDetailResponse,
  OnboardingStatus,
  ApprovalResultDto,
} from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { DataTable, Column } from '../components/DataTable';

interface RequestsPageProps {
  onRequestsUpdated?: () => void;
}

export const RequestsPage: React.FC<RequestsPageProps> = ({ onRequestsUpdated }) => {
  const [requests, setRequests] = useState<OnboardingDetailResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<OnboardingStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Selected request for detail review
  const [selectedRequest, setSelectedRequest] = useState<OnboardingDetailResponse | null>(null);

  // Approval modal state
  const [showApproveModal, setShowApproveModal] = useState<boolean>(false);
  const [customShopCode, setCustomShopCode] = useState<string>('');
  const [approveNotes, setApproveNotes] = useState<string>('');
  const [approving, setApproving] = useState<boolean>(false);
  const [approvalResult, setApprovalResult] = useState<ApprovalResultDto | null>(null);

  // Rejection modal state
  const [showRejectModal, setShowRejectModal] = useState<boolean>(false);
  const [rejectReason, setRejectReason] = useState<string>('');
  const [rejecting, setRejecting] = useState<boolean>(false);

  // Notification banners
  const [banner, setBanner] = useState<{ type: 'success' | 'danger' | 'info'; message: string } | null>(null);

  const fetchRequests = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.listOnboardingRequests({
        status: statusFilter === 'ALL' ? undefined : statusFilter,
        search: search.trim() || undefined,
        page,
        size: 10,
      });
      setRequests(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
      if (onRequestsUpdated) {
        onRequestsUpdated();
      }
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to fetch onboarding requests.');
    } finally {
      setLoading(false);
    }
  }, [statusFilter, search, page, onRequestsUpdated]);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleApprove = async () => {
    if (!selectedRequest) return;
    setApproving(true);
    try {
      const result = await apiClient.approveOnboardingRequest(selectedRequest.id, {
        customShopCode: customShopCode.trim() || undefined,
        reviewerNotes: approveNotes.trim() || undefined,
      });
      setApprovalResult(result);
      setShowApproveModal(false);
      setCustomShopCode('');
      setApproveNotes('');
      // Refresh list
      await fetchRequests();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : 'Failed to approve onboarding request.',
      });
      setShowApproveModal(false);
    } finally {
      setApproving(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest) return;
    if (!rejectReason.trim()) {
      setBanner({ type: 'danger', message: 'A rejection reason is required before confirming.' });
      return;
    }
    setRejecting(true);
    try {
      await apiClient.rejectOnboardingRequest(selectedRequest.id, {
        reason: rejectReason.trim(),
      });
      setBanner({
        type: 'success',
        message: `Registration request ${selectedRequest.referenceCode} has been rejected.`,
      });
      setShowRejectModal(false);
      setRejectReason('');
      setSelectedRequest(null);
      await fetchRequests();
    } catch (err: unknown) {
      setBanner({
        type: 'danger',
        message: err instanceof Error ? err.message : 'Failed to reject onboarding request.',
      });
    } finally {
      setRejecting(false);
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

  // If viewing approval success screen
  if (approvalResult) {
    return (
      <div>
        <div
          className="card"
          style={{
            maxWidth: '680px',
            margin: '20px auto',
            textAlign: 'center',
            padding: '40px 32px',
          }}
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              backgroundColor: 'var(--success-bg)',
              color: 'var(--success)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 16px auto',
            }}
          >
            <Check size={28} />
          </div>

          <StatusBadge status="APPROVED" label="Provisioned Successfully" />

          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--ink)', margin: '14px 0 8px 0' }}>
            {selectedRequest?.shopName || 'Shop Tenant'} is Ready for Operations
          </h2>

          <p style={{ color: 'var(--muted)', fontSize: '14px', lineHeight: 1.5, marginBottom: '24px' }}>
            The tenant shop, primary owner user account, and commercial trial subscription were provisioned in a single atomic transaction.
          </p>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
              gap: '12px',
              backgroundColor: 'var(--canvas)',
              padding: '16px',
              borderRadius: 'var(--radius-sm)',
              border: '1px solid var(--border)',
              textAlign: 'left',
              marginBottom: '24px',
            }}
          >
            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Tenant Identifier
              </span>
              <div className="mono-text" style={{ fontSize: '14px', marginTop: '4px' }}>
                {approvalResult.tenantCode}
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Primary Owner Email
              </span>
              <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--ink)', marginTop: '4px' }}>
                {approvalResult.ownerEmail}
              </div>
            </div>

            <div>
              <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                Assigned Subscription
              </span>
              <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--teal)', marginTop: '4px' }}>
                {approvalResult.planCode || 'Standard Plan'}
              </div>
            </div>
          </div>

          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setApprovalResult(null);
              setSelectedRequest(null);
            }}
          >
            Return to Requests List
          </button>
        </div>
      </div>
    );
  }

  // If viewing detailed review of a single request
  if (selectedRequest) {
    return (
      <div>
        <PageHead
          eyebrow={`Applications / Reference ${selectedRequest.referenceCode}`}
          title={selectedRequest.shopName}
          description={`Submitted ${formatDate(selectedRequest.createdAt)} by ${selectedRequest.ownerFullName}.`}
          actions={
            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => setSelectedRequest(null)}
              >
                <ArrowLeft size={14} /> Back to List
              </button>

              {selectedRequest.status === 'PENDING' && (
                <>
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={() => setShowRejectModal(true)}
                  >
                    <X size={14} /> Reject Request
                  </button>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    onClick={() => setShowApproveModal(true)}
                  >
                    <Check size={14} /> Approve Request
                  </button>
                </>
              )}
            </div>
          }
        />

        {banner && (
          <Alert
            type={banner.type}
            message={banner.message}
            onDismiss={() => setBanner(null)}
          />
        )}

        <div className="review-layout">
          {/* Main Details */}
          <div>
            {/* Shop Profile Card */}
            <div className="card detail-card">
              <div className="detail-card-head">
                <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Building2 size={16} style={{ color: 'var(--teal)' }} /> Shop Details
                </h3>
                <StatusBadge status={selectedRequest.status} />
              </div>

              <div className="data-kv-grid">
                <div className="data-kv-label">Shop Name</div>
                <div className="data-kv-value">{selectedRequest.shopName}</div>

                <div className="data-kv-label">Reference Code</div>
                <div className="data-kv-value mono-text">{selectedRequest.referenceCode}</div>

                <div className="data-kv-label">Preferred Slug</div>
                <div className="data-kv-value mono-text">
                  {selectedRequest.preferredShopCode || 'Auto-generated'}
                </div>

                <div className="data-kv-label">Business Address</div>
                <div className="data-kv-value">
                  {selectedRequest.businessAddress || 'Not provided'}
                </div>

                <div className="data-kv-label">Operating Currency</div>
                <div className="data-kv-value">{selectedRequest.currency || 'ETB'}</div>
              </div>
            </div>

            {/* Owner & Contact Card */}
            <div className="card detail-card">
              <div className="detail-card-head">
                <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <Mail size={16} style={{ color: 'var(--teal)' }} /> Applicant Contact Information
                </h3>
              </div>

              <div className="data-kv-grid">
                <div className="data-kv-label">Owner Full Name</div>
                <div className="data-kv-value">{selectedRequest.ownerFullName}</div>

                <div className="data-kv-label">Email Address</div>
                <div className="data-kv-value">{selectedRequest.ownerEmail}</div>

                <div className="data-kv-label">Phone Number</div>
                <div className="data-kv-value">{selectedRequest.ownerPhone || 'Not provided'}</div>

                <div className="data-kv-label">Submission Date</div>
                <div className="data-kv-value">{formatDate(selectedRequest.createdAt)}</div>
              </div>
            </div>

            {/* Applicant Notes Card */}
            <div className="card detail-card">
              <div className="detail-card-head">
                <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <FileText size={16} style={{ color: 'var(--teal)' }} /> Applicant Notes &amp; Trade Details
                </h3>
              </div>
              <p style={{ margin: 0, fontSize: '13.5px', color: 'var(--text)', lineHeight: 1.6 }}>
                {selectedRequest.applicantNotes || 'No supplementary trade notes provided by applicant.'}
              </p>
            </div>
          </div>

          {/* Right Sidebar: Plan & Request History */}
          <div>
            <div className="card" style={{ marginBottom: '20px' }}>
              <div className="detail-card-head">
                <h3>Subscription Tier Selection</h3>
              </div>
              <div style={{ marginBottom: '14px' }}>
                <span style={{ fontSize: '12px', color: 'var(--muted)', textTransform: 'uppercase', fontWeight: 600 }}>
                  Requested Plan
                </span>
                <div style={{ fontSize: '18px', fontWeight: 800, color: 'var(--ink)', marginTop: '4px' }}>
                  {selectedRequest.requestedPlanCode || 'STARTER'}
                </div>
              </div>

              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: '8px',
                  padding: '12px',
                  backgroundColor: 'var(--teal-soft)',
                  borderRadius: 'var(--radius-sm)',
                  fontSize: '12.5px',
                  color: 'var(--teal-dark)',
                  lineHeight: 1.4,
                }}
              >
                <CheckCircle2 size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
                <span>Default 14-day commercial trial will be automatically provisioned upon approval.</span>
              </div>
            </div>

            {/* Review History */}
            {selectedRequest.reviewedAt && (
              <div className="card">
                <div className="detail-card-head">
                  <h3 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Calendar size={16} style={{ color: 'var(--muted)' }} /> Application Timeline
                  </h3>
                </div>
                <div className="data-kv-grid" style={{ gridTemplateColumns: '100px 1fr' }}>
                  <div className="data-kv-label">Reviewed On</div>
                  <div className="data-kv-value">{formatDate(selectedRequest.reviewedAt)}</div>

                  <div className="data-kv-label">Reviewer Notes</div>
                  <div className="data-kv-value">
                    {selectedRequest.reviewerNotes || 'No notes logged.'}
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Approval Modal */}
        {showApproveModal && (
          <Modal
            title={`Approve ${selectedRequest.shopName}?`}
            subtitle={`Reference: ${selectedRequest.referenceCode}`}
            onClose={() => setShowApproveModal(false)}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
                padding: '14px',
                backgroundColor: 'var(--success-bg)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid #b7e3cc',
                color: '#0e4c33',
                fontSize: '13px',
                marginBottom: '18px',
                lineHeight: 1.5,
              }}
            >
              <ShieldCheck size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong>Atomic Provisioning Guarantee:</strong>
                <br />
                The system will automatically create the shop organization, generate initial staff credentials for{' '}
                <strong>{selectedRequest.ownerEmail}</strong>, and provision a trial subscription in a single atomic database transaction.
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="custom-shop-code">
                Custom Tenant Slug (Optional)
              </label>
              <input
                id="custom-shop-code"
                type="text"
                className="form-input"
                placeholder={selectedRequest.preferredShopCode || 'e.g. al-madina-hardware'}
                value={customShopCode}
                onChange={(e) => setCustomShopCode(e.target.value)}
                disabled={approving}
              />
              <p className="form-help">Leave blank to use the applicant&apos;s preferred slug or auto-generate.</p>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reviewer-notes">
                Approval Notes (Internal Notes)
              </label>
              <textarea
                id="reviewer-notes"
                className="form-textarea"
                placeholder="Verified building materials license and contact details."
                value={approveNotes}
                onChange={(e) => setApproveNotes(e.target.value)}
                disabled={approving}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowApproveModal(false)}
                disabled={approving}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleApprove}
                disabled={approving}
              >
                {approving ? 'Provisioning Tenant...' : 'Confirm Approval & Provision'}
              </button>
            </div>
          </Modal>
        )}

        {/* Rejection Modal */}
        {showRejectModal && (
          <Modal
            title={`Reject ${selectedRequest.shopName}?`}
            subtitle={`Reference: ${selectedRequest.referenceCode}`}
            onClose={() => setShowRejectModal(false)}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '12px',
                padding: '14px',
                backgroundColor: 'var(--danger-bg)',
                borderRadius: 'var(--radius-sm)',
                border: '1px solid #f8c9c4',
                color: 'var(--danger)',
                fontSize: '13px',
                marginBottom: '18px',
                lineHeight: 1.5,
              }}
            >
              <AlertTriangle size={20} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong>Rejection Notice:</strong>
                <br />
                The application will be marked as REJECTED. The applicant will not receive tenant access, and this decision will be permanently recorded in the system.
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="rejection-reason">
                Rejection Reason <span className="required">*</span>
              </label>
              <textarea
                id="rejection-reason"
                className="form-textarea"
                placeholder="Explain why the registration request was rejected (e.g. unverified business license, duplicate shop)..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                required
                disabled={rejecting}
              />
              {!rejectReason.trim() && (
                <p className="form-error">A mandatory reason is required by backend business rules.</p>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setShowRejectModal(false)}
                disabled={rejecting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleReject}
                disabled={rejecting || !rejectReason.trim()}
              >
                {rejecting ? 'Rejecting Request...' : 'Confirm Rejection'}
              </button>
            </div>
          </Modal>
        )}
      </div>
    );
  }

  // Requests Table View
  const columns: Column<OnboardingDetailResponse>[] = [
    {
      key: 'referenceCode',
      header: 'Reference',
      width: '130px',
      render: (r) => <span className="mono-text">{r.referenceCode}</span>,
    },
    {
      key: 'shopName',
      header: 'Shop & Applicant',
      render: (r) => (
        <div>
          <div style={{ fontWeight: 700, color: 'var(--ink)' }}>{r.shopName}</div>
          <div style={{ fontSize: '12px', color: 'var(--muted)' }}>{r.ownerFullName}</div>
        </div>
      ),
    },
    {
      key: 'contact',
      header: 'Contact Info',
      render: (r) => (
        <div style={{ fontSize: '12.5px' }}>
          <div>{r.ownerEmail}</div>
          <div style={{ fontSize: '11.5px', color: 'var(--faint)' }}>{r.ownerPhone || '—'}</div>
        </div>
      ),
    },
    {
      key: 'requestedPlanCode',
      header: 'Requested Plan',
      render: (r) => (
        <span style={{ fontWeight: 600, color: 'var(--ink-2)', fontSize: '13px' }}>
          {r.requestedPlanCode || 'Standard'}
        </span>
      ),
    },
    {
      key: 'createdAt',
      header: 'Submitted',
      render: (r) => <span style={{ fontSize: '12.5px' }}>{formatDate(r.createdAt)}</span>,
    },
    {
      key: 'status',
      header: 'Status',
      render: (r) => <StatusBadge status={r.status} />,
    },
    {
      key: 'actions',
      header: '',
      width: '90px',
      render: (r) => (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => setSelectedRequest(r)}
        >
          Review
        </button>
      ),
    },
  ];

  return (
    <div>
      <PageHead
        eyebrow="Merchant Inbound Queue"
        title="Registration Requests"
        description="Review, verify, and approve public shop registration applications before tenant workspace provisioning."
      />

      {banner && (
        <Alert
          type={banner.type}
          message={banner.message}
          onDismiss={() => setBanner(null)}
        />
      )}

      {/* Filter Tabs */}
      <DataTable
        columns={columns}
        data={requests}
        loading={loading}
        error={error}
        onRetry={fetchRequests}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        searchPlaceholder="Search by shop name, owner, or reference..."
        searchValue={search}
        onSearchChange={(val) => {
          setSearch(val);
          setPage(0);
        }}
        filterComponent={
          <div className="filter-pill-tabs">
            {(['ALL', 'PENDING', 'APPROVED', 'REJECTED'] as const).map((st) => (
              <button
                key={st}
                type="button"
                className={`filter-pill-btn ${statusFilter === st ? 'active' : ''}`}
                onClick={() => {
                  setStatusFilter(st);
                  setPage(0);
                }}
              >
                {st === 'ALL' ? 'All Requests' : st.charAt(0) + st.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        }
        emptyTitle="No registration requests found"
        emptyMessage="There are currently no onboarding applications matching this search or status filter."
        rowKey={(r) => r.id}
      />
    </div>
  );
};
