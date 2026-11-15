import React, { useState } from 'react';
import { Building2, User, LogOut, KeyRound, Lock, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Modal } from './Modal';
import { Alert } from './Alert';
import { shopApi } from '../api/client';

interface ShopTopbarProps {
  pageTitle: string;
  onToggleSidebar?: () => void;
}

export const ShopTopbar: React.FC<ShopTopbarProps> = ({ pageTitle, onToggleSidebar }) => {
  const { user, activeTenant, switchTenant, logout } = useAuth();

  // Change Password State
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const handleOpenChangePassword = () => {
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setErrorMsg(null);
    setSuccessMsg(null);
    setIsChangePasswordOpen(true);
  };

  const handleChangePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    setSuccessMsg(null);

    if (!currentPassword) {
      setErrorMsg('Please enter your current password.');
      return;
    }

    if (!newPassword || newPassword.length < 6) {
      setErrorMsg('New password must be at least 6 characters.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMsg('New password and confirmation do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      await shopApi.changePassword({
        currentPassword,
        newPassword,
      });
      setSuccessMsg('Your password has been changed successfully. Use it next time you log in.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => {
        setIsChangePasswordOpen(false);
        setSuccessMsg(null);
      }, 2000);
    } catch (err: any) {
      setErrorMsg(err.message || 'Failed to change password. Please check your current password.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <header className="shop-topbar">
        <div className="topbar-left">
          {onToggleSidebar && (
            <button
              type="button"
              className="mobile-menu-btn"
              onClick={onToggleSidebar}
              title="Open Navigation Menu"
              aria-label="Open Navigation Menu"
            >
              <Menu size={20} />
            </button>
          )}
          <h2 className="topbar-title">{pageTitle}</h2>
        </div>

        <div className="topbar-right">
          {/* Multi-tenant Switcher if user has multiple memberships */}
          {user && user.memberships && user.memberships.length > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <Building2 size={16} color="var(--muted)" />
              <select
                className="select-filter"
                value={activeTenant?.tenantId}
                onChange={(e) => switchTenant(e.target.value)}
                style={{ height: '34px', fontSize: '12.5px', padding: '0 8px' }}
              >
                {user.memberships.map((m) => (
                  <option key={m.tenantId} value={m.tenantId}>
                    {m.tenantName} ({m.role})
                  </option>
                ))}
              </select>
            </div>
          )}

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              background: 'var(--canvas)',
              border: '1px solid var(--border)',
              borderRadius: '99px',
              padding: '4px 12px',
              fontSize: '12.5px',
              fontWeight: 600,
              color: 'var(--ink)',
            }}
          >
            <User size={14} color="var(--teal)" />
            <span>{user?.fullName}</span>
            <span
              style={{
                fontSize: '10.5px',
                background: 'var(--teal-soft)',
                color: 'var(--teal)',
                padding: '2px 6px',
                borderRadius: '99px',
                fontWeight: 700,
                textTransform: 'uppercase',
              }}
            >
              {activeTenant?.role}
            </span>
          </div>

          <button
            type="button"
            className="btn-secondary btn-sm"
            onClick={handleOpenChangePassword}
            title="Change Account Password"
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <KeyRound size={14} color="var(--teal)" />
            <span>Change Password</span>
          </button>

          <button
            type="button"
            className="btn-secondary btn-sm"
            onClick={logout}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <LogOut size={14} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Change Password Modal */}
      {isChangePasswordOpen && (
        <Modal
          isOpen={true}
          title="Change Account Password"
          onClose={() => setIsChangePasswordOpen(false)}
          footer={
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsChangePasswordOpen(false)}
                disabled={isSubmitting}
              >
                Cancel
              </button>
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleChangePasswordSubmit}
                disabled={isSubmitting || !currentPassword || !newPassword || newPassword.length < 6 || newPassword !== confirmPassword}
              >
                {isSubmitting ? 'Updating...' : 'Update Password'}
              </button>
            </div>
          }
        >
          <form onSubmit={handleChangePasswordSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {errorMsg && (
              <Alert type="error" message={errorMsg} onDismiss={() => setErrorMsg(null)} />
            )}
            {successMsg && (
              <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />
            )}

            <p style={{ fontSize: '13px', color: 'var(--muted)', margin: 0 }}>
              Update your account password for <strong>{user?.email}</strong>.
            </p>

            <div className="form-group">
              <label className="form-label">Current Password *</label>
              <div style={{ position: 'relative' }}>
                <Lock size={15} color="var(--muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="password"
                  className="form-input"
                  style={{ width: '100%', paddingLeft: '36px' }}
                  placeholder="Enter current password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">New Password *</label>
              <div style={{ position: 'relative' }}>
                <Lock size={15} color="var(--muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="password"
                  className="form-input"
                  style={{ width: '100%', paddingLeft: '36px' }}
                  placeholder="At least 6 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  minLength={6}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Confirm New Password *</label>
              <div style={{ position: 'relative' }}>
                <Lock size={15} color="var(--muted)" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
                <input
                  type="password"
                  className="form-input"
                  style={{ width: '100%', paddingLeft: '36px' }}
                  placeholder="Re-enter new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  autoComplete="new-password"
                  required
                  minLength={6}
                />
              </div>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
};
