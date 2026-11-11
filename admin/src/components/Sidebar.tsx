import React from 'react';
import {
  LayoutDashboard,
  ClipboardCheck,
  Building2,
  Package,
  Layers,
  ShieldCheck,
  LogOut,
  User,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export type AdminTab =
  | 'overview'
  | 'requests'
  | 'tenants'
  | 'plans'
  | 'subscriptions'
  | 'audit';

interface SidebarProps {
  currentTab: AdminTab;
  onSelectTab: (tab: AdminTab) => void;
  pendingCount?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  pendingCount = 0,
}) => {
  const { user, logout } = useAuth();

  const navItems: { id: AdminTab; label: string; icon: React.ReactNode; badge?: number }[] = [
    { id: 'overview', label: 'Platform Overview', icon: <LayoutDashboard size={18} /> },
    {
      id: 'requests',
      label: 'Registration Requests',
      icon: <ClipboardCheck size={18} />,
      badge: pendingCount > 0 ? pendingCount : undefined,
    },
    { id: 'tenants', label: 'Shops & Tenants', icon: <Building2 size={18} /> },
    { id: 'plans', label: 'Subscription Plans', icon: <Package size={18} /> },
    { id: 'subscriptions', label: 'Tenant Subscriptions', icon: <Layers size={18} /> },
    { id: 'audit', label: 'Platform Audit Logs', icon: <ShieldCheck size={18} /> },
  ];

  return (
    <aside className="admin-sidebar">
      {/* Brand Logo Header */}
      <div className="sidebar-header">
        <div className="kuter-logo">
          <div className="kuter-logo-mark">
            <span />
            <span />
            <span />
          </div>
          <div>
            <div style={{ lineHeight: 1.1 }}>Kuter</div>
            <div style={{ fontSize: '11px', fontWeight: 500, opacity: 0.7, letterSpacing: '0.5px' }}>
              Platform Admin
            </div>
          </div>
        </div>
      </div>

      {/* Main Navigation */}
      <nav className="sidebar-nav">
        {navItems.map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelectTab(item.id)}
            className={`sidebar-link ${currentTab === item.id ? 'active' : ''}`}
          >
            <span className="sidebar-icon">{item.icon}</span>
            <span className="sidebar-label">{item.label}</span>
            {item.badge !== undefined && (
              <span className="sidebar-counter-badge">{item.badge}</span>
            )}
          </button>
        ))}
      </nav>

      {/* Admin User Footer with Logout */}
      <div className="sidebar-footer">
        <div className="sidebar-user-info">
          <div className="user-avatar-circle">
            <User size={16} />
          </div>
          <div className="user-text-meta">
            <div className="user-display-name">{user?.fullName || 'Super Administrator'}</div>
            <div className="user-email-text" title={user?.email || 'admin@kuter.local'}>
              {user?.email || 'admin@kuter.local'}
            </div>
          </div>
        </div>

        <button
          type="button"
          className="btn-sidebar-logout"
          onClick={() => logout()}
          title="Sign out of administration portal"
        >
          <LogOut size={16} />
          <span>Sign Out</span>
        </button>
      </div>
    </aside>
  );
};
