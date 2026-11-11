import React from 'react';
import { Logo } from './Logo';
import { PublicPage } from './Header';

interface FooterProps {
  onNavigate: (page: PublicPage) => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate }) => {
  return (
    <footer className="footer">
      <div>
        <Logo light onClick={() => onNavigate('home')} />
        <p>
          Calm, controlled inventory for
          <br />
          building materials businesses.
        </p>
      </div>

      <div>
        <strong>Product</strong>
        <button onClick={() => onNavigate('features')}>Features</button>
        <button onClick={() => onNavigate('pricing')}>Pricing</button>
        <button onClick={() => onNavigate('status')}>Application status</button>
        <button onClick={() => onNavigate('register')}>Request access</button>
      </div>

      <div>
        <strong>Capabilities</strong>
        <button onClick={() => onNavigate('features')}>Dual-UOM conversion</button>
        <button onClick={() => onNavigate('features')}>Moving WAC costing</button>
        <button onClick={() => onNavigate('features')}>Stock ledger & audit</button>
        <button onClick={() => onNavigate('features')}>Counter sales deduction</button>
      </div>

      <div>
        <strong>Platform</strong>
        <button onClick={() => onNavigate('about')}>About Kuter</button>
        <button onClick={() => onNavigate('contact')}>Contact inquiry</button>
        <a
          href="http://localhost:5174"
          target="_blank"
          rel="noopener noreferrer"
        >
          Platform admin
        </a>
      </div>

      <div className="footer-bottom">
        <span>© 2026 Kuter. All rights reserved.</span>
        <span>Dedicated multi-tenant SaaS inventory control for building materials yards.</span>
      </div>
    </footer>
  );
};
