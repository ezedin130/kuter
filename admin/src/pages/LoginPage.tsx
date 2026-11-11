import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, LogIn, AlertCircle } from 'lucide-react';

export const LoginPage: React.FC = () => {
  const { login, error: authError } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (!email.trim()) {
      setLocalError('Please enter your administrator email address.');
      return;
    }
    if (!password) {
      setLocalError('Please enter your administrator password.');
      return;
    }

    setSubmitting(true);
    try {
      await login(email.trim(), password);
    } catch (err: unknown) {
      setLocalError(err instanceof Error ? err.message : 'Invalid credentials or login failed.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUseDemo = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setLocalError(null);
  };

  const displayError = localError || authError;

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'var(--ink)',
        padding: '24px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '420px',
          backgroundColor: 'white',
          borderRadius: 'var(--radius-lg)',
          boxShadow: 'var(--shadow-lg)',
          overflow: 'hidden',
          border: '1px solid rgba(255, 255, 255, 0.1)',
        }}
      >
        {/* Brand Banner */}
        <div
          style={{
            backgroundColor: '#0c2230',
            padding: '28px 24px',
            color: 'white',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
          }}
        >
          <div className="kuter-logo-mark">
            <span />
            <span />
            <span />
          </div>
          <div>
            <h1 style={{ fontSize: '20px', fontWeight: 800, margin: 0, color: 'white' }}>
              Kuter Admin
            </h1>
            <p style={{ margin: '2px 0 0 0', fontSize: '12px', color: '#9bb3c2' }}>
              SaaS Platform Administration Console
            </p>
          </div>
        </div>

        {/* Login Form Body */}
        <div style={{ padding: '28px 24px' }}>
          <div style={{ marginBottom: '20px' }}>
            <h2 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--ink)', margin: 0 }}>
              Sign In to Platform Admin
            </h2>
            <p style={{ fontSize: '13px', color: 'var(--muted)', margin: '4px 0 0 0' }}>
              Restricted to authorized Kuter platform operators and administrators.
            </p>
          </div>

          {displayError && (
            <div
              style={{
                padding: '12px 14px',
                borderRadius: 'var(--radius-sm)',
                backgroundColor: 'var(--danger-bg)',
                border: '1px solid #f8c9c4',
                color: 'var(--danger)',
                fontSize: '13px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '10px',
                marginBottom: '18px',
              }}
            >
              <AlertCircle size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>{displayError}</div>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="admin-email">
                Email Address <span className="required">*</span>
              </label>
              <input
                id="admin-email"
                type="email"
                className="form-input"
                placeholder="admin@kuterplatform.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                autoFocus
                disabled={submitting}
              />
            </div>

            <div className="form-group" style={{ marginBottom: '22px' }}>
              <label className="form-label" htmlFor="admin-password">
                Password <span className="required">*</span>
              </label>
              <input
                id="admin-password"
                type="password"
                className="form-input"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                disabled={submitting}
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', minHeight: '42px', fontSize: '14.5px' }}
              disabled={submitting}
            >
              {submitting ? (
                <>Authenticating...</>
              ) : (
                <>
                  <LogIn size={16} /> Sign In
                </>
              )}
            </button>
          </form>

          {/* Quick Demo Credentials Assistant */}
          <div
            style={{
              marginTop: '24px',
              paddingTop: '18px',
              borderTop: '1px solid var(--border)',
              fontSize: '12px',
              color: 'var(--muted)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                fontWeight: 600,
                color: 'var(--ink)',
                marginBottom: '8px',
              }}
            >
              <ShieldCheck size={14} style={{ color: 'var(--teal)' }} />
              <span>Platform Admin Demo Credentials:</span>
            </div>
            <p style={{ margin: '0 0 8px 0', lineHeight: 1.4 }}>
              Click to autofill seeded credentials from the local verification suite:
            </p>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                style={{ fontSize: '11.5px', padding: '0 8px', minHeight: '28px' }}
                onClick={() => handleUseDemo('admin@kuterplatform.com', 'Admin@123456')}
              >
                Super Admin
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                style={{ fontSize: '11.5px', padding: '0 8px', minHeight: '28px' }}
                onClick={() => handleUseDemo('operator@kuterplatform.com', 'Admin@123456')}
              >
                Platform Operator
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
