import React, { useState, useEffect } from 'react';
import {
  Search,
  ShoppingCart,
  Trash2,
  Plus,
  Minus,
  CheckCircle2,
  Lock,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Alert } from '../components/Alert';
import { SaleReceiptModal } from '../components/SaleReceiptModal';
import { useAuth } from '../context/AuthContext';
import { shopApi } from '../api/client';
import {
  ProductSummary,
  CategoryResponse,
  ProductUnitConversion,
  CreateSaleRequestDto,
  CreateSaleItemRequestDto,
  SaleReceiptResponseDto,
} from '../api/types';
import { useLanguage } from '../i18n/LanguageContext';

interface CartItem {
  product: ProductSummary;
  selectedUnitId: string;
  selectedUnitCode: string;
  toBaseFactor: number;
  quantity: number;
  unitPrice: number;
  discountAmount: number;
  conversions: ProductUnitConversion[];
}

export const SalesPosPage: React.FC = () => {
  const { isOwner } = useAuth();
  const { t, formatCurrency, language } = useLanguage();
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [isLoadingProducts, setIsLoadingProducts] = useState<boolean>(true);

  // Cart state
  const [cart, setCart] = useState<CartItem[]>([]);
  const [customerName, setCustomerName] = useState<string>('');
  const [customerPhone, setCustomerPhone] = useState<string>('');
  const [notes] = useState<string>('');
  const [orderDiscount, setOrderDiscount] = useState<number>(0);

  // Submission & Receipt state
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [successReceipt, setSuccessReceipt] = useState<SaleReceiptResponseDto | null>(null);
  const [isReceiptOpen, setIsReceiptOpen] = useState<boolean>(false);

  // Load products & categories
  useEffect(() => {
    const fetchData = async () => {
      setIsLoadingProducts(true);
      try {
        const [catRes, prodRes] = await Promise.all([
          shopApi.listCategories(true),
          shopApi.listProducts({ size: 100 }),
        ]);
        setCategories(catRes.data?.content || []);
        setProducts(prodRes.data?.content || []);
      } catch (err: unknown) {
        console.error('Failed to load POS catalog:', err);
      } finally {
        setIsLoadingProducts(false);
      }
    };
    fetchData();
  }, []);

  // Filter products by search & category
  const filteredProducts = products.filter((p) => {
    if (selectedCategory && p.categoryId !== selectedCategory) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      const matchName = p.name.toLowerCase().includes(q);
      const matchSku = p.sku.toLowerCase().includes(q);
      const matchBarcode = p.barcode ? p.barcode.toLowerCase().includes(q) : false;
      return matchName || matchSku || matchBarcode;
    }
    return true;
  });

  // Add product to cart
  const handleAddToCart = async (product: ProductSummary) => {
    // Check if already in cart
    const existingIndex = cart.findIndex((item) => item.product.id === product.id);
    if (existingIndex >= 0) {
      setCart((prev) =>
        prev.map((item, idx) =>
          idx === existingIndex ? { ...item, quantity: item.quantity + 1 } : item
        )
      );
      return;
    }

    // Fetch conversions for product if not yet loaded
    let conversions: ProductUnitConversion[] = [];
    try {
      const res = await shopApi.listProductConversions(product.id);
      conversions = res.data || [];
    } catch {
      // Fallback to base unit only
    }

    const newItem: CartItem = {
      product,
      selectedUnitId: product.baseUnitId,
      selectedUnitCode: product.baseUnitCode,
      toBaseFactor: 1,
      quantity: 1,
      unitPrice: product.sellingPrice || 0,
      discountAmount: 0,
      conversions,
    };

    setCart((prev) => [...prev, newItem]);
  };

  // Change selling unit
  const handleUnitChange = (index: number, unitId: string) => {
    setCart((prev) =>
      prev.map((item, idx) => {
        if (idx !== index) return item;

        if (unitId === item.product.baseUnitId) {
          return {
            ...item,
            selectedUnitId: item.product.baseUnitId,
            selectedUnitCode: item.product.baseUnitCode,
            toBaseFactor: 1,
            unitPrice: item.product.sellingPrice || 0,
          };
        }

        const conv = item.conversions.find((c) => c.unitDefinitionId === unitId);
        if (conv) {
          const calculatedPrice = conv.customSellingPrice || (item.product.sellingPrice * conv.toBaseFactor);
          return {
            ...item,
            selectedUnitId: conv.unitDefinitionId,
            selectedUnitCode: conv.unitCode,
            toBaseFactor: conv.toBaseFactor,
            unitPrice: calculatedPrice,
          };
        }
        return item;
      })
    );
  };

  const handleUpdateQty = (index: number, delta: number) => {
    setCart((prev) =>
      prev
        .map((item, idx) => {
          if (idx !== index) return item;
          const newQty = item.quantity + delta;
          return newQty > 0 ? { ...item, quantity: newQty } : null;
        })
        .filter(Boolean) as CartItem[]
    );
  };

  const handleSetQty = (index: number, val: string) => {
    const qty = parseInt(val) || 1;
    setCart((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, quantity: Math.max(1, qty) } : item))
    );
  };

  const handleSetUnitPrice = (index: number, val: string) => {
    if (!isOwner) return; // Only owner can override base selling price
    const price = parseFloat(val) || 0;
    setCart((prev) =>
      prev.map((item, idx) => (idx === index ? { ...item, unitPrice: Math.max(0, price) } : item))
    );
  };

  const handleRemoveFromCart = (index: number) => {
    setCart((prev) => prev.filter((_, idx) => idx !== index));
  };

  // Calculations
  const subtotal = cart.reduce((acc, item) => {
    const lineTotal = item.quantity * item.unitPrice - (item.discountAmount || 0);
    return acc + Math.max(0, lineTotal);
  }, 0);

  const grandTotal = Math.max(0, subtotal - (orderDiscount || 0));

  // Submit sale to backend API
  const handleFinalizeSale = async () => {
    if (cart.length === 0) return;
    setError(null);
    setIsSubmitting(true);

    try {
      const itemsPayload: CreateSaleItemRequestDto[] = cart.map((item) => ({
        productId: item.product.id,
        unitDefinitionId: item.selectedUnitId,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discountAmount: item.discountAmount || 0,
      }));

      const payload: CreateSaleRequestDto = {
        customerName: customerName.trim() || undefined,
        customerPhone: customerPhone.trim() || undefined,
        notes: notes.trim() || undefined,
        discountAmount: orderDiscount || 0,
        items: itemsPayload,
      };

      const res = await shopApi.createSale(payload);
      const savedSale = res.data;

      // Fetch official operational receipt
      const receiptRes = await shopApi.getSaleReceipt(savedSale.id);
      setSuccessReceipt(receiptRes.data);
      setIsReceiptOpen(true);

      // Reset cart
      setCart([]);
      setCustomerName('');
      setCustomerPhone('');
      setOrderDiscount(0);

      // Refresh product stock count
      const prodRes = await shopApi.listProducts({ size: 100 });
      setProducts(prodRes.data?.content || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : (language === 'am' ? 'ሽያጩን ማጠናቀቅ አልተቻለም።' : 'Failed to finalize sale.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="pos-layout-wrapper">
      <PageHead
        eyebrow={language === 'am' ? 'የሽያጭ መመዝገቢያ' : 'Counter Operations'}
        title={t('pos.title')}
        subtitle={t('pos.subtitle')}
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}

      <div className="pos-main-grid">
        {/* Left Pane: Catalog Browser */}
        <div className="pos-catalog-pane">
          <div className="pos-catalog-header">
            <div className="search-input-wrap">
              <Search size={16} />
              <input
                type="text"
                className="input-search"
                placeholder={t('pos.searchProduct')}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>

            <select
              className="select-filter"
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
            >
              <option value="">{t('pos.allCategories')}</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>

          <div className="pos-products-grid">
            {isLoadingProducts ? (
              <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '40px', color: 'var(--muted)' }}>
                {t('common.loading')}
              </div>
            ) : filteredProducts.length === 0 ? (
              <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '40px', color: 'var(--muted)' }}>
                {t('common.noRecordsDesc')}
              </div>
            ) : (
              filteredProducts.map((p) => {
                const isOutOfStock = p.currentStock <= 0;
                return (
                  <div
                    key={p.id}
                    className="pos-product-card"
                    onClick={() => handleAddToCart(p)}
                    style={{ opacity: isOutOfStock ? 0.6 : 1 }}
                  >
                    <div className="pos-card-sku">{p.sku}</div>
                    <div className="pos-card-name">{p.name}</div>
                    {p.categoryName && (
                      <div style={{ fontSize: '11px', color: 'var(--muted)' }}>{p.categoryName}</div>
                    )}
                    <div className="pos-card-price">
                      <span>{formatCurrency(p.sellingPrice)}</span>
                      <span
                        className="pos-card-stock"
                        style={{
                          color: isOutOfStock
                            ? 'var(--danger)'
                            : p.lowStock
                            ? 'var(--warning)'
                            : 'var(--success)',
                        }}
                      >
                        {p.currentStock} {p.baseUnitCode}
                      </span>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right Pane: Cart & Checkout */}
        <div className="pos-cart-pane">
          <div className="pos-cart-header">
            <div className="pos-cart-title">
              <ShoppingCart size={18} color="var(--teal)" />
              <span>{t('pos.cart')} ({cart.length})</span>
            </div>
            {cart.length > 0 && (
              <button
                type="button"
                className="btn-icon"
                onClick={() => setCart([])}
                title={t('pos.clearCart')}
              >
                <Trash2 size={16} color="var(--danger)" />
              </button>
            )}
          </div>

          <div className="pos-cart-items">
            {cart.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--muted)' }}>
                <ShoppingCart size={32} style={{ marginBottom: '8px', opacity: 0.3 }} />
                <p style={{ fontSize: '13px', margin: 0 }}>{t('pos.cartEmpty')}</p>
              </div>
            ) : (
              cart.map((item, idx) => {
                const lineTotal = Math.max(0, item.quantity * item.unitPrice - (item.discountAmount || 0));
                return (
                  <div key={idx} className="cart-item-row">
                    <div className="cart-item-header">
                      <div>
                        <div className="cart-item-name">{item.product.name}</div>
                        <div style={{ fontSize: '11px', color: 'var(--muted)' }}>SKU: {item.product.sku}</div>
                      </div>
                      <button
                        type="button"
                        className="btn-icon"
                        onClick={() => handleRemoveFromCart(idx)}
                        style={{ width: '24px', height: '24px' }}
                      >
                        <Trash2 size={14} color="var(--danger)" />
                      </button>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '8px' }}>
                      {/* Unit Selector (Dual-UOM) */}
                      <div>
                        <label style={{ fontSize: '10.5px', color: 'var(--muted)', display: 'block', marginBottom: '2px' }}>
                          {t('pos.selectUnit')}
                        </label>
                        <select
                          className="form-select"
                          style={{ height: '30px', fontSize: '11.5px', padding: '0 6px' }}
                          value={item.selectedUnitId}
                          onChange={(e) => handleUnitChange(idx, e.target.value)}
                        >
                          <option value={item.product.baseUnitId}>
                            {item.product.baseUnitCode} ({language === 'am' ? 'መሰረታዊ' : 'Base'})
                          </option>
                          {item.conversions.map((conv) => (
                            <option key={conv.id} value={conv.unitDefinitionId}>
                              {conv.unitCode} (×{conv.toBaseFactor})
                            </option>
                          ))}
                        </select>
                      </div>

                      {/* Unit Price Input */}
                      <div>
                        <label style={{ fontSize: '10.5px', color: 'var(--muted)', display: 'flex', alignItems: 'center', gap: '3px', marginBottom: '2px' }}>
                          {t('common.unitPrice')} {!isOwner && <span title="Authoritative owner price (locked for non-owners)"><Lock size={10} /></span>}
                        </label>
                        <input
                          type="number"
                          step="0.01"
                          min="0"
                          disabled={!isOwner}
                          className="form-input"
                          style={{
                            height: '30px',
                            fontSize: '12px',
                            padding: '0 8px',
                            backgroundColor: !isOwner ? 'var(--surface-sunken, #f8fafc)' : undefined,
                            cursor: !isOwner ? 'not-allowed' : undefined,
                          }}
                          value={item.unitPrice}
                          onChange={(e) => handleSetUnitPrice(idx, e.target.value)}
                          title={!isOwner ? 'Only shop owner can modify unit selling price' : undefined}
                        />
                      </div>
                    </div>

                    <div className="cart-item-controls">
                      {/* Quantity Stepper */}
                      <div className="qty-control">
                        <button
                          type="button"
                          className="qty-btn"
                          onClick={() => handleUpdateQty(idx, -1)}
                        >
                          <Minus size={12} />
                        </button>
                        <input
                          type="number"
                          step="1"
                          min="1"
                          className="qty-input"
                          value={item.quantity}
                          onChange={(e) => handleSetQty(idx, e.target.value)}
                        />
                        <button
                          type="button"
                          className="qty-btn"
                          onClick={() => handleUpdateQty(idx, 1)}
                        >
                          <Plus size={12} />
                        </button>
                      </div>

                      <div className="cart-item-total">
                        {formatCurrency(lineTotal)}
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Cart Footer */}
          <div className="pos-cart-footer">
            {/* Customer Details */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px' }}>
              <input
                type="text"
                className="form-input"
                style={{ height: '32px', fontSize: '12px' }}
                placeholder={t('pos.customerName')}
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
              />
              <input
                type="text"
                className="form-input"
                style={{ height: '32px', fontSize: '12px' }}
                placeholder={t('pos.customerPhone')}
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value)}
              />
            </div>

            <div className="cart-summary-line">
              <span>{t('pos.subtotal')}</span>
              <span>{formatCurrency(subtotal)}</span>
            </div>

            <div className="cart-summary-line" style={{ alignItems: 'center' }}>
              <span>{t('pos.orderDiscount')}</span>
              <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>-</span>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  style={{ width: '80px', height: '28px', fontSize: '12px', textAlign: 'right', padding: '0 6px' }}
                  value={orderDiscount}
                  onChange={(e) => setOrderDiscount(parseFloat(e.target.value) || 0)}
                />
              </div>
            </div>

            <div className="cart-total-line">
              <span>{t('pos.grandTotal')}</span>
              <span style={{ color: 'var(--teal)' }}>{formatCurrency(grandTotal)}</span>
            </div>

            <button
              type="button"
              className="btn btn-primary"
              style={{ width: '100%', height: '44px', fontSize: '15px' }}
              onClick={handleFinalizeSale}
              disabled={isSubmitting || cart.length === 0}
            >
              {isSubmitting ? (
                <span>{t('pos.completingSale')}</span>
              ) : (
                <>
                  <CheckCircle2 size={18} />
                  <span>{t('pos.completeSale')}</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Printable Receipt Modal */}
      <SaleReceiptModal
        isOpen={isReceiptOpen}
        onClose={() => setIsReceiptOpen(false)}
        receipt={successReceipt}
      />
    </div>
  );
};
