import React, { useState } from 'react';
import { Menu, X } from 'lucide-react';
import { Logo } from './Logo';
import { Button } from './Button';

export type PublicPage = 'home' | 'features' | 'pricing' | 'register' | 'status' | 'about' | 'contact';

interface HeaderProps {
  currentPage: PublicPage;
  onNavigate: (page: PublicPage) => void;
}

export const Header: React.FC<HeaderProps> = ({ currentPage, onNavigate }) => {
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleNav = (page: PublicPage) => {
    onNavigate(page);
    setMobileOpen(false);
  };

  return (
    <header className="public-header">
      <Logo onClick={() => handleNav('home')} />

      <nav className={`public-nav ${mobileOpen ? 'open' : ''}`} aria-label="Main navigation">
        <button
          className={currentPage === 'features' ? 'active' : ''}
          onClick={() => handleNav('features')}
        >
          Features
        </button>
        <button
          className={currentPage === 'home' ? 'active' : ''}
          onClick={() => {
            if (currentPage !== 'home') {
              handleNav('home');
            } else {
              document.getElementById('how-it-works')?.scrollIntoView({ behavior: 'smooth' });
            }
          }}
        >
          How it works
        </button>
        <button
          className={currentPage === 'pricing' ? 'active' : ''}
          onClick={() => handleNav('pricing')}
        >
          Pricing
        </button>
        <button
          className={currentPage === 'status' ? 'active' : ''}
          onClick={() => handleNav('status')}
        >
          Application status
        </button>
        <button
          className={currentPage === 'about' ? 'active' : ''}
          onClick={() => handleNav('about')}
        >
          About
        </button>
      </nav>

      <div className="header-actions">
        <a
          href="http://localhost:5173"
          className="text-link"
          target="_blank"
          rel="noopener noreferrer"
          title="Log in to your shop workspace"
        >
          Shop login
        </a>
        <Button onClick={() => handleNav('register')}>
          Request access
        </Button>
      </div>

      <button
        className="mobile-menu"
        onClick={() => setMobileOpen(!mobileOpen)}
        aria-label={mobileOpen ? 'Close navigation menu' : 'Open navigation menu'}
        aria-expanded={mobileOpen}
      >
        {mobileOpen ? <X size={24} /> : <Menu size={24} />}
      </button>
    </header>
  );
};
