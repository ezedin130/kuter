import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Package,
  Plus,
  Trash2,
  RefreshCw,
  Scale,
  DollarSign,
  AlertTriangle,
  CheckCircle2,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  ProductDetail,
  ProductUnitConversion,
  UnitDefinition,
  CreateProductUnitConversionDto,
} from '../api/types';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../i18n/LanguageContext';

interface ProductDetailPageProps {
  productId: string;
  onBack: () => void;
}

export const ProductDetailPage: React.FC<ProductDetailPageProps> = ({
  productId,
  onBack,
}) => {
  const { canManageCatalog, role } = useAuth();
  const { t, formatCurrency, language } = useLanguage();
  const isCashier = role === 'CASHIER';

  const [product, setProduct] = useState<ProductDetail | null>(null);
  const [conversions, setConversions] = useState<ProductUnitConversion[]>([]);
  const [availableUnits, setAvailableUnits] = useState<UnitDefinition[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Add conversion modal
  const [isAddConversionOpen, setIsAddConversionOpen] = useState<boolean>(false);
  const [selectedUnitId, setSelectedUnitId] = useState<string>('');
  const [toBaseFactor, setToBaseFactor] = useState<number>(1);
  const [customPrice, setCustomPrice] = useState<number>(0);
  const [isDefaultSale, setIsDefaultSale] = useState<boolean>(false);
  const [conversionError, setConversionError] = useState<string | null>(null);
  const [isSavingConversion, setIsSavingConversion] = useState<boolean>(false);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [prodRes, convRes, unitsRes] = await Promise.all([
        shopApi.getProduct(productId),
        shopApi.listProductConversions(productId),
        shopApi.listUnits(),
      ]);

      if (prodRes.data) {
        setProduct(prodRes.data);
      }
      if (convRes.data) {
        setConversions(convRes.data);
      }
      if (unitsRes.data) {
        setAvailableUnits(unitsRes.data);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to load product details.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [productId]);

  const handleOpenAddConversion = () => {
    // Filter out base unit and already configured conversions
    const usedIds = new Set([
      product?.baseUnitId,
      ...conversions.map((c) => c.unitDefinitionId),
    ]);
    const eligible = availableUnits.filter((u) => !usedIds.has(u.id));
    setSelectedUnitId(eligible.length > 0 ? eligible[0].id : '');
    setToBaseFactor(1);
    setCustomPrice(0);
    setIsDefaultSale(false);
    setConversionError(null);
    setIsAddConversionOpen(true);
  };

  const handleSaveConversion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUnitId || toBaseFactor <= 0) {
      setConversionError('Please select a secondary unit and specify a positive conversion factor.');
      return;
    }

    setIsSavingConversion(true);
    setConversionError(null);

    const dto: CreateProductUnitConversionDto = {
      unitDefinitionId: selectedUnitId,
      toBaseFactor,
      isDefaultSaleUnit: isDefaultSale,
      customSellingPrice: customPrice > 0 ? customPrice : undefined,
    };

    try {
      await shopApi.addProductConversion(productId, dto);
      setIsAddConversionOpen(false);
      await loadData();
    } catch (err: any) {
      setConversionError(err.message || 'Failed to add unit conversion.');
    } finally {
      setIsSavingConversion(false);
    }
  };

  const handleDeleteConversion = async (unitId: string, unitCode: string) => {
    const confirmMsg = language === 'am'
      ? `ሁለተኛ ደረጃ የመለኪያ ልወጣ "${unitCode}" ይሰረዝ?`
      : `Delete secondary conversion unit "${unitCode}"?`;
    if (!window.confirm(confirmMsg)) return;
    try {
      await shopApi.deleteProductConversion(productId, unitId);
      await loadData();
    } catch (err: any) {
      alert((language === 'am' ? 'ልወጣውን መሰረዝ አልተቻለም: ' : 'Failed to delete conversion: ') + (err.message || err));
    }
  };

  if (isLoading) {
    return (
      <div style={{ textAlign: 'center', padding: '60px', color: 'var(--muted)' }}>
        {language === 'am' ? 'የምርት ዝርዝር እና የመለኪያ መረጃዎችን በመጫን ላይ...' : 'Loading product details and dual-UOM specifications...'}
      </div>
    );
  }

  if (error || !product) {
    return (
      <div>
        <button type="button" className="btn btn-secondary btn-sm" onClick={onBack} style={{ marginBottom: '16px' }}>
          <ArrowLeft size={14} />
          <span>{language === 'am' ? 'ወደ ካታሎግ ተመለስ' : 'Back to Catalog'}</span>
        </button>
        <Alert type="error" message={error || (language === 'am' ? 'ምርቱ አልተገኘም' : 'Product not found')} />
      </div>
    );
  }

  // Eligible secondary units for dropdown
  const usedUnitIds = new Set([
    product.baseUnitId,
    ...conversions.map((c) => c.unitDefinitionId),
  ]);
  const eligibleUnits = availableUnits.filter((u) => !usedUnitIds.has(u.id));

  return (
    <div>
      <button
        type="button"
        className="btn btn-secondary btn-sm"
        onClick={onBack}
        style={{ marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '6px' }}
      >
        <ArrowLeft size={14} />
        <span>{language === 'am' ? 'ወደ ምርቶች ተመለስ' : 'Back to Products'}</span>
      </button>

      <PageHead
        eyebrow={`SKU: ${product.sku}`}
        title={product.name}
        subtitle={product.categoryName ? `${language === 'am' ? 'ምድብ' : 'Category'}: ${product.categoryName}` : (language === 'am' ? 'ያልተመደበ ምድብ' : 'Unassigned Category')}
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={loadData}>
            <RefreshCw size={14} />
            <span>{t.common.refresh}</span>
          </button>
        }
      />

      {/* Overview Cards */}
      {(() => {
        const unitLabel = product.baseUnitName || product.baseUnitCode || (language === 'am' ? 'መለኪያ' : 'Units');
        const stockQty = Number(product.quantityOnHand ?? product.currentStock ?? 0);
        const threshold = Number(product.reorderThreshold || 0);
        const isOutOfStock = stockQty <= 0;
        const isLowStock = isOutOfStock || stockQty <= threshold;
        const stockStatusLabel = isOutOfStock ? t.products.outOfStock : isLowStock ? t.products.lowStock : t.products.inStock;
        const stockStatusColor = isLowStock ? '#dc2626' : '#16a34a';
        const stockStatusBg = isLowStock ? '#fef2f2' : '#f0fdf4';
        const stockStatusBorder = isLowStock ? '#fca5a5' : '#bbf7d0';

        return (
          <div className="stats-grid">
            <div className="stat-card" style={{ borderLeft: `4px solid ${stockStatusColor}` }}>
              <div className="stat-card-header">
                <span>{language === 'am' ? 'በእጅ ላይ ያለ ዝግጁ ክምችት' : 'Available Quantity on Hand'}</span>
                <div className="stat-card-icon" style={{ background: stockStatusBg, color: stockStatusColor }}>
                  <Package size={20} />
                </div>
              </div>
              <div className="stat-card-value" style={{ color: stockStatusColor }}>
                {stockQty} {unitLabel}
              </div>
              <div className="stat-card-footer" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>{language === 'am' ? 'የማስጠንቀቂያ ጣሪያ' : 'Reorder Level'}: {threshold} {unitLabel}</span>
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 700,
                    padding: '2px 8px',
                    borderRadius: '12px',
                    backgroundColor: stockStatusBg,
                    color: stockStatusColor,
                    border: `1px solid ${stockStatusBorder}`,
                  }}
                >
                  {stockStatusLabel}
                </span>
              </div>
            </div>

            <div className="stat-card">
              <div className="stat-card-header">
                <span>{language === 'am' ? 'መነሻ የመሸጫ ዋጋ' : 'Base Selling Price'}</span>
                <div className="stat-card-icon" style={{ background: 'var(--teal-soft)', color: 'var(--teal)' }}>
                  <DollarSign size={20} />
                </div>
              </div>
              <div className="stat-card-value">{formatCurrency(product.sellingPrice)}</div>
              <div className="stat-card-footer">
                <span>{language === 'am' ? 'ለ 1 ' : 'Per 1 '}{unitLabel}</span>
              </div>
            </div>

            {!isCashier && (
              <div className="stat-card">
                <div className="stat-card-header">
                  <span>{language === 'am' ? 'አማካይ የግዢ ወጪ' : 'Average Purchase Cost'}</span>
                  <div className="stat-card-icon" style={{ background: '#f5f3ff', color: '#7c3aed' }}>
                    <Scale size={20} />
                  </div>
                </div>
                <div className="stat-card-value">{formatCurrency(product.averageCost || product.costPrice)}</div>
                <div className="stat-card-footer">
                  <span>{language === 'am' ? 'ጠቅላላ የክምችት ዋጋ' : 'Total Stock Value'}: {formatCurrency(product.totalValuation)}</span>
                </div>
              </div>
            )}

            <div className="stat-card">
              <div className="stat-card-header">
                <span>{language === 'am' ? 'የክምችት ሁኔታ' : 'Stock & Catalog Status'}</span>
                <div className="stat-card-icon" style={{ background: stockStatusBg, color: stockStatusColor }}>
                  {isLowStock ? <AlertTriangle size={20} /> : <CheckCircle2 size={20} />}
                </div>
              </div>
              <div className="stat-card-value" style={{ fontSize: '18px' }}>
                <span
                  style={{
                    color: stockStatusColor,
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                    fontWeight: 700,
                  }}
                >
                  {isLowStock ? <AlertTriangle size={18} color="#dc2626" /> : <CheckCircle2 size={18} color="#16a34a" />}
                  <span>{stockStatusLabel}</span>
                </span>
              </div>
              <div className="stat-card-footer">
                <span>
                  {isOutOfStock
                    ? (language === 'am' ? 'ወዲያውኑ ይሙሉ — አሁን ያለ ክምችት 0 ነው' : 'Replenish immediately — current stock is 0')
                    : isLowStock
                    ? (language === 'am' ? `ከማስጠንቀቂያ ጣሪያ በታች (${product.reorderThreshold} ${unitLabel})` : `Below reorder threshold (${product.reorderThreshold} ${unitLabel})`)
                    : (language === 'am' ? `በቂ ክምችት (${product.currentStock} ${unitLabel} አለ)` : `Sufficient stock (${product.currentStock} ${unitLabel} available)`)}
                </span>
              </div>
            </div>
          </div>
        );
      })()}

      {/* Stock Equivalent in Secondary Units Banner */}
      {conversions.length > 0 && (
        <div
          className="card"
          style={{
            padding: '16px 20px',
            background: '#f8faf9',
            marginBottom: '24px',
          }}
        >
          <div style={{ fontSize: '12px', fontWeight: 700, textTransform: 'uppercase', color: 'var(--muted)', marginBottom: '8px' }}>
            {language === 'am' ? 'አሁን ያለ ክምችት በተመጣጣኝ መለኪያዎች' : 'Current Stock in Equivalent Units'}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', alignItems: 'center' }}>
            <span style={{ fontWeight: 800, fontSize: '15px', color: 'var(--ink)' }}>
              {product.currentStock} {product.baseUnitCode}
            </span>
            {conversions.map((conv) => {
              const eqQty = (product.currentStock / conv.toBaseFactor).toFixed(2);
              return (
                <div key={conv.id} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span style={{ color: 'var(--muted)' }}>=</span>
                  <span className="uom-badge" style={{ fontSize: '13px', padding: '4px 10px' }}>
                    <strong>{eqQty}</strong> {conv.unitCode} (×{conv.toBaseFactor})
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Dual-UOM Conversion Factors Section */}
      <div className="card">
        <div className="card-header">
          <div>
            <h2 className="card-title">{language === 'am' ? 'ድርብ የመለኪያ ልወጣዎች (Dual-UOM)' : 'Dual-UOM Unit Conversions'}</h2>
            <p style={{ fontSize: '12.5px', color: 'var(--muted)', margin: '4px 0 0 0' }}>
              {language === 'am'
                ? 'በተለያዩ መለኪያዎች መግዛት፣ መረከብ፣ መቁጠር እና መሸጥ ያስችላል (ምሳሌ፡ ጥቅል፣ ካርቶን፣ ፓሌት፣ ቶን)'
                : 'Allows purchasing, receiving, counting, and selling in alternative units (e.g. Bundles, Boxes, Pallets, Tons)'}
            </p>
          </div>
          {canManageCatalog && (
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleOpenAddConversion}
              disabled={eligibleUnits.length === 0}
            >
              <Plus size={14} />
              <span>{language === 'am' ? 'የመለኪያ ልወጣ ጨምር' : 'Add Unit Conversion'}</span>
            </button>
          )}
        </div>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{language === 'am' ? 'የመለኪያ ኮድ' : 'Unit Code'}</th>
                <th>{language === 'am' ? 'የመለኪያ ስም' : 'Unit Name'}</th>
                <th>{language === 'am' ? 'የልወጣ ምጥጥን' : 'Conversion Ratio'}</th>
                <th>{language === 'am' ? 'የተሰላ የመሸጫ ዋጋ' : 'Derived Unit Price'}</th>
                <th>{language === 'am' ? 'ነባሪ የሽያጭ መለኪያ' : 'Default Sale Unit'}</th>
                {canManageCatalog && <th style={{ textAlign: 'right' }}>{t.common.actions}</th>}
              </tr>
            </thead>
            <tbody>
              {/* Always display the base unit as the foundational row */}
              <tr style={{ background: '#fcfdfd' }}>
                <td style={{ fontWeight: 700 }}>
                  <span className="badge badge-teal">{product.baseUnitCode}</span>
                </td>
                <td>{product.baseUnitName}</td>
                <td>
                  <strong>1 {product.baseUnitCode}</strong> = 1 {product.baseUnitCode} ({language === 'am' ? 'መነሻ የክምችት መለኪያ' : 'Base Inventory Unit'})
                </td>
                <td style={{ fontWeight: 700 }}>{formatCurrency(product.sellingPrice)}</td>
                <td>
                  <span className="badge badge-neutral">{language === 'am' ? 'መነሻ ነባሪ' : 'Base Default'}</span>
                </td>
                {canManageCatalog && <td style={{ textAlign: 'right', color: 'var(--muted)' }}>{language === 'am' ? 'የስርዓት መነሻ' : 'System Base'}</td>}
              </tr>

              {conversions.map((conv) => {
                const derivedPrice = conv.customSellingPrice || (product.sellingPrice * conv.toBaseFactor);
                return (
                  <tr key={conv.id}>
                    <td style={{ fontWeight: 700 }}>
                      <span className="badge badge-neutral">{conv.unitCode}</span>
                    </td>
                    <td>{conv.unitName}</td>
                    <td>
                      <strong>1 {conv.unitCode}</strong> = {conv.toBaseFactor} {product.baseUnitCode}
                    </td>
                    <td style={{ fontWeight: 700 }}>
                      {formatCurrency(derivedPrice)}
                      {conv.customSellingPrice && (
                        <span style={{ fontSize: '10px', color: 'var(--muted)', marginLeft: '4px' }}>
                          ({language === 'am' ? 'የተለየ ዋጋ' : 'Override'})
                        </span>
                      )}
                    </td>
                    <td>
                      {conv.isDefaultSaleUnit ? (
                        <span className="badge badge-success">{t.common.yes}</span>
                      ) : (
                        <span style={{ color: 'var(--muted)' }}>{t.common.no}</span>
                      )}
                    </td>
                    {canManageCatalog && (
                      <td style={{ textAlign: 'right' }}>
                        <button
                          type="button"
                          className="btn-icon"
                          title={language === 'am' ? 'ልወጣን ሰርዝ' : 'Remove conversion'}
                          onClick={() => handleDeleteConversion(conv.unitDefinitionId, conv.unitCode)}
                        >
                          <Trash2 size={14} color="var(--danger)" />
                        </button>
                      </td>
                    )}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Unit Conversion Modal */}
      <Modal
        isOpen={isAddConversionOpen}
        onClose={() => setIsAddConversionOpen(false)}
        title={`${language === 'am' ? 'ሁለተኛ ደረጃ መለኪያ ጨምር ለ' : 'Add Secondary Unit for'} ${product.name}`}
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsAddConversionOpen(false)}
            >
              {t.common.cancel}
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleSaveConversion}
              disabled={isSavingConversion}
            >
              {isSavingConversion ? (language === 'am' ? 'በመጨመር ላይ...' : 'Adding...') : (language === 'am' ? 'የመለኪያ ልወጣ አስቀምጥ' : 'Save Unit Conversion')}
            </button>
          </div>
        }
      >
        <form onSubmit={handleSaveConversion}>
          {conversionError && (
            <Alert type="error" message={conversionError} onDismiss={() => setConversionError(null)} />
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">{language === 'am' ? 'ሁለተኛ ደረጃ መለኪያ *' : 'Secondary Measurement Unit *'}</label>
              <select
                className="form-select"
                value={selectedUnitId}
                onChange={(e) => setSelectedUnitId(e.target.value)}
                required
              >
                <option value="">{language === 'am' ? '-- ሁለተኛ ደረጃ መለኪያ ይምረጡ --' : '-- Select Secondary Unit --'}</option>
                {eligibleUnits.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name} ({u.code})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">
                {language === 'am'
                  ? `የልወጣ ምጥጥን (በ 1 ሁለተኛ ደረጃ መለኪያ ውስጥ ያለ የ ${product.baseUnitCode} ብዛት) *`
                  : `Conversion Ratio (Number of ${product.baseUnitCode} in 1 Secondary Unit) *`}
              </label>
              <input
                type="number"
                step="0.0001"
                min="0.0001"
                className="form-input"
                placeholder={language === 'am' ? 'ምሳሌ፡ 50 (1 ጥቅል = 50 ፍሬ ከሆነ)' : 'e.g. 50 (if 1 Bundle = 50 Pieces)'}
                value={toBaseFactor}
                onChange={(e) => setToBaseFactor(parseFloat(e.target.value) || 1)}
                required
              />
              <span className="form-hint">
                {language === 'am'
                  ? `ምሳሌ፡ በ 24 ፍሬ ካርቶን የሚሸጡ ከሆነ 24 ያስገቡ። 1 ካርቶን = 24 ${product.baseUnitCode}።`
                  : `Example: If selling by Box of 24 pieces, enter 24. 1 Box = 24 ${product.baseUnitCode}.`}
              </span>
            </div>

            <div className="form-group">
              <label className="form-label">{language === 'am' ? 'ለዚህ መለኪያ የተለየ የመሸጫ ዋጋ (አማራጭ)' : 'Custom Selling Price for this Unit (Optional)'}</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="form-input"
                placeholder={language === 'am'
                  ? `መነሻ ዋጋ × ምጥጥን ለመጠቀም 0 ይተዉ (${formatCurrency(product.sellingPrice * toBaseFactor)})`
                  : `Leave 0 to use base price × factor (${formatCurrency(product.sellingPrice * toBaseFactor)})`}
                value={customPrice}
                onChange={(e) => setCustomPrice(parseFloat(e.target.value) || 0)}
              />
              <span className="form-hint">
                {language === 'am' ? 'የጅምላ ቅናሽ ወይም የጥቅል ዋጋ ለማበጀት ያስችላል።' : 'Allows bulk bundle discounts or packaged unit pricing.'}
              </span>
            </div>

            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '13px' }}>
              <input
                type="checkbox"
                checked={isDefaultSale}
                onChange={(e) => setIsDefaultSale(e.target.checked)}
              />
              <span>{language === 'am' ? 'በፖስ ሽያጭ ወቅት እንደ ነባሪ የመሸጫ መለኪያ ይሁን' : 'Set as default selling unit for POS counter checkout'}</span>
            </label>
          </div>
        </form>
      </Modal>
    </div>
  );
};
