import React from 'react';
import { Modal } from './Modal';
import { AlertTriangle, CheckCircle2, Info, AlertOctagon } from 'lucide-react';
import { useLanguage } from '../i18n/LanguageContext';

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
  confirmLabel,
  cancelLabel,
  tone = 'primary',
  isOpen,
  isLoading = false,
  onConfirm,
  onCancel,
}) => {
  const { t } = useLanguage();

  if (!isOpen) return null;

  const resolvedConfirmLabel = confirmLabel || t('common.confirm');
  const resolvedCancelLabel = cancelLabel || t('common.cancel');

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
      case 'info':
        return 'btn btn-primary';
      default:
        return 'btn btn-primary';
    }
  };

  return (
    <Modal title={title} onClose={onCancel} maxWidth="480px">
      <div style={{ display: 'flex', gap: '16px', alignItems: 'flex-start', padding: '8px 0' }}>
        <div style={{ flexShrink: 0, marginTop: '2px' }}>{getToneIcon()}</div>
        <div style={{ flex: 1 }}>
          <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.5, color: 'var(--ink)' }}>
            {message}
          </p>
          {detail && <div style={{ marginTop: '12px' }}>{detail}</div>}
        </div>
      </div>

      <div className="modal-actions" style={{ marginTop: '24px' }}>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={isLoading}
        >
          {resolvedCancelLabel}
        </button>
        <button
          type="button"
          className={getConfirmBtnClass()}
          onClick={onConfirm}
          disabled={isLoading}
        >
          {isLoading ? t('common.loading') : resolvedConfirmLabel}
        </button>
      </div>
    </Modal>
  );
};
