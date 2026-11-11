import React from 'react';

interface PageHeadProps {
  eyebrow?: string;
  title: string;
  subtitle?: string;
  actions?: React.ReactNode;
}

export const PageHead: React.FC<PageHeadProps> = ({
  eyebrow,
  title,
  subtitle,
  actions,
}) => {
  return (
    <div className="page-head">
      <div className="page-head-copy">
        {eyebrow && <span className="page-head-eyebrow">{eyebrow}</span>}
        <h1 className="page-head-title">{title}</h1>
        {subtitle && <p className="page-head-subtitle">{subtitle}</p>}
      </div>
      {actions && <div className="page-head-actions">{actions}</div>}
    </div>
  );
};
