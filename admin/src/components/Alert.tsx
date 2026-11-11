import React from 'react';
import { CheckCircle2, AlertTriangle, AlertOctagon, Info, X } from 'lucide-react';

interface AlertProps {
  type: 'success' | 'warning' | 'danger' | 'info';
  title?: string;
  message: string;
  onDismiss?: () => void;
  style?: React.CSSProperties;
}

export const Alert: React.FC<AlertProps> = ({
  type,
  title,
  message,
  onDismiss,
  style,
}) => {
  const getIcon = () => {
    switch (type) {
      case 'success':
        return <CheckCircle2 size={18} style={{ color: 'var(--success)' }} />;
      case 'warning':
        return <AlertTriangle size={18} style={{ color: 'var(--warning)' }} />;
      case 'danger':
        return <AlertOctagon size={18} style={{ color: 'var(--danger)' }} />;
      case 'info':
      default:
        return <Info size={18} style={{ color: 'var(--blue)' }} />;
    }
  };

  const getAlertClass = () => {
    switch (type) {
      case 'success':
        return 'alert-success';
      case 'warning':
        return 'alert-warning';
      case 'danger':
        return 'alert-danger';
      case 'info':
      default:
        return 'alert-info';
    }
  };

  return (
    <div className={`alert-banner ${getAlertClass()}`} style={style} role="alert">
      <div className="alert-icon">{getIcon()}</div>
      <div className="alert-content">
        {title && <strong className="alert-title">{title}</strong>}
        <div className="alert-message">{message}</div>
      </div>
      {onDismiss && (
        <button
          type="button"
          className="alert-dismiss-btn"
          onClick={onDismiss}
          aria-label="Dismiss alert"
        >
          <X size={16} />
        </button>
      )}
    </div>
  );
};
