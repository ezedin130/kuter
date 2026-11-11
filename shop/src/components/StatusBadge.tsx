import React from 'react';

interface StatusBadgeProps {
  status: string;
  variant?: 'stock' | 'status' | 'sale';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, variant = 'status' }) => {
  const norm = (status || '').toUpperCase();

  let className = 'badge badge-neutral';
  let label = norm;

  if (variant === 'stock') {
    if (norm === 'IN_STOCK' || norm === 'NORMAL') {
      className = 'badge badge-success';
      label = 'In Stock';
    } else if (norm === 'LOW_STOCK' || norm === 'LOW') {
      className = 'badge badge-warning';
      label = 'Low Stock';
    } else if (norm === 'OUT_OF_STOCK' || norm === 'EMPTY') {
      className = 'badge badge-danger';
      label = 'Out of Stock';
    }
  } else if (variant === 'sale') {
    if (norm === 'COMPLETED') {
      className = 'badge badge-success';
      label = 'Completed';
    } else if (norm === 'VOIDED') {
      className = 'badge badge-danger';
      label = 'Voided';
    }
  } else {
    if (norm === 'ACTIVE' || norm === 'COMPLETED' || norm === 'APPROVED') {
      className = 'badge badge-success';
    } else if (norm === 'INACTIVE' || norm === 'SUSPENDED' || norm === 'VOIDED') {
      className = 'badge badge-danger';
    } else if (norm === 'TRIALING' || norm === 'PENDING') {
      className = 'badge badge-warning';
    } else if (norm === 'OWNER') {
      className = 'badge badge-teal';
    } else if (norm === 'MANAGER') {
      className = 'badge badge-neutral';
    } else if (norm === 'CASHIER' || norm === 'STOREKEEPER') {
      className = 'badge badge-neutral';
    }
  }

  return <span className={className}>{label}</span>;
};
