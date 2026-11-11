import React, { useState, useEffect } from 'react';
import {
  Search,
  Eye,
  Printer,
  Ban,
  RefreshCw,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { SaleReceiptModal } from '../components/SaleReceiptModal';
import { shopApi } from '../api/client';
import {
  SaleResponseDto,
  SaleReceiptResponseDto,
} from '../api/types';
import { useAuth } from '../context/AuthContext';

export const SalesHistoryPage: React.FC = () => {
  const { canVoidSales } = useAuth();

  const [sales, setSales] = useState<SaleResponseDto[]>([]);
  const [pageInfo, setPageInfo] = useState({ page: 0, size: 20, totalElements: 0, totalPages: 0 });
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [statusFilter, setStatusFilter] = useState<string>('');

  // Selected sale drawer/modal
  const [selectedSale, setSelectedSale] = useState<SaleResponseDto | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(false);

  // Void modal
  const [isVoidModalOpen, setIsVoidModalOpen] = useState<boolean>(false);
  const [voidReason, setVoidReason] = useState<string>('');
  const [isVoiding, setIsVoiding] = useState<boolean>(false);
  const [voidError, setVoidError] = useState<string | null>(null);

  // Printable receipt
  const [receiptData, setReceiptData] = useState<SaleReceiptResponseDto | null>(null);
  const [isReceiptOpen, setIsReceiptOpen] = useState<boolean>(false);

  const fetchSales = async (page = 0) => {
    setIsLoading(true);
    try {
      const res = await shopApi.listSales({
        page,
        size: pageInfo.size,
        status: statusFilter || undefined,
        search: searchQuery.trim() || undefined,
      });
      if (res.data) {
        setSales(res.data.content);
        setPageInfo({
          page: res.data.pageNumber,
          size: res.data.pageSize,
          totalElements: res.data.totalElements,
          totalPages: res.data.totalPages,
        });
      }
    } catch (err: any) {
      console.error('Failed to load sales history:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSales(0);
  }, [statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchSales(0);
  };

  const handleOpenReceipt = async (saleId: string) => {
    try {
      const res = await shopApi.getSaleReceipt(saleId);
      setReceiptData(res.data);
      setIsReceiptOpen(true);
    } catch (err: any) {
      alert('Failed to load receipt: ' + (err.message || err));
    }
  };

  const handleOpenVoidModal = (sale: SaleResponseDto) => {
    setSelectedSale(sale);
    setVoidReason('');
    setVoidError(null);
    setIsVoidModalOpen(true);
  };

  const handleConfirmVoid = async () => {
    if (!selectedSale) return;
    if (!voidReason.trim()) {
      setVoidError('Please select or specify a reason for voiding this sale.');
      return;
    }

    setIsVoiding(true);
    setVoidError(null);
    try {
      await shopApi.voidSale(selectedSale.id, { reason: voidReason.trim() });
      setIsVoidModalOpen(false);
      setIsDetailOpen(false);
      await fetchSales(pageInfo.page);
    } catch (err: any) {
      setVoidError(err.message || 'Failed to void sale. Operation rejected.');
    } finally {
      setIsVoiding(false);
    }
  };

  const formatCurrency = (val: number) => {
    return Number(val || 0).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  return (
    <div>
      <PageHead
        eyebrow="Sales Ledger"
        title="Sales History & Records"
        subtitle="Itemized customer sales, invoice audit records, operational receipts, and voids"
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchSales(pageInfo.page)}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>Refresh</span>
          </button>
        }
      />

      <div className="card">
        {/* Filter Bar */}
        <form onSubmit={handleSearchSubmit} className="filter-bar">
          <div className="search-input-wrap">
            <Search size={16} />
            <input
              type="text"
              className="input-search"
              placeholder="Search by invoice number or customer name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <select
            className="select-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="VOIDED">Voided</option>
          </select>

          <button type="submit" className="btn btn-secondary btn-sm">
            Filter
          </button>
        </form>

        {/* Sales Table */}
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Invoice #</th>
                <th>Sold At</th>
                <th>Customer</th>
                <th>Items</th>
                <th>Cashier</th>
                <th>Total</th>
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    Loading sales records...
                  </td>
                </tr>
              ) : sales.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    No sales records found.
                  </td>
                </tr>
              ) : (
                sales.map((sale) => (
                  <tr key={sale.id}>
                    <td style={{ fontWeight: 700 }}>{sale.invoiceNumber}</td>
                    <td>{new Date(sale.soldAt).toLocaleString()}</td>
                    <td>{sale.customerName || 'Walk-in'}</td>
                    <td>{sale.items?.length || 0} line(s)</td>
                    <td>{sale.cashierName || 'Staff'}</td>
                    <td style={{ fontWeight: 800 }}>{formatCurrency(sale.totalAmount)}</td>
                    <td>
                      <StatusBadge status={sale.status} variant="sale" />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '4px' }}>
                        <button
                          type="button"
                          className="btn-icon"
                          title="View Details"
                          onClick={() => {
                            setSelectedSale(sale);
                            setIsDetailOpen(true);
                          }}
                        >
                          <Eye size={16} />
                        </button>
                        <button
                          type="button"
                          className="btn-icon"
                          title="Print Receipt"
                          onClick={() => handleOpenReceipt(sale.id)}
                        >
                          <Printer size={16} color="var(--teal)" />
                        </button>
                        {canVoidSales && sale.status === 'COMPLETED' && (
                          <button
                            type="button"
                            className="btn-icon"
                            title="Void Sale"
                            onClick={() => handleOpenVoidModal(sale)}
                          >
                            <Ban size={16} color="var(--danger)" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {pageInfo.totalPages > 1 && (
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
              Showing {sales.length} of {pageInfo.totalElements} records (Page {pageInfo.page + 1} of {pageInfo.totalPages})
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={pageInfo.page === 0}
                onClick={() => fetchSales(pageInfo.page - 1)}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={pageInfo.page >= pageInfo.totalPages - 1}
                onClick={() => fetchSales(pageInfo.page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Sale Detail Modal */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={`Sale Detail — ${selectedSale?.invoiceNumber || ''}`}
        size="large"
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            {selectedSale && (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={() => handleOpenReceipt(selectedSale.id)}
              >
                <Printer size={14} />
                <span>Print Receipt</span>
              </button>
            )}
            {canVoidSales && selectedSale?.status === 'COMPLETED' && (
              <button
                type="button"
                className="btn btn-danger btn-sm"
                onClick={() => {
                  setIsDetailOpen(false);
                  handleOpenVoidModal(selectedSale);
                }}
              >
                <Ban size={14} />
                <span>Void Sale</span>
              </button>
            )}
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsDetailOpen(false)}
            >
              Close
            </button>
          </div>
        }
      >
        {selectedSale && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Sold At</span>
                <div style={{ fontWeight: 600 }}>{new Date(selectedSale.soldAt).toLocaleString()}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Cashier</span>
                <div style={{ fontWeight: 600 }}>{selectedSale.cashierName || 'Staff'}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Customer</span>
                <div style={{ fontWeight: 600 }}>{selectedSale.customerName || 'Walk-in'}</div>
              </div>
            </div>

            {selectedSale.status === 'VOIDED' && (
              <div
                style={{
                  background: 'var(--danger-bg)',
                  border: '1px solid rgba(184, 58, 58, 0.2)',
                  borderRadius: '6px',
                  padding: '12px',
                  color: 'var(--danger)',
                  fontSize: '13px',
                }}
              >
                <strong>VOIDED SALE:</strong> {selectedSale.voidReason || 'No reason provided'} (on{' '}
                {selectedSale.voidedAt ? new Date(selectedSale.voidedAt).toLocaleString() : 'N/A'})
              </div>
            )}

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Unit</th>
                    <th style={{ textAlign: 'center' }}>Qty</th>
                    <th style={{ textAlign: 'right' }}>Unit Price</th>
                    <th style={{ textAlign: 'right' }}>Discount</th>
                    <th style={{ textAlign: 'right' }}>Line Total</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedSale.items?.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div style={{ fontWeight: 700 }}>{item.productName}</div>
                        <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                      </td>
                      <td>{item.unitCode}</td>
                      <td style={{ textAlign: 'center' }}>{item.quantity}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitPrice)}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(item.discountAmount)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(item.lineTotal)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <div style={{ width: '240px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--muted)' }}>
                  <span>Subtotal:</span>
                  <span>{formatCurrency(selectedSale.subtotal)}</span>
                </div>
                {Number(selectedSale.discountAmount) > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--danger)' }}>
                    <span>Discount:</span>
                    <span>-{formatCurrency(selectedSale.discountAmount)}</span>
                  </div>
                )}
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: '16px',
                    fontWeight: 800,
                    color: 'var(--ink)',
                    borderTop: '1px solid var(--border)',
                    paddingTop: '6px',
                  }}
                >
                  <span>Grand Total:</span>
                  <span>{formatCurrency(selectedSale.totalAmount)}</span>
                </div>
              </div>
            </div>
          </div>
        )}
      </Modal>

      {/* Void Confirmation Modal */}
      <Modal
        isOpen={isVoidModalOpen}
        onClose={() => setIsVoidModalOpen(false)}
        title="Void Sale & Restore Inventory"
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsVoidModalOpen(false)}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={handleConfirmVoid}
              disabled={isVoiding}
            >
              {isVoiding ? 'Voiding...' : 'Confirm Void'}
            </button>
          </div>
        }
      >
        {voidError && <Alert type="error" message={voidError} onDismiss={() => setVoidError(null)} />}
        <p style={{ fontSize: '13.5px', color: 'var(--text)', margin: 0 }}>
          You are voiding invoice <strong>{selectedSale?.invoiceNumber}</strong> for total amount{' '}
          <strong>{formatCurrency(selectedSale?.totalAmount || 0)}</strong>.
        </p>
        <p style={{ fontSize: '12.5px', color: 'var(--muted)', margin: 0 }}>
          This operation is irreversible: items sold will be atomically returned to on-hand inventory stock and a
          compensating reversal ledger entry will be recorded.
        </p>

        <div className="form-group" style={{ marginTop: '12px' }}>
          <label className="form-label">Reason for Void *</label>
          <select
            className="form-select"
            value={voidReason}
            onChange={(e) => setVoidReason(e.target.value)}
            required
          >
            <option value="">-- Select Reason Code --</option>
            <option value="CUSTOMER_CANCEL">Customer Cancelled Transaction</option>
            <option value="DEFECTIVE_RETURN">Defective Materials / Immediate Return</option>
            <option value="MISTAKE">Cashier Keying Mistake</option>
            <option value="DUPLICATE">Duplicate Submission</option>
            <option value="OTHER">Other Operational Reason</option>
          </select>
        </div>
      </Modal>

      {/* Printable Receipt Modal */}
      <SaleReceiptModal
        isOpen={isReceiptOpen}
        onClose={() => setIsReceiptOpen(false)}
        receipt={receiptData}
      />
    </div>
  );
};
