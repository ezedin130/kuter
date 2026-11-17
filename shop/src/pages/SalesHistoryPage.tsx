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
import { useLanguage } from '../i18n/LanguageContext';

export const SalesHistoryPage: React.FC = () => {
  const { canVoidSales } = useAuth();
  const { t, formatCurrency, formatDate, language } = useLanguage();

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
      setVoidError(t.salesHistory.voidReasonRequired || 'Please select or specify a reason for voiding this sale.');
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
      setVoidError(err.message || (language === 'am' ? 'ሽያጩን መሰረዝ አልተቻለም።' : 'Failed to void sale. Operation rejected.'));
    } finally {
      setIsVoiding(false);
    }
  };

  return (
    <div>
      <PageHead
        eyebrow={language === 'am' ? 'የሽያጭ መዝገብ' : 'Sales Ledger'}
        title={t.salesHistory.title}
        subtitle={t.salesHistory.subtitle}
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchSales(pageInfo.page)}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>{t.common.refresh}</span>
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
              placeholder={t.salesHistory.searchPlaceholder}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <select
            className="select-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">{language === 'am' ? 'ሁሉም ሁኔታዎች' : 'All Statuses'}</option>
            <option value="COMPLETED">{t.status.COMPLETED}</option>
            <option value="VOIDED">{t.status.VOIDED}</option>
          </select>

          <button type="submit" className="btn btn-secondary btn-sm">
            {t.common.filter}
          </button>
        </form>

        {/* Sales Table */}
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{t.salesHistory.saleNumber}</th>
                <th>{t.salesHistory.date}</th>
                <th>{t.dashboard.customer}</th>
                <th>{language === 'am' ? 'ዕቃዎች' : 'Items'}</th>
                <th>{t.salesHistory.cashier}</th>
                <th>{t.salesHistory.total}</th>
                <th>{t.salesHistory.status}</th>
                <th style={{ textAlign: 'right' }}>{t.common.actions}</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    {t.common.loading}
                  </td>
                </tr>
              ) : sales.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    {t.common.noRecords}
                  </td>
                </tr>
              ) : (
                sales.map((sale) => (
                  <tr key={sale.id}>
                    <td style={{ fontWeight: 700 }}>{sale.invoiceNumber}</td>
                    <td>{formatDate(sale.soldAt)}</td>
                    <td>{sale.customerName || (language === 'am' ? 'ደንበኛ' : 'Walk-in')}</td>
                    <td>{sale.items?.length || 0} {language === 'am' ? 'መስመር' : 'line(s)'}</td>
                    <td>{sale.cashierName || (language === 'am' ? 'ሰራተኛ' : 'Staff')}</td>
                    <td style={{ fontWeight: 800 }}>{formatCurrency(sale.totalAmount)}</td>
                    <td>
                      <StatusBadge status={sale.status} variant="sale" />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '4px' }}>
                        <button
                          type="button"
                          className="btn-icon"
                          title={t.common.details}
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
                          title={t.salesHistory.viewReceipt}
                          onClick={() => handleOpenReceipt(sale.id)}
                        >
                          <Printer size={16} color="var(--teal)" />
                        </button>
                        {canVoidSales && sale.status === 'COMPLETED' && (
                          <button
                            type="button"
                            className="btn-icon"
                            title={t.salesHistory.voidSale}
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
              {language === 'am'
                ? `ከ ${pageInfo.totalElements} መዝገቦች ውስጥ ${sales.length} በማሳየት ላይ (ገጽ ${pageInfo.page + 1} ከ ${pageInfo.totalPages})`
                : `Showing ${sales.length} of ${pageInfo.totalElements} records (Page ${pageInfo.page + 1} of ${pageInfo.totalPages})`}
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={pageInfo.page === 0}
                onClick={() => fetchSales(pageInfo.page - 1)}
              >
                {t.common.previous}
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={pageInfo.page >= pageInfo.totalPages - 1}
                onClick={() => fetchSales(pageInfo.page + 1)}
              >
                {t.common.next}
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Sale Detail Modal */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={`${language === 'am' ? 'የሽያጭ ዝርዝር' : 'Sale Detail'} — ${selectedSale?.invoiceNumber || ''}`}
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
                <span>{t.salesHistory.viewReceipt}</span>
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
                <span>{t.salesHistory.voidSale}</span>
              </button>
            )}
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsDetailOpen(false)}
            >
              {t.common.close}
            </button>
          </div>
        }
      >
        {selectedSale && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>
                  {t.salesHistory.date}
                </span>
                <div style={{ fontWeight: 600 }}>{formatDate(selectedSale.soldAt)}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>
                  {t.salesHistory.cashier}
                </span>
                <div style={{ fontWeight: 600 }}>{selectedSale.cashierName || (language === 'am' ? 'ሰራተኛ' : 'Staff')}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>
                  {t.dashboard.customer}
                </span>
                <div style={{ fontWeight: 600 }}>{selectedSale.customerName || (language === 'am' ? 'ደንበኛ' : 'Walk-in')}</div>
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
                <strong>{language === 'am' ? 'የተሰረዘ ሽያጭ:' : 'VOIDED SALE:'}</strong> {selectedSale.voidReason || (language === 'am' ? 'ምክንያት አልተገለጸም' : 'No reason provided')} ({language === 'am' ? 'በ ' : 'on '}
                {selectedSale.voidedAt ? formatDate(selectedSale.voidedAt) : 'N/A'})
              </div>
            )}

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>{language === 'am' ? 'ምርት' : 'Product'}</th>
                    <th>{language === 'am' ? 'መለኪያ' : 'Unit'}</th>
                    <th style={{ textAlign: 'center' }}>{t.common.quantity}</th>
                    <th style={{ textAlign: 'right' }}>{t.common.unitPrice}</th>
                    <th style={{ textAlign: 'right' }}>{t.pos.orderDiscount}</th>
                    <th style={{ textAlign: 'right' }}>{language === 'am' ? 'ጠቅላላ ዋጋ' : 'Line Total'}</th>
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
                  <span>{t.pos.subtotal}:</span>
                  <span>{formatCurrency(selectedSale.subtotal)}</span>
                </div>
                {Number(selectedSale.discountAmount) > 0 && (
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--danger)' }}>
                    <span>{t.pos.orderDiscount}:</span>
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
                  <span>{t.pos.grandTotal}:</span>
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
        title={t.salesHistory.voidConfirmTitle}
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsVoidModalOpen(false)}
            >
              {t.common.cancel}
            </button>
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={handleConfirmVoid}
              disabled={isVoiding}
            >
              {isVoiding ? t.salesHistory.voiding : t.common.confirm}
            </button>
          </div>
        }
      >
        {voidError && <Alert type="error" message={voidError} onDismiss={() => setVoidError(null)} />}
        <p style={{ fontSize: '13.5px', color: 'var(--text)', margin: 0 }}>
          {language === 'am' ? (
            <>
              የደረሰኝ ቁጥር <strong>{selectedSale?.invoiceNumber}</strong> ጠቅላላ ዋጋ{' '}
              <strong>{formatCurrency(selectedSale?.totalAmount || 0)}</strong> ሽያጭ ሊሰርዙ ነው።
            </>
          ) : (
            <>
              You are voiding invoice <strong>{selectedSale?.invoiceNumber}</strong> for total amount{' '}
              <strong>{formatCurrency(selectedSale?.totalAmount || 0)}</strong>.
            </>
          )}
        </p>
        <p style={{ fontSize: '12.5px', color: 'var(--muted)', margin: '8px 0 0' }}>
          {t.salesHistory.voidConfirmMsg}
        </p>

        <div className="form-group" style={{ marginTop: '12px' }}>
          <label className="form-label">{t.salesHistory.voidReason} *</label>
          <select
            className="form-select"
            value={voidReason}
            onChange={(e) => setVoidReason(e.target.value)}
            required
          >
            <option value="">{language === 'am' ? '-- የምክንያት ኮድ ይምረጡ --' : '-- Select Reason Code --'}</option>
            <option value="CUSTOMER_CANCEL">{language === 'am' ? 'ደንበኛው ግብይቱን ሰርዟል' : 'Customer Cancelled Transaction'}</option>
            <option value="DEFECTIVE_RETURN">{language === 'am' ? 'ጉድለት ያለበት እቃ / ወዲያው የተመለሰ' : 'Defective Materials / Immediate Return'}</option>
            <option value="MISTAKE">{language === 'am' ? 'የገንዘብ ተቀባይ ስህተት' : 'Cashier Keying Mistake'}</option>
            <option value="DUPLICATE">{language === 'am' ? 'የተደገመ ግብይት' : 'Duplicate Submission'}</option>
            <option value="OTHER">{language === 'am' ? 'ሌላ ተግባራዊ ምክንያት' : 'Other Operational Reason'}</option>
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
