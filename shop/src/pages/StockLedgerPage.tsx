import React, { useState, useEffect } from 'react';
import {
  Search,
  RefreshCw,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { shopApi } from '../api/client';
import {
  StockMovementResponse,
  MovementType,
} from '../api/types';
import { useAuth } from '../context/AuthContext';

export const StockLedgerPage: React.FC = () => {
  const { role } = useAuth();
  const isCashier = role === 'CASHIER';

  const [movements, setMovements] = useState<StockMovementResponse[]>([]);
  const [movementType, setMovementType] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  const fetchMovements = async (p = 0) => {
    setIsLoading(true);
    try {
      const res = await shopApi.listStockMovements({
        page: p,
        size: 25,
        movementType: (movementType as MovementType) || undefined,
        search: searchQuery.trim() || undefined,
      });
      if (res.data) {
        setMovements(res.data.content);
        setPage(res.data.pageNumber);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
      }
    } catch (err: any) {
      console.error('Failed to load stock movement ledger:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMovements(0);
  }, [movementType]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchMovements(0);
  };

  const formatCurrency = (val?: number) => {
    if (val == null) return null;
    return `Br ${Number(val).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  const getMovementLabel = (type: string) => {
    switch (type) {
      case 'RECEIPT':
      case 'PURCHASE_RECEIPT':
        return 'Buying';
      case 'SALE':
        return 'Selling';
      case 'ADJUSTMENT':
        return 'Adjustment';
      case 'RETURN':
        return 'Return';
      default:
        return type;
    }
  };

  return (
    <div>
      <PageHead
        eyebrow="Immutable Audit Trail"
        title="Stock History"
        subtitle="Complete chronological transaction log of all buying receipts, selling deductions, and stock adjustments"
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchMovements(page)}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>
        }
      />

      <div className="card">
        {/* Filter Bar */}
        <form onSubmit={handleSearch} className="filter-bar">
          <div className="search-input-wrap">
            <Search size={16} />
            <input
              type="text"
              className="input-search"
              placeholder="Search by notes, reference code..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <select
            className="select-filter"
            value={movementType}
            onChange={(e) => setMovementType(e.target.value)}
          >
            <option value="">All Movements</option>
            <option value="RECEIPT">Buying (Receipts)</option>
            <option value="SALE">Selling (Sales)</option>
            <option value="ADJUSTMENT">Adjustment</option>
            <option value="RETURN">Sales Returns / Voids</option>
          </select>

          <button type="submit" className="btn btn-secondary btn-sm">
            Filter
          </button>
        </form>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Product</th>
                <th>Movement</th>
                <th>Change</th>
                <th>Balance After</th>
                {!isCashier && <th style={{ textAlign: 'right' }}>Cost at Time of Transaction</th>}
                <th>Reference</th>
                <th>Actor</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={isCashier ? 7 : 8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    Loading stock history ledger...
                  </td>
                </tr>
              ) : movements.length === 0 ? (
                <tr>
                  <td colSpan={isCashier ? 7 : 8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    No stock movements recorded yet.
                  </td>
                </tr>
              ) : (
                movements.map((m) => {
                  const isPositive = Number(m.quantityChange) > 0;
                  const label = getMovementLabel(m.movementType);
                  return (
                    <tr key={m.id}>
                      <td style={{ fontSize: '12px', color: 'var(--muted)' }}>
                        {new Date(m.createdAt).toLocaleString()}
                      </td>
                      <td>
                        <div style={{ fontWeight: 700 }}>{m.productName}</div>
                        <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {m.productSku}</div>
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            m.movementType === 'RECEIPT' || m.movementType === 'RETURN'
                              ? 'badge-success'
                              : m.movementType === 'SALE'
                              ? 'badge-primary'
                              : 'badge-warning'
                          }`}
                        >
                          {label}
                        </span>
                      </td>
                      <td
                        style={{
                          fontWeight: 700,
                          color: isPositive ? 'var(--success)' : 'var(--danger)',
                        }}
                      >
                        {isPositive ? `+${m.quantityChange}` : m.quantityChange} {m.baseUnitCode}
                      </td>
                      <td>
                        <strong>{m.balanceAfter}</strong> {m.baseUnitCode}
                      </td>
                      {!isCashier && (
                        <td style={{ textAlign: 'right', color: 'var(--muted)' }}>
                          {(() => {
                            const cost = m.unitCostSnapshot ?? m.unitCost ?? m.costAtTimeOfTransaction;
                            const formatted = formatCurrency(cost);
                            return formatted ? (
                              <span style={{ fontWeight: 600, color: 'var(--ink)' }}>{formatted}</span>
                            ) : (
                              <span style={{ color: 'var(--muted)', fontStyle: 'italic', fontSize: '11.5px' }}>Not available</span>
                            );
                          })()}
                        </td>
                      )}
                      <td>
                        {m.referenceType ? (
                          <div style={{ fontSize: '12px' }}>
                            <span style={{ fontWeight: 600 }}>{m.referenceType}</span>
                            {m.notes && <div style={{ fontSize: '11px', color: 'var(--muted)' }}>{m.notes}</div>}
                          </div>
                        ) : (
                          '—'
                        )}
                      </td>
                      <td style={{ fontSize: '12px', color: 'var(--muted)' }}>
                        {m.actorEmail || 'System'}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              marginTop: '16px',
              fontSize: '13px',
              color: 'var(--muted)',
            }}
          >
            <span>
              Showing {movements.length} of {totalElements} movements (Page {page + 1} of {totalPages})
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page === 0}
                onClick={() => fetchMovements(page - 1)}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => fetchMovements(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
