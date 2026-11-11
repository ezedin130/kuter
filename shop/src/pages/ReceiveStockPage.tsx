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
  ProductUnitConversion,
  CreateStockReceivingDto,
  CreateStockReceivingItemDto,
  StockReceivingResponse,
} from '../api/types';

interface ReceivingLineState {
  productId: string;
  productName: string;
  productSku: string;
  baseUnitId: string;
  baseUnitCode: string;
  receivedUnitId: string;
  receivedUnitCode: string;
  toBaseFactor: number;
  receivedQuantity: number;
  unitCost: number;
  conversions: ProductUnitConversion[];
}

export const ReceiveStockPage: React.FC = () => {
  const [activeView, setActiveView] = useState<'form' | 'history'>('form');

  // Catalog products
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [receipts, setReceipts] = useState<StockReceivingResponse[]>([]);
  const [isLoadingHistory, setIsLoadingHistory] = useState<boolean>(false);

  // Form state
  const [supplierName, setSupplierName] = useState<string>('');
  const [deliveryNoteRef, setDeliveryNoteRef] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [items, setItems] = useState<ReceivingLineState[]>([]);

  // Selected product to add to form
  const [selectedProductId, setSelectedProductId] = useState<string>('');

  // Status & Detail state
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [selectedReceipt, setSelectedReceipt] = useState<StockReceivingResponse | null>(null);
  const [isDetailOpen, setIsDetailOpen] = useState<boolean>(false);

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const res = await shopApi.listProducts({ size: 200 });
        setProducts(res.data?.content || []);
      } catch (err: any) {
        console.error('Failed to load products for stock receipt:', err);
      }
    };
    loadProducts();
  }, []);

  const loadReceipts = async () => {
    setIsLoadingHistory(true);
    try {
      const res = await shopApi.listStockReceipts(0, 20);
      setReceipts(res.data?.content || []);
    } catch (err: any) {
      console.error('Failed to load receipt history:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  useEffect(() => {
    if (activeView === 'history') {
      loadReceipts();
    }
  }, [activeView]);

  const handleAddProductToReceiving = async () => {
    if (!selectedProductId) return;
    const prod = products.find((p) => p.id === selectedProductId);
    if (!prod) return;

    // Fetch conversions for this product
    let conversions: ProductUnitConversion[] = [];
    try {
      const convRes = await shopApi.listProductConversions(prod.id);
      conversions = convRes.data || [];
    } catch {
      // Fallback to base unit only
    }

    const newLine: ReceivingLineState = {
      productId: prod.id,
      productName: prod.name,
      productSku: prod.sku,
      baseUnitId: prod.baseUnitId,
      baseUnitCode: prod.baseUnitCode,
      receivedUnitId: prod.baseUnitId,
      receivedUnitCode: prod.baseUnitCode,
      toBaseFactor: 1,
      receivedQuantity: 1,
      unitCost: prod.costPrice || 0,
      conversions,
    };

    setItems((prev) => [...prev, newLine]);
    setSelectedProductId('');
  };

  const handleUnitChange = (index: number, unitId: string) => {
    setItems((prev) =>
      prev.map((item, idx) => {
        if (idx !== index) return item;

        if (unitId === item.baseUnitId) {
          return {
            ...item,
            receivedUnitId: item.baseUnitId,
            receivedUnitCode: item.baseUnitCode,
            toBaseFactor: 1,
          };
        }

        const match = item.conversions.find((c) => c.unitDefinitionId === unitId);
        if (match) {
          return {
            ...item,
            receivedUnitId: match.unitDefinitionId,
            receivedUnitCode: match.unitCode,
            toBaseFactor: match.toBaseFactor,
          };
        }

        return item;
      })
    );
  };

  const handleUpdateItem = (index: number, field: 'receivedQuantity' | 'unitCost', value: number) => {
    setItems((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, [field]: value } : item))
    );
  };

  const handleRemoveLine = (index: number) => {
    setItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const totalCost = items.reduce((acc, item) => acc + item.receivedQuantity * item.unitCost, 0);

  const handleSubmitReceiving = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supplierName.trim()) {
      setError('Supplier name is required.');
      return;
    }
    if (items.length === 0) {
      setError('Please add at least one line item to receive.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    setSuccessMsg(null);

    const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'recv-' + Date.now();

    const payload: CreateStockReceivingDto = {
      supplierName: supplierName.trim(),
      deliveryNoteRef: deliveryNoteRef.trim() || undefined,
      notes: notes.trim() || undefined,
      idempotencyKey,
      items: items.map((i): CreateStockReceivingItemDto => ({
        productId: i.productId,
        receivedUnitId: i.receivedUnitId,
        receivedQuantity: i.receivedQuantity,
        unitCost: i.unitCost,
      })),
    };

    try {
      const res = await shopApi.receiveStock(payload);
      setSuccessMsg(`Stock receiving ${res.data.receivingNumber} recorded successfully! Inventory balances updated.`);
      setSupplierName('');
      setDeliveryNoteRef('');
      setNotes('');
      setItems([]);
    } catch (err: any) {
      setError(err.message || 'Failed to record stock receiving.');
    } finally {
      setIsSubmitting(false);
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
        eyebrow="Inventory Inbound"
        title="Stock Receiving (Delivery Notes)"
        subtitle="Log inbound deliveries, update moving weighted-average costs, and convert bulk supplier units into base inventory"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className={`btn btn-sm ${activeView === 'form' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('form')}
            >
              <Plus size={14} />
              <span>New Delivery Note</span>
            </button>
            <button
              type="button"
              className={`btn btn-sm ${activeView === 'history' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('history')}
            >
              <FileText size={14} />
              <span>Receiving History</span>
            </button>
          </div>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}
      {successMsg && <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />}

      {activeView === 'form' ? (
        <form onSubmit={handleSubmitReceiving}>
          {/* Header Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              Delivery Note & Supplier Information
            </h2>
            <div className="form-row-3">
              <div className="form-group">
                <label className="form-label">Supplier / Vendor Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. National Steel Mill, Holcim Cement"
                  value={supplierName}
                  onChange={(e) => setSupplierName(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Delivery Note / Invoice Ref</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. DN-2026-8841"
                  value={deliveryNoteRef}
                  onChange={(e) => setDeliveryNoteRef(e.target.value)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Receiving Notes</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Truck license, warehouse bay, condition..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Line Items Card */}
          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Received Line Items</h2>
              {/* Product selector to add */}
              <div style={{ display: 'flex', gap: '8px', minWidth: '320px' }}>
                <select
                  className="select-filter"
                  style={{ flex: 1 }}
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                >
                  <option value="">-- Select Product to Receive --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku})
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={handleAddProductToReceiving}
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
                    <th>Receiving Unit</th>
                    <th style={{ width: '130px' }}>Quantity</th>
                    <th>Base Equivalent</th>
                    <th style={{ width: '140px' }}>Unit Cost</th>
                    <th style={{ textAlign: 'right' }}>Total Cost</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                        No items added yet. Select a product above and click "Add Line".
                      </td>
                    </tr>
                  ) : (
                    items.map((item, idx) => {
                      const baseQty = item.receivedQuantity * item.toBaseFactor;
                      const lineTotal = item.receivedQuantity * item.unitCost;

                      return (
                        <tr key={idx}>
                          <td>
                            <div style={{ fontWeight: 700 }}>{item.productName}</div>
                            <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                          </td>
                          <td>
                            <select
                              className="form-select"
                              style={{ height: '32px', fontSize: '12.5px', padding: '0 8px' }}
                              value={item.receivedUnitId}
                              onChange={(e) => handleUnitChange(idx, e.target.value)}
                            >
                              <option value={item.baseUnitId}>
                                {item.baseUnitCode} (Base)
                              </option>
                              {item.conversions.map((conv) => (
                                <option key={conv.id} value={conv.unitDefinitionId}>
                                  {conv.unitCode} (×{conv.toBaseFactor})
                                </option>
                              ))}
                            </select>
                          </td>
                          <td>
                            <input
                              type="number"
                              step="0.0001"
                              min="0.0001"
                              className="form-input"
                              style={{ height: '32px', fontSize: '12.5px' }}
                              value={item.receivedQuantity}
                              onChange={(e) =>
                                handleUpdateItem(idx, 'receivedQuantity', parseFloat(e.target.value) || 0)
                              }
                            />
                          </td>
                          <td>
                            <span className="uom-badge">
                              {baseQty} {item.baseUnitCode}
                            </span>
                          </td>
                          <td>
                            <input
                              type="number"
                              step="0.01"
                              min="0"
                              className="form-input"
                              style={{ height: '32px', fontSize: '12.5px' }}
                              value={item.unitCost}
                              onChange={(e) =>
                                handleUpdateItem(idx, 'unitCost', parseFloat(e.target.value) || 0)
                              }
                            />
                          </td>
                          <td style={{ textAlign: 'right', fontWeight: 700 }}>
                            {formatCurrency(lineTotal)}
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
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  marginTop: '20px',
                  paddingTop: '16px',
                  borderTop: '1px solid var(--border)',
                }}
              >
                <div style={{ fontSize: '13px', color: 'var(--muted)' }}>
                  Total Lines: <strong>{items.length}</strong>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                  <div style={{ fontSize: '16px', fontWeight: 800, color: 'var(--ink)' }}>
                    Total Inbound Cost: {formatCurrency(totalCost)}
                  </div>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={isSubmitting || items.length === 0}
                  >
                    <CheckCircle2 size={16} />
                    <span>{isSubmitting ? 'Recording Inbound...' : 'Post & Update Stock'}</span>
                  </button>
                </div>
              </div>
            )}
          </div>
        </form>
      ) : (
        /* History View */
        <div className="card">
          <div className="card-header">
            <h2 className="card-title">Past Stock Receivings</h2>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={loadReceipts}
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
                  <th>Receiving #</th>
                  <th>Date</th>
                  <th>Supplier</th>
                  <th>Delivery Ref</th>
                  <th>Received By</th>
                  <th style={{ textAlign: 'right' }}>Total Cost</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLoadingHistory ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      Loading receiving history...
                    </td>
                  </tr>
                ) : receipts.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      No stock receiving records found.
                    </td>
                  </tr>
                ) : (
                  receipts.map((r) => (
                    <tr key={r.id}>
                      <td style={{ fontWeight: 700 }}>{r.receivingNumber}</td>
                      <td>{new Date(r.receivedAt).toLocaleDateString()}</td>
                      <td>{r.supplierName}</td>
                      <td>{r.deliveryNoteRef || '—'}</td>
                      <td>{r.receivedByName}</td>
                      <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(r.totalCost)}</td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          type="button"
                          className="btn-icon"
                          title="View Details"
                          onClick={() => {
                            setSelectedReceipt(r);
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

      {/* Receipt Detail Modal */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={`Receiving Record — ${selectedReceipt?.receivingNumber || ''}`}
        size="large"
        footer={
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setIsDetailOpen(false)}>
            Close
          </button>
        }
      >
        {selectedReceipt && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Supplier</span>
                <div style={{ fontWeight: 700 }}>{selectedReceipt.supplierName}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Delivery Ref</span>
                <div>{selectedReceipt.deliveryNoteRef || '—'}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Received At</span>
                <div>{new Date(selectedReceipt.receivedAt).toLocaleString()}</div>
              </div>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Received Unit</th>
                    <th>Received Qty</th>
                    <th>Base Qty Added</th>
                    <th style={{ textAlign: 'right' }}>Unit Cost</th>
                    <th style={{ textAlign: 'right' }}>Total Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedReceipt.items?.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <div style={{ fontWeight: 700 }}>{item.productName}</div>
                        <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                      </td>
                      <td>{item.receivedUnitCode}</td>
                      <td>{item.receivedQuantity}</td>
                      <td>{item.baseQuantity}</td>
                      <td style={{ textAlign: 'right' }}>{formatCurrency(item.unitCost)}</td>
                      <td style={{ textAlign: 'right', fontWeight: 700 }}>{formatCurrency(item.totalCost)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ textAlign: 'right', fontSize: '16px', fontWeight: 800, color: 'var(--ink)' }}>
              Total Value Received: {formatCurrency(selectedReceipt.totalCost)}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};
