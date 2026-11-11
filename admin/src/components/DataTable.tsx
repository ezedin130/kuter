import React from 'react';
import { Search, ChevronLeft, ChevronRight, Inbox, AlertCircle, RefreshCw } from 'lucide-react';

export interface Column<T> {
  key: string;
  header: string;
  width?: string;
  render?: (item: T) => React.ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  data: T[];
  loading: boolean;
  error?: string | null;
  onRetry?: () => void;
  page?: number;
  totalPages?: number;
  totalElements?: number;
  onPageChange?: (page: number) => void;
  searchPlaceholder?: string;
  searchValue?: string;
  onSearchChange?: (val: string) => void;
  filterComponent?: React.ReactNode;
  emptyTitle?: string;
  emptyMessage?: string;
  rowKey: (item: T) => string;
}

export function DataTable<T>({
  columns,
  data,
  loading,
  error,
  onRetry,
  page = 0,
  totalPages = 1,
  totalElements = 0,
  onPageChange,
  searchPlaceholder = 'Search records...',
  searchValue,
  onSearchChange,
  filterComponent,
  emptyTitle = 'No records found',
  emptyMessage = 'There are no items matching the current criteria.',
  rowKey,
}: DataTableProps<T>) {
  return (
    <div className="table-container-card">
      {/* Table Toolbar */}
      {(onSearchChange || filterComponent) && (
        <div className="table-toolbar">
          {onSearchChange && (
            <div className="table-search-input-wrap">
              <Search size={16} className="search-icon" />
              <input
                type="text"
                placeholder={searchPlaceholder}
                value={searchValue || ''}
                onChange={(e) => onSearchChange(e.target.value)}
                className="table-search-input"
              />
              {searchValue && (
                <button
                  type="button"
                  className="search-clear-btn"
                  onClick={() => onSearchChange('')}
                >
                  Clear
                </button>
              )}
            </div>
          )}

          {filterComponent && <div className="table-toolbar-filters">{filterComponent}</div>}
        </div>
      )}

      {/* Main Table Content */}
      <div className="table-responsive-wrapper">
        <table className="kuter-data-table">
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} style={col.width ? { width: col.width } : undefined}>
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              // Loading Skeletons
              Array.from({ length: 5 }).map((_, idx) => (
                <tr key={`skeleton-${idx}`} className="skeleton-row">
                  {columns.map((col) => (
                    <td key={col.key}>
                      <div className="skeleton-cell" />
                    </td>
                  ))}
                </tr>
              ))
            ) : error ? (
              // Error State
              <tr>
                <td colSpan={columns.length} className="table-message-cell">
                  <div className="table-state-box error-state">
                    <AlertCircle size={28} className="state-icon" />
                    <h4>Failed to load data</h4>
                    <p>{error}</p>
                    {onRetry && (
                      <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
                        <RefreshCw size={14} /> Retry
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ) : data.length === 0 ? (
              // Empty State
              <tr>
                <td colSpan={columns.length} className="table-message-cell">
                  <div className="table-state-box empty-state">
                    <Inbox size={32} className="state-icon" />
                    <h4>{emptyTitle}</h4>
                    <p>{emptyMessage}</p>
                  </div>
                </td>
              </tr>
            ) : (
              // Data Rows
              data.map((item) => (
                <tr key={rowKey(item)}>
                  {columns.map((col) => (
                    <td key={col.key}>
                      {col.render ? col.render(item) : (item as Record<string, unknown>)[col.key] as React.ReactNode}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Table Pagination Footer */}
      {!loading && !error && totalElements > 0 && onPageChange && (
        <div className="table-pagination-footer">
          <div className="pagination-info">
            Showing Page <strong>{page + 1}</strong> of <strong>{Math.max(1, totalPages)}</strong> ({totalElements} total records)
          </div>

          <div className="pagination-controls">
            <button
              type="button"
              className="btn btn-secondary pagination-btn"
              disabled={page <= 0}
              onClick={() => onPageChange(page - 1)}
            >
              <ChevronLeft size={16} /> Previous
            </button>
            <button
              type="button"
              className="btn btn-secondary pagination-btn"
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
            >
              Next <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
