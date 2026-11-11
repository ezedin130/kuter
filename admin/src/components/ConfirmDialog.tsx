import React from 'react';
import { Modal } from './Modal';
import { AlertTriangle, CheckCircle2, Info, AlertOctagon } from 'lucide-react';

interface ConfirmDialogProps {
  title: string;
  message: string;
  detail?: React.ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: 'primary' | 'danger' | 'warning' | 'info';
  isOpen: boolean;
  isLoading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  title,
  message,
  detail,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'primary',
  isOpen,
  isLoading = false,
  onConfirm,
  onCancel,
}) => {
  if (!isOpen) return null;

  const getToneIcon = () => {
    switch (tone) {
      case 'danger':
        return <AlertOctagon size={22} style={{ color: 'var(--danger)' }} />;
      case 'warning':
        return <AlertTriangle size={22} style={{ color: 'var(--warning)' }} />;
      case 'info':
        return <Info size={22} style={{ color: 'var(--blue)' }} />;
      default:
        return <CheckCircle2 size={22} style={{ color: 'var(--teal)' }} />;
    }
  };

  const getConfirmBtnClass = () => {
    switch (tone) {
      case 'danger':
        return 'btn btn-danger';
      case 'warning':
        return 'btn btn-warning';
      default:
        return 'btn btn-primary';
    }
  };

  return (
    <Modal title={title} onClose={onCancel} maxWidth="480px">
      <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', marginBottom: '20px' }}>
        <div style={{ flexShrink: 0, marginTop: '2px' }}>{getToneIcon()}</div>
        <div style={{ flex: 1 }}>
          <p style={{ margin: 0, color: 'var(--text)', fontSize: '14px', lineHeight: 1.5 }}>
            {message}
          </p>
          {detail && <div style={{ marginTop: '12px' }}>{detail}</div>}
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '24px' }}>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={isLoading}
        >
          {cancelLabel}
        </button>
        <button
          type="button"
          className={getConfirmBtnClass()}
          onClick={onConfirm}
          disabled={isLoading}
        >
          {isLoading ? 'Processing...' : confirmLabel}
        </button>
      </div>
    </Modal>
  );
};
