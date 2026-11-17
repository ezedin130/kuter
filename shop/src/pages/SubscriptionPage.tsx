import React, { useState, useEffect } from 'react';
import {
  CheckCircle,
  RefreshCw,
  Package,
  Users,
} from 'lucide-react';
import { PageHead } from '../components/PageHead';
import { Alert } from '../components/Alert';
import { shopApi } from '../api/client';
import { TenantSubscriptionInfo } from '../api/types';
import { useLanguage } from '../i18n/LanguageContext';

export const SubscriptionPage: React.FC = () => {
  const { t, formatDate, language } = useLanguage();
  const [sub, setSub] = useState<TenantSubscriptionInfo | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSubscription = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await shopApi.getSubscription();
      setSub(res.data);
    } catch (err: any) {
      setError(err.message || 'Failed to load subscription details.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSubscription();
  }, []);

  return (
    <div>
      <PageHead
        eyebrow={language === 'am' ? 'ፈቃድ እና ኮታዎች' : 'License & Quotas'}
        title={t.subscription.title}
        subtitle={t.subscription.subtitle}
        actions={
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchSubscription}
            disabled={isLoading}
          >
            <RefreshCw size={14} className={isLoading ? 'spin' : ''} />
            <span>{t.common.refresh}</span>
          </button>
        }
      />

      {error && <Alert type="error" message={error} onDismiss={() => setError(null)} />}

      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '60px', color: 'var(--muted)' }}>
          {language === 'am' ? 'የደንበኝነት ምዝገባ ሁኔታ እና ኮታዎችን በመጫን ላይ...' : 'Loading subscription status and quotas...'}
        </div>
      ) : sub ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          {/* Main Tier Overview Card */}
          <div
            className="card"
            style={{
              background: 'linear-gradient(135deg, #102a3a 0%, #173f54 100%)',
              color: 'white',
              border: 'none',
              padding: '32px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
              <div>
                <span
                  style={{
                    fontSize: '11px',
                    fontWeight: 800,
                    textTransform: 'uppercase',
                    letterSpacing: '1px',
                    color: 'var(--teal-soft)',
                  }}
                >
                  {t.subscription.currentPlan}
                </span>
                <h2 style={{ fontSize: '32px', fontWeight: 800, color: 'white', marginTop: '4px' }}>
                  {sub.planName}
                </h2>
                <p style={{ color: '#b2c8d2', fontSize: '14px', marginTop: '4px' }}>
                  {language === 'am' ? 'የፕላን ኮድ:' : 'Plan Code:'} <code>{sub.planCode}</code>
                </p>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '8px' }}>
                <span
                  style={{
                    background: 'rgba(22, 117, 108, 0.3)',
                    color: '#7be3d8',
                    border: '1px solid rgba(123, 227, 216, 0.4)',
                    padding: '6px 14px',
                    borderRadius: '99px',
                    fontSize: '12px',
                    fontWeight: 700,
                    textTransform: 'uppercase',
                  }}
                >
                  {sub.status}
                </span>
                <span style={{ fontSize: '13px', color: '#b2c8d2' }}>
                  {sub.daysRemaining != null
                    ? (language === 'am' ? `በአሁኑ የክፍያ ዑደት ውስጥ ${sub.daysRemaining} ቀናት ይቀራሉ` : `${sub.daysRemaining} days remaining in current billing cycle`)
                    : ''}
                </span>
              </div>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                gap: '20px',
                marginTop: '32px',
                paddingTop: '24px',
                borderTop: '1px solid rgba(255, 255, 255, 0.15)',
              }}
            >
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>{language === 'am' ? 'የጊዜ ጅማሬ' : 'Period Start'}</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {formatDate(sub.currentPeriodStart)}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>{t.subscription.expiryDate}</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {formatDate(sub.currentPeriodEnd)}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '11.5px', color: '#9bb7c4', textTransform: 'uppercase' }}>{language === 'am' ? 'የስረዛ ሁኔታ' : 'Cancellation Status'}</span>
                <div style={{ fontSize: '14px', fontWeight: 700, marginTop: '2px' }}>
                  {sub.cancelAtPeriodEnd
                    ? (language === 'am' ? 'በጊዜው ማብቂያ ላይ እንዲሰረዝ ተይዟል' : 'Scheduled to cancel at period end')
                    : (language === 'am' ? 'በራስ-ሰር የሚታደስ ንቁ ውል' : 'Auto-renewing active contract')}
                </div>
              </div>
            </div>
          </div>

          {/* Quota Limits Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '20px' }}>
              {t.subscription.quotas}
            </h2>
            {(() => {
              const productCount = sub.usage?.productCount ?? (sub as any).activeProductsCount ?? 0;
              const productLimit = sub.limits?.maxProducts ?? (sub as any).productLimit ?? null;
              const isProductUnlimited = productLimit == null;
              const productPct = isProductUnlimited ? 0 : Math.min(100, (productCount / (productLimit || 1)) * 100);

              const userCount = sub.usage?.userCount ?? (sub as any).activeUsersCount ?? 1;
              const userLimit = sub.limits?.maxUsers ?? (sub as any).userLimit ?? null;
              const isUserUnlimited = userLimit == null;
              const userPct = isUserUnlimited ? 0 : Math.min(100, (userCount / (userLimit || 1)) * 100);

              return (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '24px' }}>
                  {/* Products Quota */}
                  <div
                    style={{
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius)',
                      padding: '20px',
                      background: '#fcfdfd',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                      <Package size={20} color="var(--teal)" />
                      <span style={{ fontWeight: 700, fontSize: '15px' }}>{t.subscription.productsQuota}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--muted)', marginBottom: '8px' }}>
                      <span>{language === 'am' ? 'ጥቅም ላይ የዋለ: ' : 'Used: '}<strong>{productCount}</strong> {language === 'am' ? 'ምርቶች' : 'products'}</span>
                      <span>{language === 'am' ? 'ገደብ: ' : 'Limit: '}<strong>{isProductUnlimited ? (language === 'am' ? 'ያልተገደበ' : 'Unlimited') : productLimit}</strong></span>
                    </div>
                    <div style={{ height: '8px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                      <div
                        style={{
                          height: '100%',
                          background: productPct >= 90 ? 'var(--red, #e53e3e)' : 'var(--teal)',
                          width: `${isProductUnlimited ? 15 : productPct}%`,
                          transition: 'width 0.3s ease',
                        }}
                      />
                    </div>
                    <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '8px' }}>
                      {isProductUnlimited
                        ? (language === 'am' ? 'ያልተገደበ የምርት ካታሎግ ይፈቀዳል' : 'Unlimited catalog products allowed')
                        : (language === 'am' ? `${Math.max(0, productLimit - productCount)} ምርቶች ይቀራሉ` : `${Math.max(0, productLimit - productCount)} product slots remaining`)}
                    </div>
                  </div>

                  {/* Staff Users Quota */}
                  <div
                    style={{
                      border: '1px solid var(--border)',
                      borderRadius: 'var(--radius)',
                      padding: '20px',
                      background: '#fcfdfd',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '12px' }}>
                      <Users size={20} color="var(--blue)" />
                      <span style={{ fontWeight: 700, fontSize: '15px' }}>{t.subscription.staffQuota}</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--muted)', marginBottom: '8px' }}>
                      <span>{language === 'am' ? 'ንቁ: ' : 'Active: '}<strong>{userCount}</strong> {language === 'am' ? 'ተጠቃሚዎች' : 'users'}</span>
                      <span>{language === 'am' ? 'ገደብ: ' : 'Limit: '}<strong>{isUserUnlimited ? (language === 'am' ? 'ያልተገደበ' : 'Unlimited') : userLimit}</strong></span>
                    </div>
                    <div style={{ height: '8px', background: '#e2e8e7', borderRadius: '4px', overflow: 'hidden' }}>
                      <div
                        style={{
                          height: '100%',
                          background: userPct >= 90 ? 'var(--red, #e53e3e)' : 'var(--blue)',
                          width: `${isUserUnlimited ? 15 : userPct}%`,
                          transition: 'width 0.3s ease',
                        }}
                      />
                    </div>
                    <div style={{ fontSize: '11.5px', color: 'var(--faint)', marginTop: '8px' }}>
                      {isUserUnlimited
                        ? (language === 'am' ? 'ያልተገደበ የሰራተኞች መለያ ይፈቀዳል' : 'Unlimited staff accounts allowed')
                        : (language === 'am' ? `${Math.max(0, userLimit - userCount)} የተጠቃሚ ቦታዎች ይቀራሉ` : `${Math.max(0, userLimit - userCount)} user seats remaining`)}
                    </div>
                  </div>
                </div>
              );
            })()}
          </div>

          {/* Entitlements Card */}
          <div className="card">
            <h2 className="card-title" style={{ marginBottom: '16px' }}>
              {language === 'am' ? 'የፕላን መብቶች እና ባህሪያት' : 'Plan Entitlements & Features'}
            </h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>
                    {language === 'am' ? 'ድርብ የመለኪያ ልወጣ ሞተር (Dual-UOM)' : 'Dual-UOM Conversion Engine'}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    {language === 'am'
                      ? 'በተለያዩ የችርቻሮ መለኪያዎች መሸጥ እና ማሸግ (ፍሬ፣ ጥቅል፣ ቶን)።'
                      : 'Multi-unit retail sales and packaging conversions (Pieces, Bundles, Tons).'}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>
                    {language === 'am' ? 'ተለዋዋጭ አማካይ ወጪ ስሌት (Weighted-Average)' : 'Moving Weighted-Average Costing'}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    {language === 'am'
                      ? 'የተሸጡ ዕቃዎች ቀጣይነት ያለው የወጪ ክትትል እና የክምችት ሚዛን።'
                      : 'Continuous COGS tracking across deliveries and retail inventory balances.'}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>
                    {language === 'am' ? 'የማይለወጥ የእንቅስቃሴ መዝገብ' : 'Immutable Movement Ledger'}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    {language === 'am'
                      ? 'ለገቢዎች፣ ሽያጮች፣ ስረዛዎች እና ብክነቶች አስተማማኝ የክምችት እንቅስቃሴ መዝገብ።'
                      : 'Tamper-proof stock movement records for receipts, sales, voids, and shrinkage adjustments.'}
                  </div>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', alignItems: 'flex-start' }}>
                <CheckCircle size={18} color="var(--teal)" style={{ marginTop: '2px', flexShrink: 0 }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '13.5px' }}>
                    {language === 'am' ? 'በሚና ላይ የተመሰረተ የመዳረሻ ቁጥጥር' : 'Role-Based Access Control'}
                  </div>
                  <div style={{ fontSize: '12px', color: 'var(--muted)' }}>
                    {language === 'am'
                      ? 'ባለቤት፣ ስራ አስኪያጅ፣ መጋዘን ኃላፊ እና ገንዘብ ተቀባይ መለያየት ከገንዘብ ሚስጥራዊነት ጋር።'
                      : 'Owner, Manager, Storekeeper, and Cashier segregation with financial redaction.'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
