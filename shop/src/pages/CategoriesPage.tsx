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
import { useLanguage } from '../i18n/LanguageContext';

export const CategoriesPage: React.FC = () => {
  const { canManageCatalog } = useAuth();
  const { t, language } = useLanguage();

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
      setFormError(language === 'am' ? 'የምድብ ስም አስፈላጊ ነው።' : 'Category name is required.');
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
      setFormError(err.message || (language === 'am' ? 'ምድቡን ማስቀመጥ አልተቻለም።' : 'Failed to save category.'));
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: string, catName: string) => {
    const confirmMsg = language === 'am'
      ? `ምድብ "${catName}" ይሰረዝ? በዚህ ምድብ ውስጥ ያሉ ምርቶች ምድብ የሌላቸው ይሆናሉ።`
      : `Delete category "${catName}"? Products in this category will become unassigned.`;
    if (!window.confirm(confirmMsg)) {
      return;
    }
    try {
      await shopApi.deleteCategory(id);
      await fetchCategories();
    } catch (err: any) {
      alert((language === 'am' ? 'ምድቡን መሰረዝ አልተቻለም: ' : 'Failed to delete category: ') + (err.message || err));
    }
  };

  return (
    <div>
      <PageHead
        eyebrow={language === 'am' ? 'የካታሎግ አደረጃጀት' : 'Catalog Organization'}
        title={t.categories.title}
        subtitle={t.categories.subtitle}
        actions={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchCategories}
              disabled={isLoading}
            >
              <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
              <span>{t.common.refresh}</span>
            </button>
            {canManageCatalog && (
              <button type="button" className="btn btn-primary btn-sm" onClick={handleOpenAddModal}>
                <Plus size={14} />
                <span>{t.categories.addCategory}</span>
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
              placeholder={language === 'am' ? 'ምድቦችን በስም ይፈልጉ...' : 'Search categories by name...'}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-secondary btn-sm">
            {t.common.search}
          </button>
        </form>

        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th>{t.categories.name}</th>
                <th>{t.categories.description}</th>
                <th style={{ textAlign: 'center' }}>{t.categories.productCount}</th>
                <th>{t.common.status}</th>
                {canManageCatalog && <th style={{ textAlign: 'right' }}>{t.common.actions}</th>}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    {t.common.loading}
                  </td>
                </tr>
              ) : categories.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', padding: '36px', color: 'var(--muted)' }}>
                    {language === 'am'
                      ? 'እስካሁን ምንም ምድቦች አልተፈጠሩም። እቃዎችዎን ለማደራጀት "ምድብ ጨምር" የሚለውን ይጫኑ።'
                      : 'No categories created yet. Click "Add Category" to organize your inventory.'}
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
                      <span className="badge badge-neutral">
                        {cat.productCount} {language === 'am' ? 'እቃዎች' : 'items'}
                      </span>
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
                            title={t.categories.editCategory}
                            onClick={() => handleOpenEditModal(cat)}
                          >
                            <Edit size={16} />
                          </button>
                          <button
                            type="button"
                            className="btn-icon"
                            title={t.categories.deleteCategory}
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
        title={isEditing ? t.categories.editCategory : t.categories.createCategory}
        footer={
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setIsModalOpen(false)}
            >
              {t.common.cancel}
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={handleSave}
              disabled={isSaving}
            >
              {isSaving ? (language === 'am' ? 'በማስቀመጥ ላይ...' : 'Saving...') : isEditing ? t.common.save : t.categories.createCategory}
            </button>
          </div>
        }
      >
        <form onSubmit={handleSave}>
          {formError && <Alert type="error" message={formError} onDismiss={() => setFormError(null)} />}

          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="form-group">
              <label className="form-label">{t.categories.name} *</label>
              <input
                type="text"
                className="form-input"
                placeholder={language === 'am' ? 'ምሳሌ፡ ሲሚንቶ፣ ብረት፣ የቧንቧ እቃዎች' : 'e.g. Cement & Concrete, Steel & Rebar, Plumbing'}
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">{t.categories.description} ({t.common.optional})</label>
              <textarea
                className="form-textarea"
                placeholder={language === 'am' ? 'የምድብ ማስታወሻዎች ወይም ምደባ...' : 'Category notes or departmental classification...'}
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
