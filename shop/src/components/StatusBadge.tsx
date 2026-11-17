import React from 'react';
import { useLanguage } from '../i18n/LanguageContext';

interface StatusBadgeProps {
  status: string;
  variant?: 'stock' | 'status' | 'sale';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, variant = 'status' }) => {
  const { t, language } = useLanguage();
  const norm = (status || '').toUpperCase();

  let className = 'badge badge-neutral';
  let label = norm;

  if (variant === 'stock') {
    if (norm === 'IN_STOCK' || norm === 'NORMAL') {
      className = 'badge badge-success';
      label = t('status.IN_STOCK');
    } else if (norm === 'LOW_STOCK' || norm === 'LOW') {
      className = 'badge badge-warning';
      label = t('status.LOW_STOCK');
    } else if (norm === 'OUT_OF_STOCK' || norm === 'EMPTY') {
      className = 'badge badge-danger';
      label = t('status.OUT_OF_STOCK');
    }
  } else if (variant === 'sale') {
    if (norm === 'COMPLETED') {
      className = 'badge badge-success';
      label = t('status.COMPLETED');
    } else if (norm === 'VOIDED') {
      className = 'badge badge-danger';
      label = t('status.VOIDED');
    }
  } else {
    if (norm === 'ACTIVE' || norm === 'COMPLETED' || norm === 'APPROVED') {
      className = 'badge badge-success';
      label = norm === 'COMPLETED' ? t('status.COMPLETED') : norm === 'APPROVED' ? t('status.APPROVED') : t('status.ACTIVE');
    } else if (norm === 'INACTIVE' || norm === 'SUSPENDED' || norm === 'VOIDED') {
      className = 'badge badge-danger';
      label = norm === 'VOIDED' ? t('status.VOIDED') : norm === 'SUSPENDED' ? t('status.SUSPENDED') : t('status.INACTIVE');
    } else if (norm === 'TRIALING' || norm === 'PENDING') {
      className = 'badge badge-warning';
      label = norm === 'TRIALING' ? t('status.TRIALING') : t('status.PENDING');
    } else if (norm === 'OWNER') {
      className = 'badge badge-teal';
      label = language === 'am' ? 'ባለቤት' : 'Owner';
    } else if (norm === 'MANAGER') {
      className = 'badge badge-neutral';
      label = language === 'am' ? 'ስራ አስኪያጅ' : 'Manager';
    } else if (norm === 'CASHIER' || norm === 'STOREKEEPER') {
      className = 'badge badge-neutral';
      label = norm === 'CASHIER'
        ? (language === 'am' ? 'ገንዘብ ተቀባይ' : 'Cashier')
        : (language === 'am' ? 'እቃ ጠባቂ' : 'Storekeeper');
    }
  }

  return <span className={className}>{label}</span>;
};
