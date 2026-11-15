import React, { useState, useEffect } from 'react';
import {
  Plus,
  Trash2,
  CheckCircle2,
  FileText,
  Eye,
  RefreshCw,
  PackagePlus,
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

  // Product Line Input state
  const [selectedProductId, setSelectedProductId] = useState<string>('');
  const [selectedProductConversions, setSelectedProductConversions] = useState<ProductUnitConversion[]>([]);
  const [selectedUnitId, setSelectedUnitId] = useState<string>('');
  const [lineQuantity, setLineQuantity] = useState<number>(1);
  const [lineCost, setLineCost] = useState<number>(0);
  const [isLoadingConversions, setIsLoadingConversions] = useState<boolean>(false);

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

  // Handle product selection in the line input
  const handleProductSelect = async (prodId: string) => {
    setSelectedProductId(prodId);
    if (!prodId) {
      setSelectedProductConversions([]);
      setSelectedUnitId('');
      setLineCost(0);
      return;
    }

    const prod = products.find((p) => p.id === prodId);
    if (!prod) return;

    setSelectedUnitId(prod.baseUnitId);
    setLineCost(prod.costPrice || 0);

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
      setError('Received quantity must be greater than zero.');
      return;
    }
    if (lineCost < 0) {
      setError('Unit purchase cost cannot be negative.');
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

    const newLine: ReceivingLineState = {
      productId: prod.id,
      productName: prod.name,
      productSku: prod.sku,
      baseUnitId: prod.baseUnitId,
      baseUnitCode: prod.baseUnitCode,
      receivedUnitId: selectedUnitId || prod.baseUnitId,
      receivedUnitCode: unitCode,
      toBaseFactor: factor,
      receivedQuantity: lineQuantity,
      unitCost: lineCost,
      conversions: selectedProductConversions,
    };

    setItems((prev) => [...prev, newLine]);
    // Reset product line form
    setSelectedProductId('');
    setSelectedProductConversions([]);
    setSelectedUnitId('');
    setLineQuantity(1);
    setLineCost(0);
    setError(null);
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
      // Reload products
      const updated = await shopApi.listProducts({ size: 200 });
      setProducts(updated.data?.content || []);
    } catch (err: any) {
      setError(err.message || 'Failed to record stock receiving.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const formatCurrency = (val: number) => {
    return `Br ${Number(val || 0).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })}`;
  };

  const selectedProduct = products.find((p) => p.id === selectedProductId);
  const selectedConversion = selectedProductConversions.find((c) => c.unitDefinitionId === selectedUnitId);
  const activeFactor = selectedConversion ? selectedConversion.toBaseFactor : 1;
  const activeUnitCode = selectedConversion ? selectedConversion.unitCode : (selectedProduct?.baseUnitCode || '');
  const activeBaseEquivalent = lineQuantity * activeFactor;
  const activeLineSubtotal = lineQuantity * lineCost;

  return (
    <div>
      <PageHead
        eyebrow="Inventory Inbound"
        title="Stock Receiving"
        subtitle="Log inbound deliveries, update moving average purchase costs, and receive stock in configured units"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => setActiveView('form')}
            >
              <Plus size={16} />
              <span>New Stock Receiving</span>
            </button>
            <button
              type="button"
              className={`btn ${activeView === 'history' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveView('history')}
            >
              <FileText size={16} />
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
              Supplier & Inbound Delivery Details
            </h2>
            <div className="form-row-3">
              <div className="form-group">
                <label className="form-label">Supplier / Vendor Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. National Cement, Mugher Cement, Ethio Steel"
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
                  placeholder="Truck plate, driver name, condition, warehouse bay..."
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </div>
            </div>
          </div>

          {/* Add Product Line Section */}
          <div className="card" style={{ border: '2px solid var(--teal-soft)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '16px' }}>
              <PackagePlus size={20} color="var(--teal)" />
              <h2 className="card-title" style={{ margin: 0 }}>
                Add Products to This Inbound Receiving
              </h2>
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', alignItems: 'flex-end' }}>
              <div className="form-group" style={{ gridColumn: 'span 2' }}>
                <label className="form-label">Select Product *</label>
                <select
                  className="form-select"
                  value={selectedProductId}
                  onChange={(e) => handleProductSelect(e.target.value)}
                >
                  <option value="">-- Choose Product to Receive --</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.sku}) — Current Stock: {p.currentStock} {p.baseUnitCode}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">
                  Receiving Unit {isLoadingConversions ? '(Loading...)' : ''}
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
                <label className="form-label">Quantity Received</label>
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

              <div className="form-group">
                <label className="form-label">Purchase Cost per Unit (Br)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  value={lineCost}
                  onChange={(e) => setLineCost(parseFloat(e.target.value) || 0)}
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
                  <span style={{ color: 'var(--muted)' }}>Product: </span>
                  <strong>{selectedProduct.name}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--muted)' }}>Equivalent Base Stock: </span>
                  <span className="badge badge-info" style={{ fontWeight: 700 }}>
                    {activeBaseEquivalent} {selectedProduct.baseUnitCode}
                  </span>
                  {activeFactor !== 1 && (
                    <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>
                      ({lineQuantity} {activeUnitCode} × {activeFactor})
                    </span>
                  )}
                </div>
                <div>
                  <span style={{ color: 'var(--muted)' }}>Line Total: </span>
                  <strong style={{ color: 'var(--teal-dark)' }}>{formatCurrency(activeLineSubtotal)}</strong>
                </div>
              </div>
            )}
          </div>

          {/* Line Items Card */}
          <div className="card">
            <div className="card-header">
              <h2 className="card-title">Received Line Items ({items.length})</h2>
            </div>

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Receiving Unit</th>
                    <th style={{ width: '130px' }}>Quantity</th>
                    <th>Base Equivalent</th>
                    <th style={{ width: '160px' }}>Purchase Cost (Br)</th>
                    <th style={{ textAlign: 'right' }}>Total Cost (Br)</th>
                    <th style={{ textAlign: 'right' }}>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={7} style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--muted)' }}>
                        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                          <PackagePlus size={32} color="var(--muted)" />
                          <div style={{ fontWeight: 600 }}>No products added to this receiving yet.</div>
                          <div style={{ fontSize: '12.5px' }}>
                            Select a product above and click <strong>Add Product Line</strong>.
                          </div>
                        </div>
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
                              style={{ height: '34px', fontSize: '12.5px', padding: '0 8px' }}
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
                              style={{ height: '34px', fontSize: '12.5px' }}
                              value={item.receivedQuantity}
                              onChange={(e) =>
                                handleUpdateItem(idx, 'receivedQuantity', parseFloat(e.target.value) || 0)
                              }
                            />
                          </td>
                          <td>
                            <span className="badge badge-info">
                              {baseQty} {item.baseUnitCode}
                            </span>
                          </td>
                          <td>
                            <input
                              type="number"
                              step="0.01"
                              min="0"
                              className="form-input"
                              style={{ height: '34px', fontSize: '12.5px' }}
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
                <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
                  <div style={{ fontSize: '18px', fontWeight: 800, color: 'var(--ink)' }}>
                    Total Inbound Cost: {formatCurrency(totalCost)}
                  </div>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    style={{ height: '44px', padding: '0 24px', fontSize: '14px', fontWeight: 700 }}
                    disabled={isSubmitting || items.length === 0}
                  >
                    <CheckCircle2 size={18} />
                    <span>{isSubmitting ? 'Recording Inbound...' : 'Save Stock Receiving & Update Stock'}</span>
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
            <h2 className="card-title">Stock Receiving History</h2>
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
                  <th>Supplier</th>
                  <th>Delivery Note Ref</th>
                  <th>Received Date</th>
                  <th>Received By</th>
                  <th>Lines</th>
                  <th style={{ textAlign: 'right' }}>Total Value</th>
                  <th style={{ textAlign: 'right' }}>Action</th>
                </tr>
              </thead>
              <tbody>
                {isLoadingHistory ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                      Loading stock receiving history...
                    </td>
                  </tr>
                ) : receipts.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--muted)' }}>
                      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                        <div>No stock receiving records found yet.</div>
                        <button
                          type="button"
                          className="btn btn-primary btn-sm"
                          onClick={() => setActiveView('form')}
                        >
                          <Plus size={14} />
                          <span>Start New Stock Receiving</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : (
                  receipts.map((rcv) => {
                    const totalVal = (rcv.items || []).reduce(
                      (acc, item) => acc + (Number(item.receivedQuantity || 0) * Number(item.unitCost || 0)),
                      0
                    );

                    return (
                      <tr key={rcv.id}>
                        <td style={{ fontWeight: 700 }}>{rcv.receivingNumber}</td>
                        <td style={{ fontWeight: 600 }}>{rcv.supplierName}</td>
                        <td>{rcv.deliveryNoteRef || '—'}</td>
                        <td>{new Date(rcv.receivedAt).toLocaleDateString()}</td>
                        <td>{rcv.receivedByName}</td>
                        <td>{rcv.items?.length || 0} item(s)</td>
                        <td style={{ textAlign: 'right', fontWeight: 700 }}>
                          {formatCurrency(totalVal)}
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <button
                            type="button"
                            className="btn-icon"
                            title="View Details"
                            onClick={() => {
                              setSelectedReceipt(rcv);
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

      {/* Stock Receipt Detail Modal */}
      <Modal
        isOpen={isDetailOpen}
        onClose={() => setIsDetailOpen(false)}
        title={`Stock Receiving Details — ${selectedReceipt?.receivingNumber || ''}`}
        size="large"
        footer={
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => setIsDetailOpen(false)}>
            Close
          </button>
        }
      >
        {selectedReceipt && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Supplier</span>
                <div style={{ fontWeight: 700 }}>{selectedReceipt.supplierName}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Delivery Note Ref</span>
                <div>{selectedReceipt.deliveryNoteRef || '—'}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Received By</span>
                <div>{selectedReceipt.receivedByName}</div>
              </div>
              <div>
                <span style={{ fontSize: '11px', color: 'var(--muted)', textTransform: 'uppercase' }}>Received Date</span>
                <div>{new Date(selectedReceipt.receivedAt).toLocaleString()}</div>
              </div>
            </div>

            {selectedReceipt.notes && (
              <div style={{ padding: '8px 12px', background: 'var(--bg-subtle, #f8fafc)', borderRadius: '6px', fontSize: '13px' }}>
                <span style={{ fontWeight: 600 }}>Notes: </span>
                {selectedReceipt.notes}
              </div>
            )}

            <div className="table-wrap">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Received Qty</th>
                    <th>Base Equivalent</th>
                    <th style={{ textAlign: 'right' }}>Unit Cost</th>
                    <th style={{ textAlign: 'right' }}>Total Cost</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedReceipt.items?.map((item) => {
                    const lineTotal = Number(item.receivedQuantity || 0) * Number(item.unitCost || 0);

                    return (
                      <tr key={item.id}>
                        <td>
                          <div style={{ fontWeight: 700 }}>{item.productName}</div>
                          <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.productSku}</div>
                        </td>
                        <td>
                          {item.receivedQuantity} {item.receivedUnitCode}
                        </td>
                        <td>
                          <span className="badge badge-info">
                            {item.baseQuantity} {item.baseUnitCode}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          {formatCurrency(item.unitCost)}
                        </td>
                        <td style={{ textAlign: 'right', fontWeight: 700 }}>
                          {formatCurrency(lineTotal)}
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
