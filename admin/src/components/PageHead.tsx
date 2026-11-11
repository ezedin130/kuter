import React from 'react';

interface PageHeadProps {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export const PageHead: React.FC<PageHeadProps> = ({
  eyebrow,
  title,
  description,
  actions,
}) => {
  return (
    <div className="page-head">
      <div className="page-head-copy">
        {eyebrow && <span className="page-head-eyebrow">{eyebrow}</span>}
        <h1 className="page-head-title">{title}</h1>
        {description && <p className="page-head-description">{description}</p>}
      </div>
      {actions && <div className="page-head-actions">{actions}</div>}
    </div>
  );
};
