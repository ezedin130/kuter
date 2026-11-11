import React, { useState, useEffect } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle2,
  FileText,
  Eye,
  RefreshCw,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  ProductSummary,
  CreateStockAdjustmentDto,
  CreateStockAdjustmentItemDto,
  StockAdjustmentResponse,
} from '../api/types';

interface AdjustmentLineState {
  productId: string;
  productName: string;
  productSku: string;
  baseUnitCode: string;
  currentStock: number;
  quantityChange: number;
}

export const AdjustmentsPage: React.FC = () => {
  const [activeView, setActiveView] = useState<'form' | 'history'>('form');

  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [adjustments, setAdjustments] = useState<StockAdjustmentResponse[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(false);

  // Form State
  const [reasonCode, setReasonCode] = useState<string>('DAMAGED');
  const [notes, setNotes] = useState<string>('');
  const [items, setItems] = useState<AdjustmentLineState[]>([]);
  const [selectedProductId, setSelectedProductId] = useState<string>('');

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
      const res = await shopApi.listStockAdjustments(0, 20);
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

  const handleAddProduct = () => {
    if (!selectedProductId) return;
    const prod = products.find((p) => p.id === selectedProductId);
    if (!prod) return;

    // Default to -1 (damage or loss)
    const newLine: AdjustmentLineState = {
      productId: prod.id,
      productName: prod.name,
      productSku: prod.sku,
      baseUnitCode: prod.baseUnitCode,
      currentStock: prod.currentStock,
      quantityChange: -1,
    };

    setItems((prev) => [...prev, newLine]);
    setSelectedProductId('');
  };

  const handleUpdateQtyChange = (index: number, val: number) => {
    setItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, quantityChange: val } : item))
    );
  };

  const handleRemoveLine = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleSubmitAdjustment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reasonCode) {
      setError('Adjustment reason code is required.');
      return;
    }
    if (items.length === 0) {
      setError('Please add at least one product to adjust.');
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
      })),
    };

    try {
      const res = await shopApi.adjustStock(payload);
      setSuccessMsg(`Stock adjustment ${res.data.adjustmentNumber} recorded successfully!`);
      setNotes('');
      setItems([]);
      // Reload products to get updated stock balances
      const updated = await shopApi.listProducts({ size: 200 });
      setProducts(updated.data?.content || []);
    } catch (err: any) {
      setError(err.message || 'Failed to submit stock adjustment.');
    } finally {
      setIsSubmitting(false);
    }
  };

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
              className={`btn btn-sm ${activeView === 'form' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('form')}
            >
              <Plus size={14} />
              <span>New Adjustment</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${activeView === 'history' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('history')}
            >
              <FileText size={14} />
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
              Adjustment Parameters
            </h2>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Reason Code *</label>
                <select
                  className="form-select"
                  value={reasonCode}
                  onChange={(e) => setReasonCode(e.target.value)}
                  required
                >
                  <option value="DAMAGED">Damaged Goods / Weather Exposure</option>
                  <option value="BREAKAGE">Breakage during Handling (Tiles/Glass/Blocks)</option>
                  <option value="COUNT_DISCREPANCY">Physical Count Discrepancy (Stocktake Audit)</option>
                  <option value="EXPIRED">Expired / Degraded Material (e.g. Hardened Cement)</option>
                  <option value="CORRECTION">Administrative Entry Correction</option>
                  <option value="OTHER">Other Operational Reason</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Notes & Justification</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Audit reference, damaged location, incident details..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Line Items Card */}
          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Adjusted Items</h2>
              <div style={{ display: 'flex', gap: '8px', minWidth: '320px' }}>
                <select
                  className="select-filter"
                  style={{ flex: 1 }}
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                >
                  <option value="">-- Select Product to Adjust --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku}) — Current: {p.currentStock} {p.baseUnitCode}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleAddProduct}
                  disabled={!selectedProductId}
                >
                  <Plus size={14} />
                  <span>Add Line</span>
                </button>
              </div>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Current On-Hand</th>
                    <th style={{ width: '160px' }}>Adjustment Delta (±)</th>
                    <th>Resulting Balance</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        No items added. Select a product above to record an adjustment.
                      </td>
                    </tr>
                  ) : (
                    items.map((item, idx) => {
                      const newBal = item.currentStock + item.quantityChange;
                      const isNegative = item.quantityChange < 0;

                      return (
                        <tr key={idx}>
                          <td>
                            <div style={{ fontWeight: 700 }}>{item.productName}</div>
                            <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                          </td>
                          <td>
                            {item.currentStock} {item.baseUnitCode}
                          </td>
                          <td>
                            <input
                              type="number"
                              step="0.0001"
                              className="form-input"
                              style={{
                                height: '32px',
                                fontSize: '12.5px',
                                fontWeight: 700,
                                color: isNegative ? 'var(--danger)' : 'var(--success)',
                              }}
                              value={item.quantityChange}
                              onChange={(e) =>
                                handleUpdateQtyChange(idx, parseFloat(e.target.value) || 0)
                              }
                            />
                            <span style={{ fontSize: '10.5px', color: 'var(--muted)' }}>
                              Use negative for loss/damage
                            </span>
                          </td>
                          <td>
                            <strong
                              style={{
                                color: newBal < 0 ? 'var(--danger)' : 'var(--ink)',
                              }}
                            >
                              {newBal} {item.baseUnitCode}
                            </strong>
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              type="button"
                              className="btn-icon"
                              onClick={() => handleRemoveLine(idx)}
                              title="Remove item"
                            >
                              <Trash2 size={14} color="var(--danger)" />
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
                  justifyContent: 'flex-end',
                  marginTop: '20px',
                  paddingTop: '16px',
                  borderTop: '1px solid var(--border)',
                }}
              >
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isSubmitting || items.length === 0}
                >
                  <CheckCircle2 size={16} />
                  <span>{isSubmitting ? 'Posting Adjustment...' : 'Confirm Stock Adjustment'}</span>
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
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLoadingHistory ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      Loading adjustments...
                    </td>
                  </tr>
                ) : adjustments.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      No adjustment records found.
                    </td>
                  </tr>
                ) : (
                  adjustments.map((adj) => (
                    <tr key={adj.id}>
                      <td style={{ fontWeight: 700 }}>{adj.adjustmentNumber}</td>
                      <td>{new Date(adj.adjustedAt).toLocaleDateString()}</td>
                      <td>
                        <span className="badge badge-warning">{adj.reasonCode}</span>
                      </td>
                      <td style={{ maxWidth: '240px', color: 'var(--muted)' }}>{adj.notes || '—'}</td>
                      <td>{adj.adjustedByName}</td>
                      <td>{adj.items?.length || 0} item(s)</td>
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
                  ))
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
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Reason</span>
                <div>
                  <span className="badge badge-warning">{selectedAdjustment.reasonCode}</span>
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Adjusted By</span>
                <div>{selectedAdjustment.adjustedByName}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Date</span>
                <div>{new Date(selectedAdjustment.adjustedAt).toLocaleString()}</div>
              </div>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Before</th>
                    <th>Delta</th>
                    <th>After</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedAdjustment.items?.map((item) => {
                    const isNeg = Number(item.quantityChange) < 0;
                    return (
                      <tr key={item.id}>
                        <td>
                          <div style={{ fontWeight: 700 }}>{item.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                        </td>
                        <td>
                          {item.quantityBefore} {item.baseUnitCode}
                        </td>
                        <td
                          style={{
                            fontWeight: 700,
                            color: isNeg ? 'var(--danger)' : 'var(--success)',
                          }}
                        >
                          {isNeg ? item.quantityChange : `+${item.quantityChange}`} {item.baseUnitCode}
                        </td>
                        <td>
                          <strong>
                            {item.quantityAfter} {item.baseUnitCode}
                          </strong>
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
