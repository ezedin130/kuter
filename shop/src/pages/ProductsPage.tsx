import React, { useState, useEffect } from 'react';
import {
  Search,
  Plus,
  Edit,
  Trash2,
  Eye,
  RefreshCw,
  Tag,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  ProductSummary,
  CategoryResponse,
  UnitDefinition,
  CreateProductDto,
  UpdateProductDto,
} from '../api/types';
import { useAuth } from '../context/AuthContext';
import { ProductDetailPage } from './ProductDetailPage';

export const ProductsPage: React.FC = () => {
  const { canManageCatalog, canManagePrices, role } = useAuth();
  const isCashier = role === 'CASHIER';

  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [units, setUnits] = useState<UnitDefinition[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [lowStockOnly, setLowStockOnly] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [totalElements, setTotalElements] = useState<number>(0);

  // Selected product detail view
  const [selectedProductId, setSelectedProductId] = useState<string | null>(null);

  // Add / Edit Modal state
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);

  // Owner price update modal state
  const [priceModalProduct, setPriceModalProduct] = useState<ProductSummary | null>(null);
  const [newSellingPrice, setNewSellingPrice] = useState<number>(0);
  const [isUpdatingPrice, setIsUpdatingPrice] = useState<boolean>(false);

  // Form Fields
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [baseUnitId, setBaseUnitId] = useState('');
  const [costPrice, setCostPrice] = useState<number>(0);
  const [sellingPrice, setSellingPrice] = useState<number>(0);
  const [reorderThreshold, setReorderThreshold] = useState<number>(10);
  const [initialQuantity, setInitialQuantity] = useState<number>(0);

  const fetchCatalog = async (p = 0) => {
    setIsLoading(true);
    try {
      const [prodRes, catRes, unitsRes] = await Promise.all([
        shopApi.listProducts({
          page: p,
          size: 20,
          categoryId: selectedCategory || undefined,
          search: searchQuery.trim() || undefined,
          lowStockOnly: lowStockOnly || undefined,
        }),
        shopApi.listCategories(true),
        shopApi.listUnits(),
      ]);

      if (prodRes.data) {
        setProducts(prodRes.data.content);
        setPage(prodRes.data.pageNumber);
        setTotalPages(prodRes.data.totalPages);
        setTotalElements(prodRes.data.totalElements);
      }
      if (catRes.data) {
        setCategories(catRes.data.content);
      }
      if (unitsRes.data) {
        setUnits(unitsRes.data);
      }
    } catch (err: any) {
      console.error('Failed to load products:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCatalog(0);
  }, [selectedCategory, lowStockOnly]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchCatalog(0);
  };

  const handleOpenAddModal = () => {
    setIsEditing(false);
    setEditingId(null);
    setSku('');
    setName('');
    setDescription('');
    setCategoryId('');
    setBaseUnitId(units.length > 0 ? units[0].id : '');
    setCostPrice(0);
    setSellingPrice(0);
    setReorderThreshold(10);
    setInitialQuantity(0);
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = async (prodSummary: ProductSummary) => {
    try {
      const detail = await shopApi.getProduct(prodSummary.id);
      setIsEditing(true);
      setEditingId(prodSummary.id);
      setSku(detail.data.sku);
      setName(detail.data.name);
      setDescription(detail.data.description || '');
      setCategoryId(detail.data.categoryId || '');
      setBaseUnitId(detail.data.baseUnitId);
      setCostPrice(detail.data.costPrice || 0);
      setSellingPrice(detail.data.sellingPrice || 0);
      setReorderThreshold(detail.data.reorderThreshold || 0);
      setInitialQuantity(0);
      setFormError(null);
      setIsModalOpen(true);
    } catch (err: any) {
      alert('Failed to load product detail for editing: ' + (err.message || err));
    }
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !sku.trim() || !baseUnitId) {
      setFormError('SKU, Product Name, and Base Unit are required.');
      return;
    }

    setIsSaving(true);
    setFormError(null);

    try {
      if (isEditing && editingId) {
        const updateDto: UpdateProductDto = {
          name: name.trim(),
          description: description.trim() || undefined,
          categoryId: categoryId || undefined,
          baseUnitId,
          costPrice,
          sellingPrice,
          reorderThreshold,
        };
        await shopApi.updateProduct(editingId, updateDto);
      } else {
        const createDto: CreateProductDto = {
          sku: sku.trim(),
          name: name.trim(),
          description: description.trim() || undefined,
          categoryId: categoryId || undefined,
          baseUnitId,
          costPrice,
          sellingPrice,
          reorderThreshold,
          initialQuantity: initialQuantity > 0 ? initialQuantity : undefined,
        };
        await shopApi.createProduct(createDto);
      }

      setIsModalOpen(false);
      await fetchCatalog(page);
    } catch (err: any) {
      setFormError(err.message || 'Failed to save product.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteProduct = async (id: string, prodName: string) => {
    if (!window.confirm(`Are you sure you want to deactivate or remove product "${prodName}"?`)) {
      return;
    }
    try {
      await shopApi.deleteProduct(id);
      await fetchCatalog(page);
    } catch (err: any) {
      alert('Failed to delete product: ' + (err.message || err));
    }
  };

  const handleOpenPriceModal = (product: ProductSummary) => {
    setPriceModalProduct(product);
    setNewSellingPrice(product.sellingPrice || 0);
  };

  const handleSavePrice = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!priceModalProduct || newSellingPrice <= 0) return;
    setIsUpdatingPrice(true);
    try {
      await shopApi.updateProductSellingPrice(priceModalProduct.id, { sellingPrice: newSellingPrice });
      setPriceModalProduct(null);
      await fetchCatalog(page);
    } catch (err: any) {
      alert('Failed to update selling price: ' + (err.message || err));
    } finally {
      setIsUpdatingPrice(false);
    }
  };

  const formatCurrency = (val?: number) => {
    if (val == null) return '—';
    return Number(val).toLocaleString(undefined, {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  // If a product is selected for detail view, render ProductDetailPage
  if (selectedProductId) {
    return (
      <ProductDetailPage
        productId={selectedProductId}
        onBack={() => {
          setSelectedProductId(null);
          fetchCatalog(page);
        }}
      />
    );
  }

  return (
    <div>
      <PageHead
        eyebrow="Catalog Management"
        title="Products & Inventory Catalog"
        subtitle="Manage building material specifications, base UOM, dual-unit conversions, and selling prices"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => fetchCatalog(page)}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            {canManageCatalog && (
              <button type="button" className="btn btn-primary btn-sm" onClick={handleOpenAddModal}>
                <Plus size={14} />
                <span>Add Product</span>
              </button>
            )}
          </div>
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
              placeholder="Search by SKU, product name, or barcode..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>

          <select
            className="select-filter"
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          <label
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              color: lowStockOnly ? 'var(--warning)' : 'var(--muted)',
            }}
          >
            <input
              type="checkbox"
              checked={lowStockOnly}
              onChange={(e) => setLowStockOnly(e.target.checked)}
            />
            <span>Low Stock Only</span>
          </label>

          <button type="submit" className="btn btn-secondary btn-sm">
            Filter
          </button>
        </form>

        {/* Products Table */}
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product Name</th>
                <th>Category</th>
                <th>Base Unit</th>
                <th style={{ textAlign: 'center' }}>Stock On Hand</th>
                <th style={{ textAlign: 'right' }}>Selling Price</th>
                {!isCashier && <th style={{ textAlign: 'right' }}>Avg Cost</th>}
                <th>Status</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={isCashier ? 8 : 9} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    Loading product catalog...
                  </td>
                </tr>
              ) : products.length === 0 ? (
                <tr>
                  <td colSpan={isCashier ? 8 : 9} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    No products found. Use "Add Product" to create your first catalog item.
                  </td>
                </tr>
              ) : (
                products.map((p) => (
                  <tr key={p.id}>
                    <td style={{ fontWeight: 700, fontFamily: 'monospace' }}>{p.sku}</td>
                    <td>
                      <div
                        style={{ fontWeight: 700, color: 'var(--ink)', cursor: 'pointer' }}
                        onClick={() => setSelectedProductId(p.id)}
                        title="Click to view details and unit conversions"
                      >
                        {p.name}
                      </div>
                      {p.brand && <div style={{ fontSize: '11px', color: 'var(--muted)' }}>Brand: {p.brand}</div>}
                    </td>
                    <td>{p.categoryName || 'Unassigned'}</td>
                    <td>
                      <span className="badge badge-neutral">{p.baseUnitCode}</span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span
                        className={`badge ${
                          p.currentStock <= 0
                            ? 'badge-danger'
                            : p.lowStock
                            ? 'badge-warning'
                            : 'badge-success'
                        }`}
                      >
                        {p.currentStock} {p.baseUnitCode}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 700 }}>
                      {formatCurrency(p.sellingPrice)}
                    </td>
                    {!isCashier && (
                      <td style={{ textAlign: 'right', color: 'var(--muted)' }}>
                        {formatCurrency(p.costPrice)}
                      </td>
                    )}
                    <td>
                      <StatusBadge status={p.status} />
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '4px' }}>
                        <button
                          type="button"
                          className="btn-icon"
                          title="View Details & Conversions"
                          onClick={() => setSelectedProductId(p.id)}
                        >
                          <Eye size={16} color="var(--teal)" />
                        </button>
                        {canManageCatalog && (
                          <>
                            {canManagePrices && (
                              <button
                                type="button"
                                className="btn-icon"
                                title="Update Selling Price (Owner Only)"
                                onClick={() => handleOpenPriceModal(p)}
                              >
                                <Tag size={16} color="var(--primary)" />
                              </button>
                            )}
                            <button
                              type="button"
                              className="btn-icon"
                              title="Edit Product"
                              onClick={() => handleOpenEditModal(p)}
                            >
                              <Edit size={16} />
                            </button>
                            <button
                              type="button"
                              className="btn-icon"
                              title="Delete Product"
                              onClick={() => handleDeleteProduct(p.id, p.name)}
                            >
                              <Trash2 size={16} color="var(--danger)" />
                            </button>
                          </>
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
              Showing {products.length} of {totalElements} products (Page {page + 1} of {totalPages})
            </span>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page === 0}
                onClick={() => fetchCatalog(page - 1)}
              >
                Previous
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                disabled={page >= totalPages - 1}
                onClick={() => fetchCatalog(page + 1)}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Add / Edit Product Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={isEditing ? 'Edit Product' : 'Add New Product'}
        size="large"
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsModalOpen(false)}
            >
              Cancel
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleSaveProduct}
              disabled={isSaving}
            >
              {isSaving ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Product'}
            </button>
          </div>
        }
      >
        <form onSubmit={handleSaveProduct}>
          {formError && <Alert type="error" message={formError} onDismiss={() => setFormError(null)} />}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">SKU / Code *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. CEM-425-50KG"
                  value={sku}
                  onChange={(e) => setSku(e.target.value)}
                  disabled={isEditing}
                  required
                />
                <span className="form-hint">Unique product code identifier</span>
              </div>

              <div className="form-group">
                <label className="form-label">Product Name *</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Portland Cement Type I/II (50kg Bag)"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Category</label>
                <select
                  className="form-select"
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                >
                  <option value="">-- Unassigned --</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Base Stock Unit *</label>
                <select
                  className="form-select"
                  value={baseUnitId}
                  onChange={(e) => setBaseUnitId(e.target.value)}
                  disabled={isEditing}
                  required
                >
                  <option value="">-- Select Base Unit --</option>
                  {units.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name} ({u.code})
                    </option>
                  ))}
                </select>
                <span className="form-hint">Base UOM cannot be changed once stock movements exist</span>
              </div>
            </div>

            <div className="form-row-3">
              <div className="form-group">
                <label className="form-label">
                  Default Selling Price * {isEditing && !canManagePrices && <span style={{ fontSize: '11px', color: 'var(--amber)' }}>(Owner only)</span>}
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  disabled={isEditing && !canManagePrices}
                  className="form-input"
                  style={{
                    backgroundColor: isEditing && !canManagePrices ? 'var(--surface-sunken, #f8fafc)' : undefined,
                    cursor: isEditing && !canManagePrices ? 'not-allowed' : undefined,
                  }}
                  value={sellingPrice}
                  onChange={(e) => setSellingPrice(parseFloat(e.target.value) || 0)}
                  required
                />
                {isEditing && !canManagePrices && (
                  <span className="form-hint" style={{ color: 'var(--muted)' }}>
                    Only the shop owner is authorized to modify selling prices.
                  </span>
                )}
              </div>

              <div className="form-group">
                <label className="form-label">Initial Cost Price</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="form-input"
                  value={costPrice}
                  onChange={(e) => setCostPrice(parseFloat(e.target.value) || 0)}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Reorder Threshold</label>
                <input
                  type="number"
                  step="1"
                  min="0"
                  className="form-input"
                  value={reorderThreshold}
                  onChange={(e) => setReorderThreshold(parseFloat(e.target.value) || 0)}
                />
                <span className="form-hint">Alerts when stock drops to or below</span>
              </div>
            </div>

            {!isEditing && (
              <div className="form-group">
                <label className="form-label">Initial Quantity on Hand (Optional)</label>
                <input
                  type="number"
                  step="1"
                  min="0"
                  className="form-input"
                  value={initialQuantity}
                  onChange={(e) => setInitialQuantity(parseFloat(e.target.value) || 0)}
                />
                <span className="form-hint">Opening stock balance recorded as an initial receipt</span>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Description (Optional)</label>
              <textarea
                className="form-textarea"
                placeholder="Product specifications, grade, manufacturer notes..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>
        </form>
      </Modal>

      {/* Quick Price Update Modal (Owner only) */}
      {priceModalProduct && (
        <Modal
          isOpen={true}
          title={`Update Selling Price — ${priceModalProduct.name}`}
          onClose={() => setPriceModalProduct(null)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setPriceModalProduct(null)}
                disabled={isUpdatingPrice}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleSavePrice}
                disabled={isUpdatingPrice || newSellingPrice <= 0}
              >
                {isUpdatingPrice ? 'Saving...' : 'Save Price'}
              </button>
            </div>
          }
        >
          <div>
            <p style={{ fontSize: '13px', color: 'var(--muted)', marginBottom: '12px' }}>
              SKU: <strong>{priceModalProduct.sku}</strong> | Base Unit: <strong>{priceModalProduct.baseUnitCode}</strong>
            </p>
            <div className="form-group">
              <label className="form-label">Authoritative Selling Price *</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                className="form-input"
                value={newSellingPrice}
                onChange={(e) => setNewSellingPrice(parseFloat(e.target.value) || 0)}
                required
                autoFocus
              />
              <span className="form-hint">
                This owner-controlled price will be strictly enforced during POS checkouts across all staff terminals.
              </span>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
