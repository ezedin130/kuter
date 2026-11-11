import React from 'react';
import { AlertCircle, CheckCircle, Info, AlertTriangle, X } from 'lucide-react';

interface AlertProps {
  type?: 'success' | 'error' | 'warning' | 'info';
  message: string;
  onDismiss?: () => void;
}

export const Alert: React.FC<AlertProps> = ({ type = 'info', message, onDismiss }) => {
  if (!message) return null;

  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle size={18} color="var(--success)" />;
      case 'error':
        return <AlertCircle size={18} color="var(--danger)" />;
      case 'warning':
        return <AlertTriangle size={18} color="var(--warning)" />;
      default:
        return <Info size={18} color="var(--blue)" />;
    }
  };

  const getBgStyle = () => {
    switch (type) {
      case 'success':
        return { background: 'var(--success-bg)', border: '1px solid rgba(22, 114, 77, 0.2)', color: 'var(--success)' };
      case 'error':
        return { background: 'var(--danger-bg)', border: '1px solid rgba(184, 58, 58, 0.2)', color: 'var(--danger)' };
      case 'warning':
        return { background: 'var(--warning-bg)', border: '1px solid rgba(167, 91, 9, 0.2)', color: 'var(--warning)' };
      default:
        return { background: '#edf5f9', border: '1px solid rgba(53, 106, 139, 0.2)', color: 'var(--blue)' };
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '12px 16px',
        borderRadius: 'var(--radius-sm)',
        marginBottom: '16px',
        fontSize: '13.5px',
        fontWeight: 600,
        ...getBgStyle(),
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
        {getIcon()}
        <span>{message}</span>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', display: 'flex' }}
          aria-label="Dismiss alert"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
};
