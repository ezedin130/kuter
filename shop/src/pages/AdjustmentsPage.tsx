import React, { useState, useEffect } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle2,
  FileText,
  Eye,
  RefreshCw,
  Sliders,
  TrendingDown,
  TrendingUp,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  ProductSummary,
  ProductUnitConversion,
  CreateStockAdjustmentDto,
  CreateStockAdjustmentItemDto,
  StockAdjustmentResponse,
} from '../api/types';

interface AdjustmentLineState {
  productId: string;
  productName: string;
  productSku: string;
  baseUnitId: string;
  baseUnitCode: string;
  currentStock: number;
  costPrice: number;
  adjustmentType: 'DECREASE' | 'INCREASE';
  selectedUnitId: string;
  selectedUnitCode: string;
  toBaseFactor: number;
  enteredQuantity: number;
  quantityChange: number; // in base units (signed)
  resultingBalance: number;
  conversions: ProductUnitConversion[];
}

export const AdjustmentsPage: React.FC = () => {
  const [activeView, setActiveView] = useState<'form' | 'history'>('form');

  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [adjustments, setAdjustments] = useState<StockAdjustmentResponse[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(false);

  // Form State
  const [reasonCode, setReasonCode] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [items, setItems] = useState<AdjustmentLineState[]>([]);

  const formatReason = (code?: string): string => {
    if (!code) return 'Not specified';
    switch (code.toUpperCase()) {
      case 'DAMAGED':
        return 'Damaged Goods';
      case 'LOST':
        return 'Lost Stock';
      case 'FOUND':
        return 'Found Stock';
      case 'COUNT_CORRECTION':
      case 'COUNT_DISCREPANCY':
        return 'Counting Correction';
      case 'EXPIRED':
        return 'Expired Products';
      case 'BREAKAGE':
        return 'Damaged Goods (Breakage)';
      case 'CORRECTION':
        return 'Administrative Correction';
      case 'OTHER':
        return 'Other';
      default:
        return code;
    }
  };

  // Product Line Input State
  const [selectedProductId, setSelectedProductId] = useState<string>('');
  const [selectedProductConversions, setSelectedProductConversions] = useState<ProductUnitConversion[]>([]);
  const [adjustmentDirection, setAdjustmentDirection] = useState<'DECREASE' | 'INCREASE'>('DECREASE');
  const [selectedUnitId, setSelectedUnitId] = useState<string>('');
  const [lineQuantity, setLineQuantity] = useState<number>(1);
  const [isLoadingConversions, setIsLoadingConversions] = useState<boolean>(false);

  // Status & Detail
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [selectedAdjustment, setSelectedAdjustment] = useState<StockAdjustmentResponse | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(false);

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const res = await shopApi.listProducts({ size: 200 });
        setProducts(res.data?.content || []);
      } catch (err: any) {
        console.error('Failed to load products for stock adjustments:', err);
      }
    };
    loadProducts();
  }, []);

  const loadAdjustments = async () => {
    setIsLoadingHistory(true);
    try {
      const res = await shopApi.listStockAdjustments(0, 30);
      setAdjustments(res.data?.content || []);
    } catch (err: any) {
      console.error('Failed to load adjustments history:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (activeView === 'history') {
      loadAdjustments();
    }
  }, [activeView]);

  // Handle product selection in line input
  const handleProductSelect = async (prodId: string) => {
    setSelectedProductId(prodId);
    if (!prodId) {
      setSelectedProductConversions([]);
      setSelectedUnitId('');
      return;
    }

    const prod = products.find((p) => p.id === prodId);
    if (!prod) return;

    setSelectedUnitId(prod.baseUnitId);

    setIsLoadingConversions(true);
    try {
      const convRes = await shopApi.listProductConversions(prod.id);
      setSelectedProductConversions(convRes.data || []);
    } catch {
      setSelectedProductConversions([]);
    } finally {
      setIsLoadingConversions(false);
    }
  };

  const handleAddLine = () => {
    if (!selectedProductId) return;
    const prod = products.find((p) => p.id === selectedProductId);
    if (!prod) return;

    if (lineQuantity <= 0) {
      setError('Adjustment quantity must be greater than zero.');
      return;
    }

    let unitCode = prod.baseUnitCode;
    let factor = 1;

    if (selectedUnitId && selectedUnitId !== prod.baseUnitId) {
      const match = selectedProductConversions.find((c) => c.unitDefinitionId === selectedUnitId);
      if (match) {
        unitCode = match.unitCode;
        factor = match.toBaseFactor;
      }
    }

    const baseQuantity = lineQuantity * factor;
    const signedQuantityChange = adjustmentDirection === 'DECREASE' ? -baseQuantity : baseQuantity;
    const resultingBal = prod.currentStock + signedQuantityChange;

    if (resultingBal < 0) {
      setError(
        `Cannot decrease ${prod.name} by ${baseQuantity} ${prod.baseUnitCode}. Current stock is only ${prod.currentStock} ${prod.baseUnitCode}.`
      );
      return;
    }

    const newLine: AdjustmentLineState = {
      productId: prod.id,
      productName: prod.name,
      productSku: prod.sku,
      baseUnitId: prod.baseUnitId,
      baseUnitCode: prod.baseUnitCode,
      currentStock: prod.currentStock,
      costPrice: prod.costPrice || 0,
      adjustmentType: adjustmentDirection,
      selectedUnitId: selectedUnitId || prod.baseUnitId,
      selectedUnitCode: unitCode,
      toBaseFactor: factor,
      enteredQuantity: lineQuantity,
      quantityChange: signedQuantityChange,
      resultingBalance: resultingBal,
      conversions: selectedProductConversions,
    };

    setItems((prev) => [...prev, newLine]);
    // Reset product line form
    setSelectedProductId('');
    setSelectedProductConversions([]);
    setSelectedUnitId('');
    setLineQuantity(1);
    setError(null);
  };

  const handleRemoveLine = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleSubmitAdjustment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reasonCode || !reasonCode.trim()) {
      setError('Please select an adjustment reason.');
      return;
    }
    if (reasonCode === 'OTHER' && !notes.trim()) {
      setError('Please provide an explanation in Notes when selecting "Other" as the adjustment reason.');
      return;
    }
    if (items.length === 0) {
      setError('Please add at least one product line to adjust.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSuccessMsg(null);

    const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'adj-' + Date.now();

    const payload: CreateStockAdjustmentDto = {
      reasonCode,
      notes: notes.trim() || undefined,
      idempotencyKey,
      items: items.map((i): CreateStockAdjustmentItemDto => ({
        productId: i.productId,
        quantityChange: i.quantityChange,
        unitId: i.selectedUnitId,
        enteredQuantity: i.enteredQuantity,
      })),
    };

    try {
      const res = await shopApi.adjustStock(payload);
      setSuccessMsg(`Stock adjustment ${res.data.adjustmentNumber} recorded successfully! Inventory balances updated.`);
      setNotes('');
      setItems([]);
      // Reload products to get updated stock balances
      const updated = await shopApi.listProducts({ size: 200 });
      setProducts(updated.data?.content || []);
      // Refresh history and switch to history view
      await loadAdjustments();
      setActiveView('history');
    } catch (err: any) {
      setError(err.message || 'Failed to submit stock adjustment.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const selectedProduct = products.find((p) => p.id === selectedProductId);
  const selectedConversion = selectedProductConversions.find((c) => c.unitDefinitionId === selectedUnitId);
  const activeFactor = selectedConversion ? selectedConversion.toBaseFactor : 1;
  const activeUnitCode = selectedConversion ? selectedConversion.unitCode : (selectedProduct?.baseUnitCode || '');
  const activeBaseEquivalent = lineQuantity * activeFactor;
  const activeResultingStock = selectedProduct
    ? (adjustmentDirection === 'DECREASE'
        ? selectedProduct.currentStock - activeBaseEquivalent
        : selectedProduct.currentStock + activeBaseEquivalent)
    : 0;

  return (
    <div>
      <PageHead
        eyebrow="Inventory Corrections"
        title="Stock Adjustments"
        subtitle="Record inventory shrinkage, damaged goods, physical count discrepancies, and breakage"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setActiveView('form')}
            >
              <Plus size={16} />
              <span>New Adjustment</span>
            </button>
            <button
              type="button"
              className={`btn ${activeView === 'history' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('history')}
            >
              <FileText size={16} />
              <span>Adjustment History</span>
            </button>
          </div>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}
      {successMsg && <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />}

      {activeView === 'form' ? (
        <form onSubmit={handleSubmitAdjustment}>
          {/* Header Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Adjustment Reason & Parameters
            </h2>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Adjustment Reason *</label>
                <select
                  className="form-select"
                  value={reasonCode}
                  onChange={(e) => setReasonCode(e.target.value)}
                  required
                >
                  <option value="">Select adjustment reason</option>
                  <option value="DAMAGED">Damaged Goods</option>
                  <option value="LOST">Lost Stock</option>
                  <option value="FOUND">Found Stock</option>
                  <option value="COUNT_CORRECTION">Counting Correction</option>
                  <option value="EXPIRED">Expired Products</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Notes & Justification {reasonCode === 'OTHER' && <span style={{ color: 'var(--danger)' }}>*</span>}
                </label>
                <input
                  type="text"
                  className="form-input"
                  placeholder={reasonCode === 'OTHER' ? 'Please provide explanation for "Other"...' : 'Batch reference, incident details, location notes...'}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  required={reasonCode === 'OTHER'}
                />
              </div>
            </div>
          </div>

          {/* Add Product Line Section */}
          <div className="card" style={{ border: '2px solid var(--teal-soft)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <Sliders size={20} color="var(--teal)" />
              <h2 className="card-title" style={{ margin: 0 }}>
                Configure Stock Adjustment Line
              </h2>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '16px', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label className="form-label">Select Product *</label>
                <select
                  className="form-select"
                  value={selectedProductId}
                  onChange={(e) => handleProductSelect(e.target.value)}
                >
                  <option value="">-- Choose Product to Adjust --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku}) — Current Stock: {p.currentStock} {p.baseUnitCode}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Adjustment Type *</label>
                <select
                  className="form-select"
                  value={adjustmentDirection}
                  onChange={(e) => setAdjustmentDirection(e.target.value as 'DECREASE' | 'INCREASE')}
                  style={{ fontWeight: 700, color: adjustmentDirection === 'DECREASE' ? 'var(--danger)' : 'var(--success)' }}
                >
                  <option value="DECREASE">▼ Decrease Stock (Damage / Loss / Shrinkage)</option>
                  <option value="INCREASE">▲ Increase Stock (Found / Audit Surplus)</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Unit {isLoadingConversions ? '(Loading...)' : ''}
                </label>
                <select
                  className="form-select"
                  value={selectedUnitId}
                  onChange={(e) => setSelectedUnitId(e.target.value)}
                  disabled={!selectedProductId}
                >
                  {selectedProduct && (
                    <option value={selectedProduct.baseUnitId}>
                      {selectedProduct.baseUnitCode} (Base Unit)
                    </option>
                  )}
                  {selectedProductConversions.map((conv) => (
                    <option key={conv.id} value={conv.unitDefinitionId}>
                      {conv.unitCode} (×{conv.toBaseFactor} {selectedProduct?.baseUnitCode})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Entered Quantity</label>
                <input
                  type="number"
                  step="0.0001"
                  min="0.0001"
                  className="form-input"
                  value={lineQuantity}
                  onChange={(e) => setLineQuantity(parseFloat(e.target.value) || 0)}
                  disabled={!selectedProductId}
                />
              </div>

              <div style={{ paddingBottom: '2px' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  style={{ width: '100%', height: '42px', fontWeight: 700 }}
                  onClick={handleAddLine}
                  disabled={!selectedProductId || lineQuantity <= 0}
                >
                  <Plus size={16} />
                  <span>Add Product Line</span>
                </button>
              </div>
            </div>

            {selectedProduct && (
              <div
                style={{
                  marginTop: '16px',
                  padding: '12px 16px',
                  background: 'var(--bg-subtle, #f8fafc)',
                  borderRadius: '8px',
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: '24px',
                  fontSize: '13px',
                }}
              >
                <div>
                  <span style={{ color: 'var(--muted)' }}>Current Stock: </span>
                  <strong>{selectedProduct.currentStock} {selectedProduct.baseUnitCode}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--muted)' }}>Adjustment Impact: </span>
                  <span
                    className={`badge ${adjustmentDirection === 'DECREASE' ? 'badge-danger' : 'badge-success'}`}
                    style={{ fontWeight: 700 }}
                  >
                    {adjustmentDirection === 'DECREASE' ? `-${activeBaseEquivalent}` : `+${activeBaseEquivalent}`} {selectedProduct.baseUnitCode}
                  </span>
                  {activeFactor !== 1 && (
                    <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>
                      ({lineQuantity} {activeUnitCode} × {activeFactor})
                    </span>
                  )}
                </div>
                <div>
                  <span style={{ color: 'var(--muted)' }}>New Resulting Balance: </span>
                  <strong style={{ color: activeResultingStock < 0 ? 'var(--danger)' : 'var(--ink)' }}>
                    {activeResultingStock} {selectedProduct.baseUnitCode}
                  </strong>
                </div>
              </div>
            )}
          </div>

          {/* Line Items Card */}
          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Adjusted Items ({items.length})</h2>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Type</th>
                    <th>Entered Qty & Unit</th>
                    <th>Base Adjustment Delta</th>
                    <th>Current On-Hand</th>
                    <th>Resulting Balance</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--muted)' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                          <Sliders size={32} color="var(--muted)" />
                          <div style={{ fontWeight: 600 }}>No products added to this adjustment yet.</div>
                          <div style={{ fontSize: '12.5px' }}>
                            Configure a product above and click <strong>Add Product Line</strong>.
                          </div>
                        </div>
                      </td>
                    </tr>
                  ) : (
                    items.map((item, idx) => {
                      const isDecrease = item.adjustmentType === 'DECREASE';

                      return (
                        <tr key={idx}>
                          <td>
                            <div style={{ fontWeight: 700 }}>{item.productName}</div>
                            <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                          </td>
                          <td>
                            <span className={`badge ${isDecrease ? 'badge-danger' : 'badge-success'}`}>
                              {isDecrease ? <TrendingDown size={12} style={{ display: 'inline', marginRight: '4px' }} /> : <TrendingUp size={12} style={{ display: 'inline', marginRight: '4px' }} />}
                              {item.adjustmentType}
                            </span>
                          </td>
                          <td>
                            <strong>{item.enteredQuantity}</strong> {item.selectedUnitCode}
                            {item.toBaseFactor !== 1 && (
                              <div style={{ fontSize: '11px', color: 'var(--muted)' }}>
                                (factor: ×{item.toBaseFactor})
                              </div>
                            )}
                          </td>
                          <td>
                            <span
                              style={{
                                fontWeight: 700,
                                color: isDecrease ? 'var(--danger)' : 'var(--success)',
                              }}
                            >
                              {item.quantityChange > 0 ? `+${item.quantityChange}` : item.quantityChange} {item.baseUnitCode}
                            </span>
                          </td>
                          <td>
                            {item.currentStock} {item.baseUnitCode}
                          </td>
                          <td>
                            <strong style={{ color: item.resultingBalance < 0 ? 'var(--danger)' : 'var(--ink)' }}>
                              {item.resultingBalance} {item.baseUnitCode}
                            </strong>
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              type="button"
                              className="btn-icon"
                              onClick={() => handleRemoveLine(idx)}
                              title="Remove item"
                            >
                              <Trash2 size={16} color="var(--danger)" />
                            </button>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>

            {items.length > 0 && (
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: '20px',
                  paddingTop: '16px',
                  borderTop: '1px solid var(--border)',
                  flexWrap: 'wrap',
                  gap: '16px',
                }}
              >
                <div style={{ fontSize: '14px', color: 'var(--muted)' }}>
                  Total Product Lines: <strong>{items.length}</strong>
                </div>
                <button
                  type="submit"
                  className="btn btn-primary"
                  style={{ height: '44px', padding: '0 24px', fontSize: '14px', fontWeight: 700 }}
                  disabled={isSubmitting || items.length === 0}
                >
                  <CheckCircle2 size={18} />
                  <span>{isSubmitting ? 'Posting Adjustment...' : 'Confirm Stock Adjustment & Update Stock'}</span>
                </button>
              </div>
            )}
          </div>
        </form>
      ) : (
        /* History View */
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Stock Adjustment History</h2>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadAdjustments}
              disabled={isLoadingHistory}
            >
              <RefreshCw size={14} className={isLoadingHistory ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
          </div>

          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Adjustment #</th>
                  <th>Date</th>
                  <th>Reason</th>
                  <th>Notes</th>
                  <th>Adjusted By</th>
                  <th>Lines</th>
                  <th>Valuation Impact</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLoadingHistory ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      Loading adjustments...
                    </td>
                  </tr>
                ) : adjustments.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--muted)' }}>
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                        <div>No stock adjustment records found.</div>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => setActiveView('form')}
                        >
                          <Plus size={14} />
                          <span>Start New Stock Adjustment</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  adjustments.map((adj) => {
                    const totalImpact = (adj.items || []).reduce(
                      (acc, item) => acc + (Number(item.valuationImpact ?? item.financialValue ?? 0)),
                      0
                    );
                    const isNegative = totalImpact < 0;
                    const adjDate = adj.adjustedAt || adj.createdAt;

                    return (
                      <tr key={adj.id}>
                        <td style={{ fontWeight: 700 }}>{adj.adjustmentNumber}</td>
                        <td>{adjDate ? new Date(adjDate).toLocaleDateString() : '—'}</td>
                        <td>
                          <span className="badge badge-warning">{formatReason(adj.reasonCode)}</span>
                        </td>
                        <td style={{ maxWidth: '240px', color: 'var(--muted)' }}>{adj.notes || '—'}</td>
                        <td>{adj.adjustedByName || adj.createdByName || 'Admin'}</td>
                        <td>{adj.items?.length || 0} item(s)</td>
                        <td style={{ fontWeight: 600, color: isNegative ? 'var(--danger)' : totalImpact > 0 ? 'var(--success)' : 'inherit' }}>
                          {isNegative ? '-' : totalImpact > 0 ? '+' : ''}Br {Math.abs(totalImpact).toFixed(2)}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button
                            type="button"
                            className="btn-icon"
                            title="View Details"
                            onClick={() => {
                              setSelectedAdjustment(adj);
                              setIsDetailOpen(true);
                            }}
                          >
                            <Eye size={16} />
                          </button>
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

      {/* Adjustment Detail Modal */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={`Adjustment Details — ${selectedAdjustment?.adjustmentNumber || ''}`}
        size="large"
        footer={
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setIsDetailOpen(false)}>
            Close
          </button>
        }
      >
        {selectedAdjustment && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Reason</span>
                <div>
                  <span className="badge badge-warning">{formatReason(selectedAdjustment.reasonCode)}</span>
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Adjusted By</span>
                <div>{selectedAdjustment.adjustedByName || selectedAdjustment.createdByName || 'Admin'}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Date & Time</span>
                <div>
                  {selectedAdjustment.adjustedAt || selectedAdjustment.createdAt
                    ? new Date(selectedAdjustment.adjustedAt || selectedAdjustment.createdAt!).toLocaleString()
                    : '—'}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Notes</span>
                <div style={{ color: selectedAdjustment.notes ? 'inherit' : 'var(--muted)' }}>
                  {selectedAdjustment.notes || 'No notes provided'}
                </div>
              </div>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Type</th>
                    <th>Entered Qty & Unit</th>
                    <th>Conversion</th>
                    <th>Base Qty</th>
                    <th>Before</th>
                    <th>After</th>
                    <th>Cost at Time of Transaction</th>
                    <th>Total Stock Value Impact</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedAdjustment.items?.map((item) => {
                    const delta = Number(item.quantityChange);
                    const isNeg = delta < 0;
                    const impact = Number(item.valuationImpact ?? item.financialValue ?? 0);
                    const costSnapshot = Number(item.unitCost ?? 0);

                    return (
                      <tr key={item.id}>
                        <td>
                          <div style={{ fontWeight: 700 }}>{item.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                        </td>
                        <td>
                          <span className={`badge ${isNeg ? 'badge-danger' : 'badge-success'}`}>
                            {item.adjustmentType || (isNeg ? 'DECREASE' : 'INCREASE')}
                          </span>
                        </td>
                        <td>
                          <strong>{item.enteredQuantity ?? Math.abs(delta)}</strong> {item.unitCode || item.baseUnitCode}
                        </td>
                        <td>
                          {item.conversionFactor ? `×${item.conversionFactor}` : '1.0'}
                        </td>
                        <td
                          style={{
                            fontWeight: 700,
                            color: isNeg ? 'var(--danger)' : 'var(--success)',
                          }}
                        >
                          {isNeg ? delta : `+${delta}`} {item.baseUnitCode}
                        </td>
                        <td>
                          {Number(item.quantityBefore ?? 0).toFixed(2)} {item.baseUnitCode}
                        </td>
                        <td>
                          <strong>
                            {Number(item.quantityAfter ?? 0).toFixed(2)} {item.baseUnitCode}
                          </strong>
                        </td>
                        <td>
                          Br {costSnapshot.toFixed(2)}
                        </td>
                        <td
                          style={{
                            fontWeight: 700,
                            color: impact < 0 ? 'var(--danger)' : impact > 0 ? 'var(--success)' : 'inherit',
                          }}
                        >
                          {impact < 0 ? '-' : impact > 0 ? '+' : ''}Br {Math.abs(impact).toFixed(2)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
