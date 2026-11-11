import React, { useState } from 'react';
import { CheckCircle2, Mail, MessageSquare } from 'lucide-react';
import { Button } from '../components/Button';
import { PublicPage } from '../components/Header';

interface ContactPageProps {
  onNavigate: (page: PublicPage) => void;
}

export const ContactPage: React.FC<ContactPageProps> = ({ onNavigate }) => {
  const [submitted, setSubmitted] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email.trim() || !message.trim()) return;
    setSubmitted(true);
  };

  return (
    <main className="subpage">
      <div className="subpage-hero">
        <div className="section-label">Get in touch</div>
        <h1>Platform Inquiries & Support</h1>
        <p>
          Have questions regarding multi-branch capacity, custom integrations, or
          your shop onboarding status? Our engineering team is here to help.
        </p>
      </div>

      <section className="section no-top" style={{ maxWidth: 760 }}>
        {submitted ? (
          <div
            style={{
              background: 'white',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius)',
              padding: 40,
              textAlign: 'center',
            }}
          >
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: '50%',
                background: 'var(--teal-soft)',
                color: 'var(--teal)',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                marginBottom: 18,
              }}
            >
              <CheckCircle2 size={28} />
            </div>
            <h2>Thank you for reaching out.</h2>
            <p style={{ color: 'var(--muted)', marginTop: 8, lineHeight: 1.6 }}>
              We have received your message and will follow up with you at <strong>{email}</strong>.
            </p>
            <div style={{ marginTop: 24 }}>
              <Button tone="secondary" onClick={() => setSubmitted(false)}>
                Send another message
              </Button>
            </div>
          </div>
        ) : (
          <div
            style={{
              background: 'white',
              border: '1px solid var(--border)',
              borderRadius: 'var(--radius)',
              padding: 40,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
              <Mail style={{ color: 'var(--teal)' }} size={24} />
              <div>
                <strong>Direct Platform Contact</strong>
                <div style={{ color: 'var(--muted)', fontSize: 13 }}>
                  Email: support@kuterplatform.com
                </div>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="field-grid">
              <label className="field wide">
                <span>
                  Your Name <b>*</b>
                </span>
                <input
                  type="text"
                  placeholder="e.g. Maya Okafor"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                />
              </label>

              <label className="field wide">
                <span>
                  Work Email <b>*</b>
                </span>
                <input
                  type="email"
                  placeholder="name@business.example"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </label>

              <label className="field wide">
                <span>
                  Your Message <b>*</b>
                </span>
                <textarea
                  placeholder="Tell us what you'd like to discuss (e.g. bulk catalog migration, high-volume hardware stores)..."
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                  required
                />
              </label>

              <div className="form-actions wide">
                <Button type="submit" icon={<MessageSquare size={16} />}>
                  Send message
                </Button>
              </div>
            </form>
          </div>
        )}

        <div style={{ marginTop: 40, textAlign: 'center' }}>
          <p style={{ color: 'var(--muted)', fontSize: 14 }}>
            Looking to check on a pending shop application?{' '}
            <button
              className="text-link"
              type="button"
              onClick={() => onNavigate('status')}
              style={{ fontWeight: 700 }}
            >
              Check application status
            </button>
          </p>
        </div>
      </section>
    </main>
  );
};
