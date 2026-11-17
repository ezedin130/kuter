import React, { useState } from 'react';
import { Building2, User, KeyRound, Menu, Languages } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Modal } from './Modal';
import { Alert } from './Alert';
import { shopApi } from '../api/client';
import { useLanguage } from '../i18n/LanguageContext';

interface ShopTopbarProps {
  pageTitle: string;
  onToggleSidebar?: () => void;
}

export const ShopTopbar: React.FC<ShopTopbarProps> = ({ pageTitle, onToggleSidebar }) => {
  const { user, activeTenant, switchTenant } = useAuth();
  const { language, toggleLanguage, t } = useLanguage();

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
      setErrorMsg(t('topbar.currentPasswordRequired'));
      return;
    }

    if (!newPassword || newPassword.length < 6) {
      setErrorMsg(t('topbar.passwordMinLength'));
      return;
    }

    if (!newPassword !== !confirmPassword || newPassword !== confirmPassword) {
      setErrorMsg(t('topbar.passwordMismatch'));
      return;
    }

    setIsSubmitting(true);
    try {
      await shopApi.changePassword({
        currentPassword,
        newPassword,
      });
      setSuccessMsg(t('topbar.passwordChanged'));
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => {
        setIsChangePasswordOpen(false);
        setSuccessMsg(null);
      }, 2000);
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : (language === 'am' ? 'የይለፍ ቃል መቀየር አልተቻለም።' : 'Failed to change password.'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const resolveRoleLabel = (r?: string) => {
    if (!r) return '';
    if (language === 'am') {
      switch (r.toUpperCase()) {
        case 'OWNER':
          return t('topbar.roleOwner');
        case 'MANAGER':
          return t('topbar.roleManager');
        case 'CASHIER':
          return t('topbar.roleCashier');
        default:
          return t('topbar.roleStaff');
      }
    }
    return r;
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
              title={language === 'am' ? 'የአሰሳ ማውጫውን ክፈት' : 'Open Navigation Menu'}
              aria-label={language === 'am' ? 'የአሰሳ ማውጫውን ክፈት' : 'Open Navigation Menu'}
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
                title={t('topbar.shopSwitcher')}
              >
                {user.memberships.map((m) => (
                  <option key={m.tenantId} value={m.tenantId}>
                    {m.tenantName} ({resolveRoleLabel(m.role)})
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
              {resolveRoleLabel(activeTenant?.role)}
            </span>
          </div>

          <button
            type="button"
            className="btn-secondary btn-sm"
            onClick={handleOpenChangePassword}
            title={t('topbar.changePassword')}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <KeyRound size={14} color="var(--teal)" />
            <span>{t('topbar.changePassword')}</span>
          </button>

          {/* Localization Switcher Button (Replaces Logout Button on the Top Right) */}
          <button
            type="button"
            onClick={toggleLanguage}
            className="topbar-lang-btn"
            title={language === 'am' ? 'Switch interface to English' : 'የሱቅ በይነገጹን ወደ አማርኛ ቀይር'}
            aria-label={t('topbar.switchLanguage')}
          >
            <Languages size={15} className="lang-icon" />
            <span className="lang-label">{language === 'am' ? 'አማርኛ' : 'English'}</span>
            <span className="lang-code-pill">{language === 'am' ? 'AM' : 'EN'}</span>
          </button>
        </div>
      </header>

      {/* Change Password Modal */}
      <Modal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
        title={t('topbar.changePasswordTitle')}
      >
        <form onSubmit={handleChangePasswordSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {errorMsg && (
            <Alert type="error" message={errorMsg} onDismiss={() => setErrorMsg(null)} />
          )}
          {successMsg && (
            <Alert type="success" message={successMsg} onDismiss={() => setSuccessMsg(null)} />
          )}

          <div className="form-group">
            <label className="form-label">{t('topbar.currentPassword')}</label>
            <input
              type="password"
              className="form-input"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="••••••••"
              disabled={isSubmitting}
              autoFocus
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">{t('topbar.newPassword')}</label>
            <input
              type="password"
              className="form-input"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder={language === 'am' ? 'ቢያንስ 6 ፊደላት' : 'Min 6 characters'}
              disabled={isSubmitting}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">{t('topbar.confirmPassword')}</label>
            <input
              type="password"
              className="form-input"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="••••••••"
              disabled={isSubmitting}
              required
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsChangePasswordOpen(false)}
              disabled={isSubmitting}
            >
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isSubmitting}
            >
              {isSubmitting ? t('topbar.submittingPassword') : t('topbar.changePassword')}
            </button>
          </div>
        </form>
      </Modal>
    </>
  );
};
