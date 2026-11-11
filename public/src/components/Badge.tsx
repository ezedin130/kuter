import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  tone?: 'default' | 'success' | 'warning' | 'danger' | 'info';
  status?: string;
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  tone,
  status,
  className = '',
}) => {
  let badgeClass = 'badge';

  if (status) {
    const s = status.toUpperCase();
    if (s === 'APPROVED' || s === 'ACTIVE' || s === 'IN STOCK') {
      badgeClass += ' badge-approved';
    } else if (s === 'PENDING' || s === 'LOW STOCK' || s === 'TRIALING') {
      badgeClass += ' badge-pending';
    } else if (s === 'REJECTED' || s === 'OUT OF STOCK' || s === 'SUSPENDED') {
      badgeClass += ' badge-rejected';
    } else if (s === 'MORE_INFO_REQUESTED' || s === 'MORE INFO') {
      badgeClass += ' badge-more-info';
    }
  } else if (tone) {
    if (tone === 'warning') badgeClass += ' badge-pending';
    else if (tone === 'danger') badgeClass += ' badge-rejected';
    else if (tone === 'success') badgeClass += ' badge-approved';
  }

  return (
    <span className={`${badgeClass} ${className}`}>
      <span className="badge-dot" aria-hidden="true" />
      {children}
    </span>
  );
};
