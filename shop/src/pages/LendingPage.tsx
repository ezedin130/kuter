import React, { useState, useEffect } from 'react';
import {
  RefreshCw,
  Eye,
  RotateCcw,
  XCircle,
  Layers,
  ArrowDownRight,
  ArrowUpRight,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  StockLoanResponse,
  LendingObligationsSummary,
  ProductSummary,
  UnitDefinition,
  CreateBorrowingRequestDto,
  CreateLoanRequestDto,
  RecordLoanReturnDto,
} from '../api/types';
import { useAuth } from '../context/AuthContext';

type LendingTab = 'borrowings' | 'loans' | 'obligations';

export const LendingPage: React.FC = () => {
  const { canManageLending } = useAuth();

  const [activeTab, setActiveTab] = useState<LendingTab>('borrowings');
  const [borrowings, setBorrowings] = useState<StockLoanResponse[]>([]);
  const [loans, setLoans] = useState<StockLoanResponse[]>([]);
  const [obligations, setObligations] = useState<LendingObligationsSummary | null>(null);
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [units, setUnits] = useState<UnitDefinition[]>([]);

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Status Filter for Lists
  const [statusFilter, setStatusFilter] = useState<string>('');

  // Modals
  const [isBorrowModalOpen, setIsBorrowModalOpen] = useState<boolean>(false);
  const [isLoanModalOpen, setIsLoanModalOpen] = useState<boolean>(false);
  const [selectedLoanForReturn, setSelectedLoanForReturn] = useState<StockLoanResponse | null>(null);
  const [selectedLoanForCancel, setSelectedLoanForCancel] = useState<StockLoanResponse | null>(null);
  const [selectedLoanForDetail, setSelectedLoanForDetail] = useState<StockLoanResponse | null>(null);

  // Form State: Borrowing
  const [borrowCounterparty, setBorrowCounterparty] = useState<string>('');
  const [borrowContact, setBorrowContact] = useState<string>('');
  const [borrowPhone, setBorrowPhone] = useState<string>('');
  const [borrowAddress, setBorrowAddress] = useState<string>('');
  const [borrowProductId, setBorrowProductId] = useState<string>('');
  const [borrowUnitId, setBorrowUnitId] = useState<string>('');
  const [borrowQuantity, setBorrowQuantity] = useState<number>(1);
  const [borrowExpectedReturn, setBorrowExpectedReturn] = useState<string>('');
  const [borrowNotes, setBorrowNotes] = useState<string>('');
  const [isSubmittingBorrow, setIsSubmittingBorrow] = useState<boolean>(false);

  // Form State: Loan
  const [loanCounterparty, setLoanCounterparty] = useState<string>('');
  const [loanContact, setLoanContact] = useState<string>('');
  const [loanPhone, setLoanPhone] = useState<string>('');
  const [loanAddress, setLoanAddress] = useState<string>('');
  const [loanProductId, setLoanProductId] = useState<string>('');
  const [loanUnitId, setLoanUnitId] = useState<string>('');
  const [loanQuantity, setLoanQuantity] = useState<number>(1);
  const [loanExpectedReturn, setLoanExpectedReturn] = useState<string>('');
  const [loanNotes, setLoanNotes] = useState<string>('');
  const [isSubmittingLoan, setIsSubmittingLoan] = useState<boolean>(false);

  // Form State: Return
  const [returnQuantity, setReturnQuantity] = useState<number>(1);
  const [returnNotes, setReturnNotes] = useState<string>('');
  const [isSubmittingReturn, setIsSubmittingReturn] = useState<boolean>(false);

  // Form State: Cancel
  const [cancelReason, setCancelReason] = useState<string>('');
  const [isSubmittingCancel, setIsSubmittingCancel] = useState<boolean>(false);

  // Load catalog items (products & units) once
  useEffect(() => {
    const loadCatalog = async () => {
      try {
        const [prodRes, unitRes] = await Promise.all([
          shopApi.listProducts({ size: 200 }),
          shopApi.listUnits(),
        ]);
        const pList = prodRes.data?.content || [];
        setProducts(pList);
        setUnits(unitRes.data || []);
        if (pList.length > 0) {
          setBorrowProductId(pList[0].id);
          setBorrowUnitId(pList[0].baseUnitId);
          setLoanProductId(pList[0].id);
          setLoanUnitId(pList[0].baseUnitId);
        }
      } catch (err: any) {
        console.error('Failed to load products/units for lending:', err);
      }
    };
    loadCatalog();
  }, []);

  const fetchData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (activeTab === 'borrowings') {
        const res = await shopApi.listBorrowings({
          status: statusFilter || undefined,
          size: 50,
        });
        setBorrowings(res.data?.content || []);
      } else if (activeTab === 'loans') {
        const res = await shopApi.listLoans({
          status: statusFilter || undefined,
          size: 50,
        });
        setLoans(res.data?.content || []);
      } else if (activeTab === 'obligations') {
        const res = await shopApi.getLendingObligations();
        setObligations(res.data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load lending data.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [activeTab, statusFilter]);

  // Handlers for creating borrowing
  const handleOpenBorrowModal = () => {
    setBorrowCounterparty('');
    setBorrowContact('');
    setBorrowPhone('');
    setBorrowAddress('');
    setBorrowQuantity(1);
    setBorrowExpectedReturn('');
    setBorrowNotes('');
    if (products.length > 0) {
      setBorrowProductId(products[0].id);
      setBorrowUnitId(products[0].baseUnitId);
    }
    setIsBorrowModalOpen(true);
  };

  const handleProductChangeForBorrow = (productId: string) => {
    setBorrowProductId(productId);
    const prod = products.find((p) => p.id === productId);
    if (prod) {
      setBorrowUnitId(prod.baseUnitId);
    }
  };

  const handleSubmitBorrow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!borrowCounterparty.trim() || !borrowProductId || !borrowUnitId || borrowQuantity <= 0) {
      setError('Please provide counterparty name, product, unit, and valid quantity.');
      return;
    }

    setIsSubmittingBorrow(true);
    setError(null);
    try {
      const dto: CreateBorrowingRequestDto = {
        counterpartyName: borrowCounterparty.trim(),
        counterpartyContactPerson: borrowContact.trim() || undefined,
        counterpartyPhone: borrowPhone.trim() || undefined,
        counterpartyAddress: borrowAddress.trim() || undefined,
        productId: borrowProductId,
        unitDefinitionId: borrowUnitId,
        quantity: borrowQuantity,
        expectedReturnDate: borrowExpectedReturn ? new Date(borrowExpectedReturn).toISOString() : undefined,
        notes: borrowNotes.trim() || undefined,
      };

      const res = await shopApi.createBorrowing(dto);
      setIsBorrowModalOpen(false);
      setSuccessMsg(`Incoming borrowing ${res.data.loanReference} recorded successfully. Physical stock balance updated.`);
      if (activeTab === 'borrowings') {
        fetchData();
      } else {
        setActiveTab('borrowings');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to record borrowing.');
    } finally {
      setIsSubmittingBorrow(false);
    }
  };

  // Handlers for creating loan
  const handleOpenLoanModal = () => {
    setLoanCounterparty('');
    setLoanContact('');
    setLoanPhone('');
    setLoanAddress('');
    setLoanQuantity(1);
    setLoanExpectedReturn('');
    setLoanNotes('');
    if (products.length > 0) {
      setLoanProductId(products[0].id);
      setLoanUnitId(products[0].baseUnitId);
    }
    setIsLoanModalOpen(true);
  };

  const handleProductChangeForLoan = (productId: string) => {
    setLoanProductId(productId);
    const prod = products.find((p) => p.id === productId);
    if (prod) {
      setLoanUnitId(prod.baseUnitId);
    }
  };

  const handleSubmitLoan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loanCounterparty.trim() || !loanProductId || !loanUnitId || loanQuantity <= 0) {
      setError('Please provide borrowing shop name, product, unit, and valid quantity.');
      return;
    }

    setIsSubmittingLoan(true);
    setError(null);
    try {
      const dto: CreateLoanRequestDto = {
        counterpartyName: loanCounterparty.trim(),
        counterpartyContactPerson: loanContact.trim() || undefined,
        counterpartyPhone: loanPhone.trim() || undefined,
        counterpartyAddress: loanAddress.trim() || undefined,
        productId: loanProductId,
        unitDefinitionId: loanUnitId,
        quantity: loanQuantity,
        expectedReturnDate: loanExpectedReturn ? new Date(loanExpectedReturn).toISOString() : undefined,
        notes: loanNotes.trim() || undefined,
      };

      const res = await shopApi.createLoan(dto);
      setIsLoanModalOpen(false);
      setSuccessMsg(`Outgoing loan ${res.data.loanReference} recorded successfully. Physical stock dispatched.`);
      if (activeTab === 'loans') {
        fetchData();
      } else {
        setActiveTab('loans');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to record loan. Ensure sufficient stock on hand.');
    } finally {
      setIsSubmittingLoan(false);
    }
  };

  // Handlers for recording returns
  const handleOpenReturnModal = (loan: StockLoanResponse) => {
    setSelectedLoanForReturn(loan);
    setReturnQuantity(loan.outstandingBaseQuantity);
    setReturnNotes('');
  };

  const handleSubmitReturn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoanForReturn || returnQuantity <= 0) return;

    setIsSubmittingReturn(true);
    setError(null);
    try {
      const dto: RecordLoanReturnDto = {
        quantity: returnQuantity,
        notes: returnNotes.trim() || undefined,
      };

      await shopApi.recordLoanReturn(selectedLoanForReturn.id, dto);
      setSelectedLoanForReturn(null);
      setSuccessMsg(`Return of ${returnQuantity} units recorded on ${selectedLoanForReturn.loanReference}.`);
      fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to record return.');
    } finally {
      setIsSubmittingReturn(false);
    }
  };

  // Handlers for cancellation
  const handleOpenCancelModal = (loan: StockLoanResponse) => {
    setSelectedLoanForCancel(loan);
    setCancelReason('');
  };

  const handleSubmitCancel = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedLoanForCancel || !cancelReason.trim()) return;

    setIsSubmittingCancel(true);
    setError(null);
    try {
      await shopApi.cancelLoan(selectedLoanForCancel.id, { reason: cancelReason.trim() });
      setSelectedLoanForCancel(null);
      setSuccessMsg(`Transaction ${selectedLoanForCancel.loanReference} cancelled and physical stock reversed.`);
      fetchData();
    } catch (err: any) {
      setError(err.message || 'Failed to cancel loan.');
    } finally {
      setIsSubmittingCancel(false);
    }
  };

  const formatDate = (iso?: string) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div>
      <PageHead
        eyebrow="Inventory Management"
        title="Inter-Shop Lending & Borrowing"
        subtitle="Single-tenant tracking of inventory borrowings and loans with external counterparties and physical stock ledger integration"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchData}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            {canManageLending && (
              <>
                <button
                  type="button"
                  className="btn btn-primary btn-sm"
                  onClick={handleOpenBorrowModal}
                >
                  <ArrowDownRight size={14} />
                  <span>Record Borrowing</span>
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleOpenLoanModal}
                >
                  <ArrowUpRight size={14} />
                  <span>Record Loan</span>
                </button>
              </>
            )}
          </div>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}
      {successMsg && <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />}

      {/* Tabs navigation */}
      <div className="tab-nav" style={{ marginBottom: '20px' }}>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'borrowings' ? 'active' : ''}`}
          onClick={() => setActiveTab('borrowings')}
        >
          <ArrowDownRight size={15} />
          <span>Incoming Borrowings (Stock In)</span>
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'loans' ? 'active' : ''}`}
          onClick={() => setActiveTab('loans')}
        >
          <ArrowUpRight size={15} />
          <span>Outgoing Loans (Stock Out)</span>
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'obligations' ? 'active' : ''}`}
          onClick={() => setActiveTab('obligations')}
        >
          <Layers size={15} />
          <span>Obligations & Net Position</span>
        </button>
      </div>

      {/* Filter Bar (for Borrowings and Loans tabs) */}
      {activeTab !== 'obligations' && (
        <div className="filter-bar" style={{ marginBottom: '16px', display: 'flex', gap: '12px', alignItems: 'center' }}>
          <label style={{ fontSize: '13px', color: 'var(--muted)' }}>Status Filter:</label>
          <select
            className="form-select"
            style={{ width: '200px', height: '36px', fontSize: '13px' }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="ACTIVE">ACTIVE (Open)</option>
            <option value="PARTIALLY_RETURNED">PARTIALLY RETURNED</option>
            <option value="RETURNED">RETURNED (Closed)</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
        </div>
      )}

      {/* Borrowings Tab */}
      {activeTab === 'borrowings' && (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Supplying Counterparty</th>
                  <th>Product</th>
                  <th style={{ textAlign: 'right' }}>Initial Qty</th>
                  <th style={{ textAlign: 'right' }}>Outstanding</th>
                  <th>Status</th>
                  <th>Transaction Date</th>
                  <th>Expected Return</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {borrowings.length === 0 ? (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '32px', color: 'var(--muted)' }}>
                      No incoming borrowings recorded yet.
                    </td>
                  </tr>
                ) : (
                  borrowings.map((b) => {
                    const isClosed = b.status === 'RETURNED' || b.status === 'CANCELLED';
                    return (
                      <tr key={b.id}>
                        <td>
                          <span style={{ fontWeight: 700, fontFamily: 'monospace' }}>{b.loanReference}</span>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{b.counterpartyName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>
                            {b.counterpartyContactPerson ? `${b.counterpartyContactPerson} • ` : ''}
                            {b.counterpartyPhone || ''}
                          </div>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{b.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {b.productSku}</div>
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 600 }}>
                          {b.initialQuantity} {b.unitCode}
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 700, color: isClosed ? 'var(--muted)' : 'var(--primary)' }}>
                          {b.outstandingBaseQuantity} {b.unitCode}
                        </td>
                        <td>
                          <StatusBadge status={b.status} />
                        </td>
                        <td>{formatDate(b.transactionDate)}</td>
                        <td>{formatDate(b.expectedReturnDate)}</td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: '4px' }}>
                            <button
                              type="button"
                              className="btn-icon"
                              title="View Details & Return History"
                              onClick={() => setSelectedLoanForDetail(b)}
                            >
                              <Eye size={15} color="var(--teal)" />
                            </button>
                            {!isClosed && canManageLending && (
                              <>
                                <button
                                  type="button"
                                  className="btn-icon"
                                  title="Return Borrowed Stock to Shop"
                                  onClick={() => handleOpenReturnModal(b)}
                                >
                                  <RotateCcw size={15} color="var(--primary)" />
                                </button>
                                {b.returnedBaseQuantity === 0 && (
                                  <button
                                    type="button"
                                    className="btn-icon"
                                    title="Cancel Unreturned Borrowing"
                                    onClick={() => handleOpenCancelModal(b)}
                                  >
                                    <XCircle size={15} color="var(--danger)" />
                                  </button>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Loans Tab */}
      {activeTab === 'loans' && (
        <div className="card">
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Borrowing Counterparty</th>
                  <th>Product</th>
                  <th style={{ textAlign: 'right' }}>Initial Qty</th>
                  <th style={{ textAlign: 'right' }}>Outstanding</th>
                  <th>Status</th>
                  <th>Transaction Date</th>
                  <th>Expected Return</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loans.length === 0 ? (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '32px', color: 'var(--muted)' }}>
                      No outgoing loans recorded yet.
                    </td>
                  </tr>
                ) : (
                  loans.map((l) => {
                    const isClosed = l.status === 'RETURNED' || l.status === 'CANCELLED';
                    return (
                      <tr key={l.id}>
                        <td>
                          <span style={{ fontWeight: 700, fontFamily: 'monospace' }}>{l.loanReference}</span>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{l.counterpartyName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>
                            {l.counterpartyContactPerson ? `${l.counterpartyContactPerson} • ` : ''}
                            {l.counterpartyPhone || ''}
                          </div>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{l.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {l.productSku}</div>
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 600 }}>
                          {l.initialQuantity} {l.unitCode}
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 700, color: isClosed ? 'var(--muted)' : 'var(--teal)' }}>
                          {l.outstandingBaseQuantity} {l.unitCode}
                        </td>
                        <td>
                          <StatusBadge status={l.status} />
                        </td>
                        <td>{formatDate(l.transactionDate)}</td>
                        <td>{formatDate(l.expectedReturnDate)}</td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', gap: '4px' }}>
                            <button
                              type="button"
                              className="btn-icon"
                              title="View Details & Return History"
                              onClick={() => setSelectedLoanForDetail(l)}
                            >
                              <Eye size={15} color="var(--teal)" />
                            </button>
                            {!isClosed && canManageLending && (
                              <>
                                <button
                                  type="button"
                                  className="btn-icon"
                                  title="Record Return from Borrower"
                                  onClick={() => handleOpenReturnModal(l)}
                                >
                                  <RotateCcw size={15} color="var(--teal)" />
                                </button>
                                {l.returnedBaseQuantity === 0 && (
                                  <button
                                    type="button"
                                    className="btn-icon"
                                    title="Cancel Unreturned Loan"
                                    onClick={() => handleOpenCancelModal(l)}
                                  >
                                    <XCircle size={15} color="var(--danger)" />
                                  </button>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Obligations & Net Position Tab */}
      {activeTab === 'obligations' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {/* KPI Cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px' }}>
            <div className="card" style={{ padding: '16px' }}>
              <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>Active Borrowings</div>
              <div style={{ fontSize: '24px', fontWeight: 700, color: 'var(--primary)' }}>
                {obligations?.totalActiveBorrowings ?? 0}
              </div>
            </div>

            <div className="card" style={{ padding: '16px' }}>
              <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>Active Outgoing Loans</div>
              <div style={{ fontSize: '24px', fontWeight: 700, color: 'var(--teal)' }}>
                {obligations?.totalActiveLoans ?? 0}
              </div>
            </div>

            <div className="card" style={{ padding: '16px' }}>
              <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>Total Borrowed Units</div>
              <div style={{ fontSize: '24px', fontWeight: 700 }}>
                {obligations?.totalOutstandingBorrowedBaseUnits ?? 0}
              </div>
            </div>

            <div className="card" style={{ padding: '16px' }}>
              <div style={{ fontSize: '12px', color: 'var(--muted)', marginBottom: '4px' }}>Total Loaned Units</div>
              <div style={{ fontSize: '24px', fontWeight: 700 }}>
                {obligations?.totalOutstandingLoanedBaseUnits ?? 0}
              </div>
            </div>
          </div>

          {/* Obligations by Product Table */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Product Net Lending Position
            </h2>
            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Base Unit</th>
                    <th style={{ textAlign: 'right' }}>Total Borrowed</th>
                    <th style={{ textAlign: 'right' }}>Total Loaned</th>
                    <th style={{ textAlign: 'center' }}>Net Position</th>
                  </tr>
                </thead>
                <tbody>
                  {(!obligations?.productObligations || obligations.productObligations.length === 0) ? (
                    <tr>
                      <td colSpan={5} style={{ textAlign: 'center', padding: '32px', color: 'var(--muted)' }}>
                        No active lending obligations. All borrowed and loaned items are balanced.
                      </td>
                    </tr>
                  ) : (
                    obligations.productObligations.map((po) => {
                      const net = po.netObligationBaseQuantity;
                      let badgeClass = 'badge-neutral';
                      let badgeText = 'Balanced';
                      if (net > 0) {
                        badgeClass = 'badge-warning';
                        badgeText = `We owe ${net} ${po.baseUnitCode}`;
                      } else if (net < 0) {
                        badgeClass = 'badge-success';
                        badgeText = `Owed ${Math.abs(net)} ${po.baseUnitCode}`;
                      }

                      return (
                        <tr key={po.productId}>
                          <td>
                            <div style={{ fontWeight: 600 }}>{po.productName}</div>
                            <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {po.productSku}</div>
                          </td>
                          <td>{po.baseUnitCode}</td>
                          <td style={{ textAlign: 'right', fontWeight: 600 }}>
                            {po.totalBorrowedBaseQuantity} {po.baseUnitCode}
                          </td>
                          <td style={{ textAlign: 'right', fontWeight: 600 }}>
                            {po.totalLoanedBaseQuantity} {po.baseUnitCode}
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span className={`badge ${badgeClass}`} style={{ fontSize: '12px', padding: '3px 10px' }}>
                              {badgeText}
                            </span>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Record Borrowing Modal */}
      {isBorrowModalOpen && (
        <Modal
          isOpen={true}
          title="Record Incoming Borrowing (Stock In)"
          onClose={() => setIsBorrowModalOpen(false)}
          size="large"
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsBorrowModalOpen(false)}
                disabled={isSubmittingBorrow}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSubmitBorrow}
                disabled={isSubmittingBorrow || !borrowCounterparty.trim() || borrowQuantity <= 0}
              >
                {isSubmittingBorrow ? 'Recording...' : 'Record Borrowing'}
              </button>
            </div>
          }
        >
          <form onSubmit={handleSubmitBorrow} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Supplying Counterparty Shop Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Simba Hardware Ltd"
                  value={borrowCounterparty}
                  onChange={(e) => setBorrowCounterparty(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Contact Person</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. John Doe"
                  value={borrowContact}
                  onChange={(e) => setBorrowContact(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Counterparty Phone</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="+254 700 123456"
                  value={borrowPhone}
                  onChange={(e) => setBorrowPhone(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Counterparty Address</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Plot 4B Industrial Area"
                  value={borrowAddress}
                  onChange={(e) => setBorrowAddress(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Select Local Product SKU *</label>
                <select
                  className="form-select"
                  value={borrowProductId}
                  onChange={(e) => handleProductChangeForBorrow(e.target.value)}
                  required
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku}) — Base: {p.baseUnitCode}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Transaction Unit *</label>
                <select
                  className="form-select"
                  value={borrowUnitId}
                  onChange={(e) => setBorrowUnitId(e.target.value)}
                  required
                >
                  {units.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name} ({u.code})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Quantity Borrowed *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-input"
                  value={borrowQuantity}
                  onChange={(e) => setBorrowQuantity(parseFloat(e.target.value) || 0)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Expected Return Date</label>
                <input
                  type="date"
                  className="form-input"
                  value={borrowExpectedReturn}
                  onChange={(e) => setBorrowExpectedReturn(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Notes / Terms</label>
              <textarea
                className="form-textarea"
                placeholder="Reason for borrowing, agreed repayment conditions..."
                value={borrowNotes}
                onChange={(e) => setBorrowNotes(e.target.value)}
              />
            </div>
          </form>
        </Modal>
      )}

      {/* Record Loan Modal */}
      {isLoanModalOpen && (
        <Modal
          isOpen={true}
          title="Record Outgoing Loan (Stock Out)"
          onClose={() => setIsLoanModalOpen(false)}
          size="large"
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsLoanModalOpen(false)}
                disabled={isSubmittingLoan}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSubmitLoan}
                disabled={isSubmittingLoan || !loanCounterparty.trim() || loanQuantity <= 0}
              >
                {isSubmittingLoan ? 'Recording...' : 'Record Loan'}
              </button>
            </div>
          }
        >
          <form onSubmit={handleSubmitLoan} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Borrowing Counterparty Shop Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Apex Hardware Store"
                  value={loanCounterparty}
                  onChange={(e) => setLoanCounterparty(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Contact Person</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Mary Jane"
                  value={loanContact}
                  onChange={(e) => setLoanContact(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Counterparty Phone</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="+254 700 987654"
                  value={loanPhone}
                  onChange={(e) => setLoanPhone(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Counterparty Address</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Highway Junction, Sector 2"
                  value={loanAddress}
                  onChange={(e) => setLoanAddress(e.target.value)}
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Select Local Product SKU *</label>
                <select
                  className="form-select"
                  value={loanProductId}
                  onChange={(e) => handleProductChangeForLoan(e.target.value)}
                  required
                >
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku}) — Base: {p.baseUnitCode}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Transaction Unit *</label>
                <select
                  className="form-select"
                  value={loanUnitId}
                  onChange={(e) => setLoanUnitId(e.target.value)}
                  required
                >
                  {units.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name} ({u.code})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Quantity Loaned *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-input"
                  value={loanQuantity}
                  onChange={(e) => setLoanQuantity(parseFloat(e.target.value) || 0)}
                  required
                />
                <span className="form-hint">Physical stock will be immediately deducted upon confirmation.</span>
              </div>

              <div className="form-group">
                <label className="form-label">Expected Return Date</label>
                <input
                  type="date"
                  className="form-input"
                  value={loanExpectedReturn}
                  onChange={(e) => setLoanExpectedReturn(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Notes / Conditions</label>
              <textarea
                className="form-textarea"
                placeholder="Lending notes, return expectations..."
                value={loanNotes}
                onChange={(e) => setLoanNotes(e.target.value)}
              />
            </div>
          </form>
        </Modal>
      )}

      {/* Record Return Modal (Supports multi-step partial returns) */}
      {selectedLoanForReturn && (
        <Modal
          isOpen={true}
          title={`Record Return — ${selectedLoanForReturn.loanReference}`}
          onClose={() => setSelectedLoanForReturn(null)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedLoanForReturn(null)}
                disabled={isSubmittingReturn}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSubmitReturn}
                disabled={isSubmittingReturn || returnQuantity <= 0 || returnQuantity > selectedLoanForReturn.outstandingBaseQuantity}
              >
                {isSubmittingReturn ? 'Saving Return...' : 'Confirm Return'}
              </button>
            </div>
          }
        >
          <form onSubmit={handleSubmitReturn} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ fontSize: '13px', color: 'var(--muted)', background: 'var(--surface-sunken, #f8fafc)', padding: '12px', borderRadius: '6px' }}>
              <div>Product: <strong>{selectedLoanForReturn.productName} ({selectedLoanForReturn.productSku})</strong></div>
              <div>Counterparty: <strong>{selectedLoanForReturn.counterpartyName}</strong></div>
              <div>Direction: <strong>{selectedLoanForReturn.direction === 'BORROW' ? 'Returning to Supplier' : 'Receiving from Borrower'}</strong></div>
              <div style={{ marginTop: '4px', fontWeight: 700, color: 'var(--primary)' }}>
                Outstanding Balance: {selectedLoanForReturn.outstandingBaseQuantity} {selectedLoanForReturn.unitCode}
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Return Quantity ({selectedLoanForReturn.unitCode}) *</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                max={selectedLoanForReturn.outstandingBaseQuantity}
                className="form-input"
                value={returnQuantity}
                onChange={(e) => setReturnQuantity(parseFloat(e.target.value) || 0)}
                required
                autoFocus
              />
              <span className="form-hint">
                Partial returns are supported. Balance remaining after this return: {(selectedLoanForReturn.outstandingBaseQuantity - returnQuantity).toFixed(2)} {selectedLoanForReturn.unitCode}.
              </span>
            </div>

            <div className="form-group">
              <label className="form-label">Return Notes</label>
              <textarea
                className="form-textarea"
                placeholder="Condition of returned goods, vehicle reference, delivery notes..."
                value={returnNotes}
                onChange={(e) => setReturnNotes(e.target.value)}
              />
            </div>
          </form>
        </Modal>
      )}

      {/* Cancel Loan Modal */}
      {selectedLoanForCancel && (
        <Modal
          isOpen={true}
          title={`Cancel Transaction — ${selectedLoanForCancel.loanReference}`}
          onClose={() => setSelectedLoanForCancel(null)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedLoanForCancel(null)}
                disabled={isSubmittingCancel}
              >
                Back
              </button>
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleSubmitCancel}
                disabled={isSubmittingCancel || !cancelReason.trim()}
              >
                {isSubmittingCancel ? 'Cancelling...' : 'Confirm Cancellation'}
              </button>
            </div>
          }
        >
          <form onSubmit={handleSubmitCancel} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <p style={{ fontSize: '13px', color: 'var(--muted)' }}>
              Cancelling this active loan or borrowing will automatically reverse the physical inventory balance back to its pre-transaction state.
            </p>
            <div className="form-group">
              <label className="form-label">Cancellation Reason *</label>
              <textarea
                className="form-textarea"
                placeholder="e.g. Counterparty cancelled borrowing request prior to delivery..."
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                required
                autoFocus
              />
            </div>
          </form>
        </Modal>
      )}

      {/* Loan Detail & Return History Modal */}
      {selectedLoanForDetail && (
        <Modal
          isOpen={true}
          title={`Lending Details — ${selectedLoanForDetail.loanReference}`}
          onClose={() => setSelectedLoanForDetail(null)}
          size="large"
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedLoanForDetail(null)}
              >
                Close
              </button>
            </div>
          }
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px', background: 'var(--surface-sunken, #f8fafc)', padding: '12px', borderRadius: '6px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Direction</span>
                <div style={{ fontWeight: 700 }}>{selectedLoanForDetail.direction}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Status</span>
                <div><StatusBadge status={selectedLoanForDetail.status} /></div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Counterparty</span>
                <div style={{ fontWeight: 700 }}>{selectedLoanForDetail.counterpartyName}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Product</span>
                <div style={{ fontWeight: 600 }}>{selectedLoanForDetail.productName} ({selectedLoanForDetail.productSku})</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Initial Quantity</span>
                <div style={{ fontWeight: 600 }}>{selectedLoanForDetail.initialQuantity} {selectedLoanForDetail.unitCode}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>Outstanding</span>
                <div style={{ fontWeight: 700, color: 'var(--primary)' }}>
                  {selectedLoanForDetail.outstandingBaseQuantity} {selectedLoanForDetail.unitCode}
                </div>
              </div>
            </div>

            {selectedLoanForDetail.notes && (
              <div>
                <label className="form-label">Notes</label>
                <p style={{ fontSize: '13px', margin: '4px 0', color: 'var(--muted)' }}>
                  {selectedLoanForDetail.notes}
                </p>
              </div>
            )}

            {/* Return Events List */}
            <div>
              <h3 style={{ fontSize: '14px', fontWeight: 700, marginBottom: '8px' }}>
                Return History Logs
              </h3>
              {(!selectedLoanForDetail.returns || selectedLoanForDetail.returns.length === 0) ? (
                <div style={{ fontSize: '12.5px', color: 'var(--muted)', fontStyle: 'italic', padding: '12px 0' }}>
                  No returns recorded on this loan yet.
                </div>
              ) : (
                <div className="table-wrap">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Return Date</th>
                        <th style={{ textAlign: 'right' }}>Quantity</th>
                        <th>Received By</th>
                        <th>Notes</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedLoanForDetail.returns.map((ret) => (
                        <tr key={ret.id}>
                          <td>{formatDate(ret.returnDate)}</td>
                          <td style={{ textAlign: 'right', fontWeight: 600 }}>
                            {ret.quantity} {ret.unitCode}
                          </td>
                          <td>{ret.receivedByUserName || ret.receivedByUserId || 'Staff'}</td>
                          <td style={{ color: 'var(--muted)' }}>{ret.notes || '—'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
