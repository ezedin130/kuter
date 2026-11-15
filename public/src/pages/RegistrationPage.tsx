import React, { useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  Check,
  CheckCircle2,
  Clock3,
  LockKeyhole,
  ShieldCheck,
} from 'lucide-react';
import { Button } from '../components/Button';
import { Logo } from '../components/Logo';
import { PublicPage } from '../components/Header';
import { CreateOnboardingRequestDto, OnboardingPublicResponse } from '../types/api';
import { publicApi } from '../api/client';

interface RegistrationPageProps {
  initialPlan?: string;
  onNavigate: (page: PublicPage) => void;
}

export function normalizeEthiopianPhone(raw: string): { normalized: string; valid: boolean; error?: string } {
  if (!raw || !raw.trim()) {
    return { normalized: '', valid: true };
  }
  let cleaned = raw.trim().replace(/[\s\-\(\)\.]/g, '');
  if (cleaned.startsWith('+')) {
    cleaned = cleaned.substring(1);
  }
  if (cleaned.startsWith('251')) {
    cleaned = cleaned.substring(3);
  }
  if (cleaned.startsWith('0')) {
    cleaned = cleaned.substring(1);
  }
  if (!/^[79]\d{8}$/.test(cleaned)) {
    return {
      normalized: '',
      valid: false,
      error: 'Invalid Ethiopian phone number. Must be 9 digits starting with 9 or 7 (e.g. 982112233)',
    };
  }
  return {
    normalized: `+251${cleaned}`,
    valid: true,
  };
}

export const RegistrationPage: React.FC<RegistrationPageProps> = ({
  initialPlan = 'growth',
  onNavigate,
}) => {
  const [step, setStep] = useState(1);
  const [copied, setCopied] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);
  const [submittedResponse, setSubmittedResponse] = useState<OnboardingPublicResponse | null>(null);

  // Form State
  const [formData, setFormData] = useState<CreateOnboardingRequestDto>({
    shopName: '',
    preferredShopCode: '',
    businessAddress: '',
    currency: 'ETB',
    ownerFullName: '',
    ownerEmail: '',
    ownerPhone: '',
    requestedPlanCode: initialPlan,
    applicantNotes: '',
  });

  const [phoneLocal, setPhoneLocal] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [consentTerms, setConsentTerms] = useState(true);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const validateStep1 = () => {
    const errors: Record<string, string> = {};
    if (!formData.shopName.trim()) {
      errors.shopName = 'Shop name is required';
    } else if (formData.shopName.trim().length < 2) {
      errors.shopName = 'Shop name must be at least 2 characters';
    }

    if (formData.preferredShopCode && formData.preferredShopCode.trim()) {
      const code = formData.preferredShopCode.trim();
      if (!/^[a-z0-9-]{3,20}$/.test(code)) {
        errors.preferredShopCode = 'Shop code must be 3-20 characters using lowercase letters, numbers, and hyphens only.';
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateStep2 = () => {
    const errors: Record<string, string> = {};
    if (!formData.ownerFullName.trim()) {
      errors.ownerFullName = 'Account owner full legal name is required';
    } else if (formData.ownerFullName.trim().length < 2) {
      errors.ownerFullName = 'Name must be at least 2 characters';
    }

    if (!formData.ownerEmail.trim()) {
      errors.ownerEmail = 'Work email address is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.ownerEmail.trim())) {
      errors.ownerEmail = 'Please provide a valid email address';
    }

    if (phoneLocal.trim()) {
      const phoneRes = normalizeEthiopianPhone(phoneLocal);
      if (!phoneRes.valid) {
        errors.ownerPhone = phoneRes.error || 'Invalid Ethiopian phone format';
      }
    }

    if (!password) {
      errors.password = 'Account password is required';
    } else if (password.length < 8) {
      errors.password = 'Password must be at least 8 characters long';
    }

    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const validateStep3 = () => {
    const errors: Record<string, string> = {};
    if (!formData.requestedPlanCode) {
      errors.requestedPlanCode = 'Please select a preferred subscription plan';
    }
    if (!consentTerms) {
      errors.consent = 'You must agree to the Terms of Service to submit an application';
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleNext = () => {
    setErrorBanner(null);
    if (step === 1 && validateStep1()) {
      setStep(2);
    } else if (step === 2 && validateStep2()) {
      setStep(3);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateStep3()) return;

    setSubmitting(true);
    setErrorBanner(null);

    // Compute canonical normalized phone
    let normalizedPhone: string | undefined = undefined;
    if (phoneLocal.trim()) {
      const phoneCheck = normalizeEthiopianPhone(phoneLocal);
      if (!phoneCheck.valid) {
        setFieldErrors((prev) => ({ ...prev, ownerPhone: phoneCheck.error || 'Invalid phone' }));
        setSubmitting(false);
        setStep(2);
        return;
      }
      normalizedPhone = phoneCheck.normalized;
    }

    try {
      const payload: CreateOnboardingRequestDto = {
        shopName: formData.shopName.trim(),
        ownerFullName: formData.ownerFullName.trim(),
        ownerEmail: formData.ownerEmail.trim().toLowerCase(),
        ownerPhone: normalizedPhone,
        requestedPlanCode: formData.requestedPlanCode,
        preferredShopCode: formData.preferredShopCode?.trim() || undefined,
        businessAddress: formData.businessAddress?.trim() || undefined,
        currency: 'ETB',
        applicantNotes: formData.applicantNotes?.trim() || undefined,
        password: password.trim(),
      };

      const response = await publicApi.submitOnboardingRequest(payload);
      setSubmittedResponse(response);
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'fieldErrors' in err && (err as any).fieldErrors) {
        const serverFieldErrors = (err as any).fieldErrors as Record<string, string>;
        setFieldErrors(serverFieldErrors);
        // If errors belong to step 1 or 2, take user there
        if (serverFieldErrors.shopName || serverFieldErrors.preferredShopCode) {
          setStep(1);
        } else if (serverFieldErrors.ownerFullName || serverFieldErrors.ownerEmail || serverFieldErrors.ownerPhone || serverFieldErrors.password) {
          setStep(2);
        }
      }
      let msg = err instanceof Error ? err.message : 'Failed to submit registration request';
      // Translate raw HTTP status codes into user-friendly messages
      const status = (err as any)?.status as number | undefined;
      if (msg.includes('403') || status === 403) {
        msg = 'Registration is currently unavailable. This is a server configuration issue — please try again shortly or contact support.';
      } else if (msg.includes('404') || status === 404) {
        msg = 'The registration endpoint could not be reached. Please try again later.';
      } else if (msg.includes('500') || status === 500 || (msg.includes('HTTP 5'))) {
        msg = 'An unexpected server error occurred. Please try again shortly.';
      } else if (msg.toLowerCase().includes('network') || msg.toLowerCase().includes('unable to connect') || msg.toLowerCase().includes('failed to fetch')) {
        msg = 'Unable to reach the Kuter server. Please check your internet connection and try again.';
      }
      setErrorBanner(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const copyReference = () => {
    if (submittedResponse?.referenceCode) {
      navigator.clipboard?.writeText(submittedResponse.referenceCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    }
  };

  // SUCCESS CONFIRMATION VIEW
  if (submittedResponse) {
    return (
      <div className="auth-page">
        <div className="auth-brand">
          <Logo light onClick={() => onNavigate('home')} />
          <button onClick={() => onNavigate('home')}>Back to website</button>
        </div>

        <div className="success-panel">
          <div className="success-icon" aria-hidden="true">
            <Check />
          </div>
          <div className="eyebrow">Request received</div>
          <h1>Your shop registration request has been submitted.</h1>
          <p>
            We have received your application for <strong>{submittedResponse.shopName}</strong>.
            A confirmation email has been dispatched to <strong>{submittedResponse.ownerEmail}</strong>.
            Our platform administration team reviews every request within 24 business hours.
            Keep your reference code safe to monitor progress.
          </p>

          <div className="reference-box">
            <span>Request reference code</span>
            <strong>{submittedResponse.referenceCode}</strong>
            <button type="button" onClick={copyReference}>
              {copied ? 'Copied!' : 'Copy reference'}
            </button>
          </div>

          <div className="timeline" aria-label="Application timeline">
            <div className="done">
              <CheckCircle2 />
              <span>1. Submitted</span>
            </div>
            <span aria-hidden="true" />
            <div className="done">
              <Clock3 />
              <span>2. Admin review</span>
            </div>
            <span aria-hidden="true" />
            <div>
              <LockKeyhole />
              <span>3. Setup invite</span>
            </div>
          </div>

          <Button onClick={() => onNavigate('status')}>
            Check application status
          </Button>
          <button className="text-link" onClick={() => onNavigate('home')}>
            Return to homepage
          </button>
        </div>
      </div>
    );
  }

  // 3-STEP WIZARD VIEW
  return (
    <div className="registration-page">
      {/* Left Aside */}
      <aside className="registration-aside">
        <Logo light onClick={() => onNavigate('home')} />
        <div>
          <span className="aside-step">STEP {step} OF 3</span>
          <h2>
            {step === 1
              ? 'Tell us about your materials shop.'
              : step === 2
              ? 'Who will own the master account?'
              : 'Choose how your yard will start.'}
          </h2>
          <p>
            Your request is reviewed by a human platform administrator before tenant
            databases and inventory accounts are provisioned.
          </p>
        </div>
        <div className="privacy-note">
          <ShieldCheck />
          <span>
            <strong>Your data stays protected.</strong> A public reference code alone
            cannot reveal private inventory data or shop credentials.
          </span>
        </div>
      </aside>

      {/* Main Form Area */}
      <main className="registration-main">
        <button
          className="back-link"
          type="button"
          onClick={() => (step > 1 ? setStep(step - 1) : onNavigate('home'))}
        >
          ← {step > 1 ? 'Previous step' : 'Return to website'}
        </button>

        <div className="form-wrap">
          {/* Progress Indicators */}
          <div className="form-progress" aria-label="Form progress">
            <span className={step >= 1 ? 'active' : ''}>1. Shop details</span>
            <i aria-hidden="true" />
            <span className={step >= 2 ? 'active' : ''}>2. Account owner</span>
            <i aria-hidden="true" />
            <span className={step >= 3 ? 'active' : ''}>3. Plan & consent</span>
          </div>

          {errorBanner && (
            <div className="error-banner" role="alert" style={{ marginBottom: '20px' }}>
              <AlertCircle size={20} />
              <div>{errorBanner}</div>
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {/* STEP 1: SHOP DETAILS */}
            {step === 1 && (
              <>
                <h1>Shop information</h1>
                <p className="form-intro">
                  Tell us about the building materials business you want to manage with Kuter. All shops operate in Ethiopian Birr (ETB).
                </p>
                <div className="field-grid">
                  <label className="field wide">
                    <span>
                      Shop name <b>*</b>
                    </span>
                    <input
                      type="text"
                      placeholder="e.g. Merkato Steel & Cement Supply"
                      value={formData.shopName}
                      onChange={(e) =>
                        setFormData({ ...formData, shopName: e.target.value })
                      }
                      autoFocus
                    />
                    {fieldErrors.shopName && (
                      <span className="field-error">{fieldErrors.shopName}</span>
                    )}
                  </label>

                  <label className="field wide">
                    <span>Preferred shop identifier</span>
                    <input
                      type="text"
                      placeholder="e.g. merkato-supply (3-20 characters)"
                      value={formData.preferredShopCode}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          preferredShopCode: e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''),
                        })
                      }
                    />
                    {fieldErrors.preferredShopCode && (
                      <span className="field-error">{fieldErrors.preferredShopCode}</span>
                    )}
                    <small style={{ color: 'var(--muted)', fontSize: 11 }}>
                      Used for shop URLs and portal access. Letters, numbers, and hyphens only.
                    </small>
                  </label>

                  <label className="field wide">
                    <span>Business / Yard address</span>
                    <input
                      type="text"
                      placeholder="Street address, sub-city, Addis Ababa / regional city"
                      value={formData.businessAddress}
                      onChange={(e) =>
                        setFormData({ ...formData, businessAddress: e.target.value })
                      }
                    />
                  </label>
                </div>

                <div className="form-actions">
                  <Button type="button" onClick={handleNext}>
                    Continue to Account Owner <ArrowRight size={16} />
                  </Button>
                </div>
              </>
            )}

            {/* STEP 2: ACCOUNT OWNER */}
            {step === 2 && (
              <>
                <h1>Account owner</h1>
                <p className="form-intro">
                  This person will receive the approval decision and the initial shop setup invitation.
                </p>
                <div className="field-grid">
                  <label className="field wide">
                    <span>
                      Full legal name <b>*</b>
                    </span>
                    <input
                      type="text"
                      placeholder="e.g. Abebe Bekele"
                      value={formData.ownerFullName}
                      onChange={(e) =>
                        setFormData({ ...formData, ownerFullName: e.target.value })
                      }
                      autoFocus
                    />
                    {fieldErrors.ownerFullName && (
                      <span className="field-error">{fieldErrors.ownerFullName}</span>
                    )}
                  </label>

                  <label className="field wide">
                    <span>
                      Business work email <b>*</b>
                    </span>
                    <input
                      type="email"
                      placeholder="owner@shopdomain.com"
                      value={formData.ownerEmail}
                      onChange={(e) =>
                        setFormData({ ...formData, ownerEmail: e.target.value })
                      }
                    />
                    {fieldErrors.ownerEmail && (
                      <span className="field-error">{fieldErrors.ownerEmail}</span>
                    )}
                  </label>

                  <label className="field wide">
                    <span>Phone number (Ethiopia)</span>
                    <div style={{ display: 'flex', alignItems: 'center', marginTop: '6px' }}>
                      <span
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          padding: '0 12px',
                          height: '42px',
                          background: 'var(--bg-subtle, #f1f5f9)',
                          border: '1px solid var(--border, #cbd5e1)',
                          borderRight: 'none',
                          borderTopLeftRadius: '6px',
                          borderBottomLeftRadius: '6px',
                          fontWeight: 700,
                          color: 'var(--ink, #0f172a)',
                          fontSize: '14px',
                          userSelect: 'none',
                        }}
                      >
                        +251
                      </span>
                      <input
                        type="tel"
                        style={{
                          borderTopLeftRadius: 0,
                          borderBottomLeftRadius: 0,
                          flex: 1,
                        }}
                        placeholder="982112233"
                        value={phoneLocal}
                        onChange={(e) => {
                          let val = e.target.value;
                          val = val.replace(/^\+?251/, '');
                          setPhoneLocal(val);
                        }}
                      />
                    </div>
                    {fieldErrors.ownerPhone && (
                      <span className="field-error">{fieldErrors.ownerPhone}</span>
                    )}
                    <small style={{ color: 'var(--muted)', fontSize: 11, marginTop: '4px', display: 'block' }}>
                      Supported formats: 982112233 or 098 211 2233. Canonical format: +2519XXXXXXXX.
                    </small>
                  </label>

                  <label className="field wide">
                    <span>
                      Account master password <b>*</b>
                    </span>
                    <input
                      type="password"
                      placeholder="Minimum 8 characters"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                    />
                    {fieldErrors.password && (
                      <span className="field-error">{fieldErrors.password}</span>
                    )}
                  </label>

                  <label className="field wide">
                    <span>
                      Confirm account password <b>*</b>
                    </span>
                    <input
                      type="password"
                      placeholder="Re-enter password to confirm"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                    />
                    {fieldErrors.confirmPassword && (
                      <span className="field-error">{fieldErrors.confirmPassword}</span>
                    )}
                  </label>

                  <div className="info-banner wide">
                    <AlertCircle />
                    <span>
                      <strong>Secure Credentials:</strong> Your password will be used to log in to your shop workspace once your registration is approved.
                    </span>
                  </div>
                </div>

                <div className="form-actions">
                  <Button type="button" tone="secondary" onClick={() => setStep(1)}>
                    Back
                  </Button>
                  <Button type="button" onClick={handleNext}>
                    Continue to Plan & Consent <ArrowRight size={16} />
                  </Button>
                </div>
              </>
            )}

            {/* STEP 3: PLAN PREFERENCE & CONSENT */}
            {step === 3 && (
              <>
                <h1>Plan preference & consent</h1>
                <p className="form-intro">
                  Select your requested capacity tier and confirm application terms.
                </p>
                <div className="field-grid">
                  <label className="field wide">
                    <span>
                      Requested subscription tier <b>*</b>
                    </span>
                    <select
                      value={formData.requestedPlanCode}
                      onChange={(e) =>
                        setFormData({ ...formData, requestedPlanCode: e.target.value })
                      }
                    >
                      <option value="starter">Starter — Up to 500 items, 3 staff seats (Br 2,500/mo)</option>
                      <option value="growth">Growth — Up to 2,500 items, 10 staff seats (Br 5,500/mo)</option>
                      <option value="enterprise">Enterprise — Up to 10,000 items, 50 staff seats (Br 12,000/mo)</option>
                    </select>
                  </label>

                  <label className="field wide">
                    <span>Additional notes or special requirements (optional)</span>
                    <textarea
                      placeholder="Tell our review team about your product catalog size, current spreadsheets, or special conversion needs..."
                      value={formData.applicantNotes}
                      onChange={(e) =>
                        setFormData({ ...formData, applicantNotes: e.target.value })
                      }
                    />
                  </label>

                  <label className="check-row wide">
                    <input
                      type="checkbox"
                      checked={consentTerms}
                      onChange={(e) => setConsentTerms(e.target.checked)}
                    />
                    <span>
                      <strong>I agree to the Terms of Service and Privacy Policy.</strong>
                      <small>
                        I confirm I am authorized to submit this application on behalf of the business.
                      </small>
                    </span>
                  </label>
                  {fieldErrors.consent && (
                    <span className="field-error wide">{fieldErrors.consent}</span>
                  )}
                </div>

                <div className="form-actions">
                  <Button type="button" tone="secondary" onClick={() => setStep(2)}>
                    Back
                  </Button>
                  <Button type="submit" loading={submitting} disabled={submitting}>
                    {submitting ? 'Submitting Application...' : 'Submit registration request'} <ArrowRight size={16} />
                  </Button>
                </div>
              </>
            )}
          </form>
        </div>
      </main>
    </div>
  );
};
