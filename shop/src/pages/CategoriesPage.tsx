import React, { useState, useEffect } from 'react';
import {
  Plus,
  Edit,
  Trash2,
  RefreshCw,
  Search,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { StatusBadge } from '../components/StatusBadge';
import { Modal } from '../components/Modal';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import {
  CategoryResponse,
  CreateCategoryDto,
  UpdateCategoryDto,
} from '../api/types';
import { useAuth } from '../context/AuthContext';

export const CategoriesPage: React.FC = () => {
  const { canManageCatalog } = useAuth();

  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Add / Edit Modal state
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [isEditing, setIsEditing] = useState<boolean>(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState<string>('');
  const [description, setDescription] = useState<string>('');
  const [formError, setFormError] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);

  const fetchCategories = async () => {
    setIsLoading(true);
    try {
      const res = await shopApi.listCategories(undefined, searchQuery.trim() || undefined);
      if (res.data) {
        setCategories(res.data.content);
      }
    } catch (err: any) {
      console.error('Failed to load categories:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    fetchCategories();
  };

  const handleOpenAddModal = () => {
    setIsEditing(false);
    setEditingId(null);
    setName('');
    setDescription('');
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (cat: CategoryResponse) => {
    setIsEditing(true);
    setEditingId(cat.id);
    setName(cat.name);
    setDescription(cat.description || '');
    setFormError(null);
    setIsModalOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setFormError('Category name is required.');
      return;
    }

    setIsSaving(true);
    setFormError(null);

    try {
      if (isEditing && editingId) {
        const updateDto: UpdateCategoryDto = {
          name: name.trim(),
          description: description.trim() || undefined,
        };
        await shopApi.updateCategory(editingId, updateDto);
      } else {
        const createDto: CreateCategoryDto = {
          name: name.trim(),
          description: description.trim() || undefined,
        };
        await shopApi.createCategory(createDto);
      }

      setIsModalOpen(false);
      await fetchCategories();
    } catch (err: any) {
      setFormError(err.message || 'Failed to save category.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: string, catName: string) => {
    if (!window.confirm(`Delete category "${catName}"? Products in this category will become unassigned.`)) {
      return;
    }
    try {
      await shopApi.deleteCategory(id);
      await fetchCategories();
    } catch (err: any) {
      alert('Failed to delete category: ' + (err.message || err));
    }
  };

  return (
    <div>
      <PageHead
        eyebrow="Catalog Organization"
        title="Product Categories"
        subtitle="Group materials into structured retail categories for fast POS counter search and valuation reports"
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchCategories}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>Refresh</span>
            </button>
            {canManageCatalog && (
              <button type="button" className="btn btn-primary btn-sm" onClick={handleOpenAddModal}>
                <Plus size={14} />
                <span>Add Category</span>
              </button>
            )}
          </div>
        }
      />

      <div className="card">
        {/* Search */}
        <form onSubmit={handleSearch} className="filter-bar">
          <div className="search-input-wrap">
            <Search size={16} />
            <input
              type="text"
              className="input-search"
              placeholder="Search categories by name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-secondary btn-sm">
            Search
          </button>
        </form>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>Category Name</th>
                <th>Description</th>
                <th style={{ textAlign: 'center' }}>Product Count</th>
                <th>Status</th>
                {canManageCatalog && <th style={{ textAlign: 'right' }}>Actions</th>}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    Loading categories...
                  </td>
                </tr>
              ) : categories.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    No categories created yet. Click "Add Category" to organize your inventory.
                  </td>
                </tr>
              ) : (
                categories.map((cat) => (
                  <tr key={cat.id}>
                    <td style={{ fontWeight: 700, color: 'var(--ink)' }}>{cat.name}</td>
                    <td style={{ color: 'var(--muted)', maxWidth: '300px' }}>
                      {cat.description || '—'}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className="badge badge-neutral">{cat.productCount} items</span>
                    </td>
                    <td>
                      <StatusBadge status={cat.active ? 'ACTIVE' : 'INACTIVE'} />
                    </td>
                    {canManageCatalog && (
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'inline-flex', gap: '4px' }}>
                          <button
                            type="button"
                            className="btn-icon"
                            title="Edit Category"
                            onClick={() => handleOpenEditModal(cat)}
                          >
                            <Edit size={16} />
                          </button>
                          <button
                            type="button"
                            className="btn-icon"
                            title="Delete Category"
                            onClick={() => handleDelete(cat.id, cat.name)}
                          >
                            <Trash2 size={16} color="var(--danger)" />
                          </button>
                        </div>
                      </td>
                    )}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add / Edit Category Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={isEditing ? 'Edit Category' : 'Create Category'}
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
              onClick={handleSave}
              disabled={isSaving}
            >
              {isSaving ? 'Saving...' : isEditing ? 'Save Changes' : 'Create Category'}
            </button>
          </div>
        }
      >
        <form onSubmit={handleSave}>
          {formError && <Alert type="error" message={formError} onDismiss={() => setFormError(null)} />}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">Category Name *</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Cement & Concrete, Steel & Rebar, Plumbing"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Description (Optional)</label>
              <textarea
                className="form-textarea"
                placeholder="Category notes or departmental classification..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
};
