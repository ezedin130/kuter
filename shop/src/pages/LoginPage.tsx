import React, { useState } from 'react';
import { Lock, Mail, ArrowRight, KeyRound, Languages } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Alert } from '../components/Alert';
import { useLanguage } from '../i18n/LanguageContext';

export const LoginPage: React.FC = () => {
  const { login, isLoading, error } = useAuth();
  const { language, toggleLanguage, t } = useLanguage();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);
    if (!email.trim() || !password) {
      setLocalError(t('login.emailRequired'));
      return;
    }
    try {
      await login(email.trim(), password);
    } catch (err: unknown) {
      setLocalError(err instanceof Error ? err.message : t('login.authFailed'));
    }
  };

  const handleDemoFill = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setLocalError(null);
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--canvas)',
        padding: '24px',
        position: 'relative',
      }}
    >
      {/* Top right language switch for shop login screen */}
      <div style={{ position: 'absolute', top: '20px', right: '20px' }}>
        <button
          type="button"
          onClick={toggleLanguage}
          className="topbar-lang-btn"
          title={language === 'am' ? 'Switch interface to English' : 'የሱቅ በይነገጹን ወደ አማርኛ ቀይር'}
        >
          <Languages size={15} className="lang-icon" />
          <span className="lang-label">{language === 'am' ? 'አማርኛ' : 'English'}</span>
          <span className="lang-code-pill">{language === 'am' ? 'AM' : 'EN'}</span>
        </button>
      </div>

      <div
        style={{
          width: '100%',
          maxWidth: '440px',
          background: 'white',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)',
          boxShadow: 'var(--shadow-md)',
          padding: '36px',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <div
            style={{
              width: '48px',
              height: '48px',
              background: 'var(--teal)',
              color: 'white',
              borderRadius: '12px',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '22px',
              marginBottom: '12px',
            }}
          >
            K
          </div>
          <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--ink)' }}>
            {t('login.title')}
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--muted)', marginTop: '4px' }}>
            {t('login.subtitle')}
          </p>
        </div>

        {(localError || error) && (
          <Alert
            type="error"
            message={localError || error || ''}
            onDismiss={() => setLocalError(null)}
          />
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div className="form-group">
            <label className="form-label">{t('login.workEmail')}</label>
            <div style={{ position: 'relative' }}>
              <Mail
                size={16}
                color="var(--muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
              <input
                type="email"
                className="form-input"
                style={{ width: '100%', paddingLeft: '38px' }}
                placeholder="staff@materials-shop.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">{t('login.password')}</label>
            <div style={{ position: 'relative' }}>
              <Lock
                size={16}
                color="var(--muted)"
                style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}
              />
              <input
                type="password"
                className="form-input"
                style={{ width: '100%', paddingLeft: '38px' }}
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', minHeight: '44px', marginTop: '8px', fontSize: '15px' }}
            disabled={isLoading}
          >
            {isLoading ? (
              <span>{t('login.authenticating')}</span>
            ) : (
              <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span>{t('login.signIn')}</span>
                <ArrowRight size={16} />
              </span>
            )}
          </button>
        </form>

        <div
          style={{
            marginTop: '28px',
            paddingTop: '20px',
            borderTop: '1px solid var(--border)',
            fontSize: '12.5px',
            color: 'var(--muted)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px', fontWeight: 600, color: 'var(--ink)' }}>
            <KeyRound size={14} color="var(--teal)" />
            <span>{t('login.demoTitle')}</span>
          </div>
          <p style={{ margin: '0 0 10px 0', fontSize: '11.5px', lineHeight: 1.4 }}>
            {t('login.demoHint')}
          </p>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '12px', padding: '0 10px', height: '30px' }}
              onClick={() => handleDemoFill('owner@materials-yard.com', 'Admin@123456')}
            >
              {t('login.ownerBtn')}
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ fontSize: '12px', padding: '0 10px', height: '30px' }}
              onClick={() => handleDemoFill('cashier@materials-yard.com', 'Admin@123456')}
            >
              {t('login.cashierBtn')}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
