import React, { useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  Check,
  Clock3,
  LockKeyhole,
  Search,
  XCircle,
} from 'lucide-react';
import { Button } from '../components/Button';
import { Badge } from '../components/Badge';
import { Logo } from '../components/Logo';
import { PublicPage } from '../components/Header';
import { OnboardingPublicResponse } from '../types/api';
import { publicApi } from '../api/client';

interface StatusPageProps {
  onNavigate: (page: PublicPage) => void;
}

export const StatusPage: React.FC<StatusPageProps> = ({ onNavigate }) => {
  const [referenceCode, setReferenceCode] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<OnboardingPublicResponse | null>(null);

  const handleLookup = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanRef = referenceCode.trim();
    if (!cleanRef) {
      setError('Please enter a valid request reference code (e.g. REQ-2026-00001)');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const data = await publicApi.getOnboardingStatus(cleanRef);
      setResult(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Lookup failed';
      if (msg.includes('404') || msg.toLowerCase().includes('not found')) {
        setError(`No registration request was found matching reference "${cleanRef}". Please verify the code.`);
      } else {
        setError(msg);
      }
      setResult(null);
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (isoString?: string) => {
    if (!isoString) return 'Recent';
    try {
      const date = new Date(isoString);
      return new Intl.DateTimeFormat('en-US', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date);
    } catch {
      return isoString;
    }
  };

  return (
    <div className="auth-page status-page">
      <div className="auth-brand">
        <Logo light onClick={() => onNavigate('home')} />
        <button onClick={() => onNavigate('home')}>Back to website</button>
      </div>

      <div className="status-card">
        <div className="eyebrow">Application Status</div>
        <h1>
          {result
            ? 'Application review progress'
            : 'Check your registration status'}
        </h1>

        {result ? (
          <>
            <div className="status-result">
              <div className="status-result-head">
                <div>
                  <small>REQUEST REFERENCE</small>
                  <strong>{result.referenceCode}</strong>
                </div>
                <Badge status={result.status}>{result.status}</Badge>
              </div>

              {/* Visual review timeline */}
              <div className="review-visual" aria-hidden="true">
                <span className="active" title="Submitted">
                  <Check />
                </span>
                <i />
                <span
                  className={
                    result.status === 'REJECTED'
                      ? 'rejected'
                      : result.status === 'APPROVED' || result.status === 'PENDING'
                      ? 'active'
                      : ''
                  }
                  title="Administrator Review"
                >
                  {result.status === 'REJECTED' ? (
                    <XCircle />
                  ) : result.status === 'APPROVED' ? (
                    <Check />
                  ) : (
                    <Clock3 />
                  )}
                </span>
                <i />
                <span
                  className={result.status === 'APPROVED' ? 'active' : ''}
                  title="Account Setup"
                >
                  {result.status === 'APPROVED' ? <Check /> : <LockKeyhole />}
                </span>
              </div>

              <h3>
                {result.status === 'PENDING' && 'Under Administrator Review'}
                {result.status === 'APPROVED' && 'Application Approved!'}
                {result.status === 'REJECTED' && 'Application Declined'}
                {result.status === 'MORE_INFO_REQUESTED' && 'More Information Requested'}
              </h3>

              <p>
                {result.status === 'PENDING' &&
                  `Your registration request for ${result.shopName} was received on ${formatDate(
                    result.createdAt
                  )}. Our team is verifying shop and plan configurations.`}
                {result.status === 'APPROVED' &&
                  `Congratulations! ${result.shopName} has been approved. An invitation link has been dispatched to ${result.ownerEmail} to activate your workspace.`}
                {result.status === 'REJECTED' &&
                  `The application for ${result.shopName} could not be approved at this time. Please contact support@kuterplatform.com for assistance.`}
                {result.status === 'MORE_INFO_REQUESTED' &&
                  `Our reviewers have questions regarding ${result.shopName}. Please check ${result.ownerEmail} for our verification request.`}
              </p>

              <div className="status-meta-grid">
                <div>
                  <small>Shop Name</small>
                  <strong>{result.shopName}</strong>
                </div>
                <div>
                  <small>Owner Contact</small>
                  <strong>{result.ownerEmail}</strong>
                </div>
                <div>
                  <small>Requested Plan</small>
                  <strong style={{ textTransform: 'capitalize' }}>
                    {result.requestedPlanCode}
                  </strong>
                </div>
                <div>
                  <small>Submission Date</small>
                  <strong>{formatDate(result.createdAt)}</strong>
                </div>
              </div>
            </div>

            <Button
              tone="secondary"
              onClick={() => {
                setResult(null);
                setReferenceCode('');
                setError(null);
              }}
            >
              Check another reference
            </Button>
          </>
        ) : (
          <>
            <p>
              Enter the request reference code provided when you submitted your shop
              application (format: <code>REQ-YYYY-XXXXX</code>).
            </p>

            {error && (
              <div className="error-banner" role="alert">
                <AlertCircle />
                <div>{error}</div>
              </div>
            )}

            <form onSubmit={handleLookup}>
              <label className="field">
                <span>
                  Request reference code <b>*</b>
                </span>
                <input
                  type="text"
                  placeholder="e.g. REQ-2026-00001"
                  value={referenceCode}
                  onChange={(e) => setReferenceCode(e.target.value.toUpperCase())}
                  autoFocus
                />
              </label>

              <Button type="submit" loading={loading} icon={<Search size={16} />}>
                Check application status <ArrowRight size={16} />
              </Button>
            </form>
          </>
        )}

        <button
          className="text-link"
          type="button"
          onClick={() => onNavigate('home')}
        >
          Return to homepage
        </button>
      </div>
    </div>
  );
};
