import React from 'react';
import { useLanguage } from '../i18n/LanguageContext';

export type BadgeTone = 'success' | 'warning' | 'danger' | 'neutral' | 'blue';

interface StatusBadgeProps {
  status?: string;
  label?: string;
  tone?: BadgeTone;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  status,
  label,
  tone,
  size = 'md',
}) => {
  const { translateStatus } = useLanguage();

  // Infer tone from status string if tone not explicitly supplied
  const resolveTone = (): BadgeTone => {
    if (tone) return tone;
    if (!status) return 'neutral';

    const normalized = status.toUpperCase();
    switch (normalized) {
      case 'ACTIVE':
      case 'APPROVED':
      case 'SUCCESS':
      case 'UP':
      case 'CONNECTED':
        return 'success';

      case 'PENDING':
      case 'TRIAL':
      case 'WARNING':
      case 'PAST_DUE':
      case 'MORE INFO':
        return 'warning';

      case 'TRIALING':
      case 'INFO':
        return 'blue';

      case 'SUSPENDED':
      case 'REJECTED':
      case 'FAILED':
      case 'FAILURE':
      case 'DISCONNECTED':
      case 'DOWN':
      case 'ERROR':
        return 'danger';

      case 'EXPIRED':
        return 'warning';

      case 'INACTIVE':
      case 'CANCELLED':
      default:
        return 'neutral';
    }
  };

  const badgeTone = resolveTone();
  const displayLabel = label || (status ? translateStatus(status) : '—');

  const toneClass = {
    success: 'badge-success',
    warning: 'badge-warning',
    danger: 'badge-danger',
    neutral: 'badge-neutral',
    blue: 'badge-blue',
  }[badgeTone];

  return (
    <span className={`badge ${toneClass} ${size === 'sm' ? 'badge-sm' : ''}`}>
      <span className="badge-dot" />
      <span>{displayLabel}</span>
    </span>
  );
};
