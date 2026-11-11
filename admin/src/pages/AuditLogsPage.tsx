import { useState, useEffect, useCallback } from 'react';
import {
  Filter,
  Eye,
  RefreshCw,
} from 'lucide-react';
import { apiClient } from '../api/client';
import { AuditLogResponseDto } from '../api/types';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { DataTable, Column } from '../components/DataTable';

export const AuditLogsPage: React.FC = () => {
  const [logs, setLogs] = useState<AuditLogResponseDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [outcomeFilter, setOutcomeFilter] = useState<string>('ALL');
  const [actorEmail, setActorEmail] = useState<string>('');
  const [actionInput, setActionInput] = useState<string>('');
  const [entityTypeInput, setEntityTypeInput] = useState<string>('');
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Inspection modal
  const [inspectedLog, setInspectedLog] = useState<AuditLogResponseDto | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.getAdminAuditLogs({
        outcome: outcomeFilter === 'ALL' ? undefined : outcomeFilter,
        actorEmail: actorEmail.trim() || undefined,
        action: actionInput.trim() || undefined,
        entityType: entityTypeInput.trim() || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 15,
      });
      setLogs(res.content || []);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements || 0);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Failed to query platform audit logs.');
    } finally {
      setLoading(false);
    }
  }, [outcomeFilter, actorEmail, actionInput, entityTypeInput, startDate, endDate, page]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleResetFilters = () => {
    setOutcomeFilter('ALL');
    setActorEmail('');
    setActionInput('');
    setEntityTypeInput('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const formatDate = (isoStr?: string) => {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
    } catch {
      return isoStr;
    }
  };

  const columns: Column<AuditLogResponseDto>[] = [
    {
      key: 'createdAt',
      header: 'Timestamp (UTC)',
      width: '180px',
      render: (l) => <span style={{ fontSize: '12px' }}>{formatDate(l.createdAt)}</span>,
    },
    {
      key: 'actorEmail',
      header: 'Actor',
      render: (l) => (
        <span style={{ fontWeight: 600, color: 'var(--ink)' }}>
          {l.actorEmail || 'System Process'}
        </span>
      ),
    },
    {
      key: 'action',
      header: 'Action',
      render: (l) => (
        <span className="mono-text" style={{ fontSize: '12px', color: 'var(--teal-dark)' }}>
          {l.action}
        </span>
      ),
    },
    {
      key: 'entity',
      header: 'Entity Target',
      render: (l) => (
        <div style={{ fontSize: '12px' }}>
          <div>{l.entityType || 'General'}</div>
          {l.entityId && (
            <div className="mono-text" style={{ fontSize: '10.5px', color: 'var(--muted)' }}>
              {l.entityId.length > 12 ? `${l.entityId.slice(0, 12)}...` : l.entityId}
            </div>
          )}
        </div>
      ),
    },
    {
      key: 'outcome',
      header: 'Outcome',
      width: '110px',
      render: (l) => <StatusBadge status={l.outcome || 'SUCCESS'} size="sm" />,
    },
    {
      key: 'actions',
      header: '',
      width: '90px',
      render: (l) => (
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => setInspectedLog(l)}
          title="Inspect full audit record"
        >
          <Eye size={13} /> View
        </button>
      ),
    },
  ];

  const hasActiveFilters =
    outcomeFilter !== 'ALL' ||
    !!actorEmail ||
    !!actionInput ||
    !!entityTypeInput ||
    !!startDate ||
    !!endDate;

  return (
    <div>
      <PageHead
        eyebrow="Security &amp; Compliance"
        title="Platform Audit Logs"
        description="Tamper-evident chronological audit trail capturing administrative mutations, tenant approvals, subscription transitions, and security events."
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchLogs}
            disabled={loading}
          >
            <RefreshCw size={14} className={loading ? 'spin' : ''} />
            <span>Refresh Logs</span>
          </button>
        }
      />

      {/* Advanced Filter Panel */}
      <div className="card" style={{ padding: '16px 20px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px', flexWrap: 'wrap', gap: '8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13.5px', fontWeight: 700, color: 'var(--ink)' }}>
            <Filter size={16} style={{ color: 'var(--teal)' }} />
            <span>Audit Query Filters</span>
          </div>

          <div className="filter-pill-tabs">
            {(['ALL', 'SUCCESS', 'FAILURE', 'DENIED'] as const).map((oc) => (
              <button
                key={oc}
                type="button"
                className={`filter-pill-btn ${outcomeFilter === oc ? 'active' : ''}`}
                onClick={() => {
                  setOutcomeFilter(oc);
                  setPage(0);
                }}
              >
                {oc === 'ALL' ? 'All Outcomes' : oc}
              </button>
            ))}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
          <div>
            <label className="form-label" style={{ fontSize: '11.5px' }}>Actor Email</label>
            <input
              type="text"
              className="form-input"
              style={{ minHeight: '34px', fontSize: '13px' }}
              placeholder="Filter by actor email..."
              value={actorEmail}
              onChange={(e) => {
                setActorEmail(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '11.5px' }}>Action Name</label>
            <input
              type="text"
              className="form-input"
              style={{ minHeight: '34px', fontSize: '13px' }}
              placeholder="e.g. TENANT_APPROVED, LOGIN"
              value={actionInput}
              onChange={(e) => {
                setActionInput(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '11.5px' }}>Entity Type</label>
            <input
              type="text"
              className="form-input"
              style={{ minHeight: '34px', fontSize: '13px' }}
              placeholder="e.g. Tenant, OnboardingRequest"
              value={entityTypeInput}
              onChange={(e) => {
                setEntityTypeInput(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '11.5px' }}>Start Date (UTC)</label>
            <input
              type="date"
              className="form-input"
              style={{ minHeight: '34px', fontSize: '13px' }}
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value);
                setPage(0);
              }}
            />
          </div>

          <div>
            <label className="form-label" style={{ fontSize: '11.5px' }}>End Date (UTC)</label>
            <input
              type="date"
              className="form-input"
              style={{ minHeight: '34px', fontSize: '13px' }}
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        {hasActiveFilters && (
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '12px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={handleResetFilters}
            >
              Reset All Filters
            </button>
          </div>
        )}
      </div>

      {/* Main Table */}
      <DataTable
        columns={columns}
        data={logs}
        loading={loading}
        error={error}
        onRetry={fetchLogs}
        page={page}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={setPage}
        emptyTitle="No audit records match the query"
        emptyMessage="Try adjusting the outcome or date range filters to view historical administrative events."
        rowKey={(l) => l.id}
      />

      {/* Inspection Modal */}
      {inspectedLog && (
        <Modal
          title={`Audit Event: ${inspectedLog.action}`}
          subtitle={`Event ID: ${inspectedLog.id}`}
          onClose={() => setInspectedLog(null)}
          maxWidth="640px"
        >
          <div className="card detail-card" style={{ padding: '16px' }}>
            <div className="data-kv-grid">
              <div className="data-kv-label">Event Timestamp</div>
              <div className="data-kv-value">{formatDate(inspectedLog.createdAt)}</div>

              <div className="data-kv-label">Action Outcome</div>
              <div className="data-kv-value">
                <StatusBadge status={inspectedLog.outcome || 'SUCCESS'} />
              </div>

              <div className="data-kv-label">Actor Email</div>
              <div className="data-kv-value">{inspectedLog.actorEmail || 'System / Batch'}</div>

              {inspectedLog.actorId && (
                <>
                  <div className="data-kv-label">Actor User ID</div>
                  <div className="data-kv-value mono-text">{inspectedLog.actorId}</div>
                </>
              )}

              <div className="data-kv-label">Entity Target</div>
              <div className="data-kv-value">
                {inspectedLog.entityType || 'General'}
                {inspectedLog.entityId && (
                  <span className="mono-text" style={{ marginLeft: '8px', color: 'var(--muted)' }}>
                    (ID: {inspectedLog.entityId})
                  </span>
                )}
              </div>

              {inspectedLog.tenantId && (
                <>
                  <div className="data-kv-label">Tenant ID</div>
                  <div className="data-kv-value mono-text">{inspectedLog.tenantId}</div>
                </>
              )}

              {inspectedLog.ipAddress && (
                <>
                  <div className="data-kv-label">Source IP</div>
                  <div className="data-kv-value mono-text">{inspectedLog.ipAddress}</div>
                </>
              )}

              {inspectedLog.correlationId && (
                <>
                  <div className="data-kv-label">Correlation ID</div>
                  <div className="data-kv-value mono-text">{inspectedLog.correlationId}</div>
                </>
              )}
            </div>
          </div>

          {inspectedLog.details && (
            <div className="form-group" style={{ marginTop: '16px' }}>
              <label className="form-label">Event Details Payload</label>
              <pre
                style={{
                  backgroundColor: 'var(--canvas)',
                  border: '1px solid var(--border)',
                  borderRadius: 'var(--radius-sm)',
                  padding: '12px',
                  fontSize: '12px',
                  overflowX: 'auto',
                  maxHeight: '200px',
                  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word',
                  margin: 0,
                }}
              >
                {inspectedLog.details}
              </pre>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setInspectedLog(null)}
            >
              Close
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
};
