import React, { useEffect, useState } from 'react';
import {
  AlertCircle,
  ArrowRight,
  Check,
  PackageOpen,
  RefreshCw,
  ShieldCheck,
  Users,
  Warehouse,
} from 'lucide-react';
import { Button } from '../components/Button';
import { PublicPage } from '../components/Header';
import { PublicSubscriptionPlanDto } from '../types/api';
import { publicApi } from '../api/client';

interface PricingPageProps {
  onNavigate: (page: PublicPage, selectedPlan?: string) => void;
}

export const PricingPage: React.FC<PricingPageProps> = ({ onNavigate }) => {
  const [plans, setPlans] = useState<PublicSubscriptionPlanDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [annual, setAnnual] = useState(false);

  const fetchPlans = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await publicApi.getPublicPlans();
      const sorted = [...data].sort((a, b) => Number(a.price) - Number(b.price));
      setPlans(sorted);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load subscription plans';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPlans();
  }, []);

  const getPrice = (basePrice: number) => {
    if (annual) {
      return Math.round(basePrice * 0.85);
    }
    return basePrice;
  };

  return (
    <main className="subpage">
      {/* Subpage Hero */}
      <div className="subpage-hero">
        <div className="section-label">Plans configured for your stage</div>
        <h1>
          Simple inventory pricing.
          <br />
          No accounting-suite overhead.
        </h1>
        <p>
          Choose a starting tier for your shop. Pricing is locked in upon approval,
          and every tier includes a 14-day free trial with full data access.
        </p>

        <div className="billing-toggle">
          <button
            className={!annual ? 'active' : ''}
            onClick={() => setAnnual(false)}
            type="button"
          >
            Monthly
          </button>
          <button
            className={annual ? 'active' : ''}
            onClick={() => setAnnual(true)}
            type="button"
          >
            Yearly <span>Save 15%</span>
          </button>
        </div>
      </div>

      {/* Plan Cards Section */}
      <section className="section no-top">
        {loading ? (
          <div className="loading-box">
            <span className="spinner" />
            <p>Fetching current public plan offerings...</p>
          </div>
        ) : error ? (
          <div className="error-banner" style={{ maxWidth: 680, margin: '20px auto' }}>
            <AlertCircle />
            <div style={{ flex: 1 }}>
              <strong>Could not load plans:</strong> {error}
            </div>
            <Button tone="secondary" onClick={fetchPlans} icon={<RefreshCw size={14} />}>
              Retry
            </Button>
          </div>
        ) : plans.length === 0 ? (
          <div className="loading-box">
            <p>No active public subscription tiers are currently listed.</p>
            <Button onClick={() => onNavigate('register')}>Contact us about access</Button>
          </div>
        ) : (
          <div className="plan-grid">
            {plans.map((plan) => {
              const isGrowth = plan.code.toLowerCase() === 'growth';
              const calculatedPrice = getPrice(plan.price);
              return (
                <article
                  key={plan.code}
                  className={`plan-card ${isGrowth ? 'recommended' : ''}`}
                >
                  {isGrowth && <div className="recommended-label">Most popular</div>}
                  <h3>{plan.name}</h3>
                  <p>{plan.description}</p>
                  <div className="price">
                    <small>from</small>
                    <strong>Br {calculatedPrice.toLocaleString()}</strong>
                    <span>/ month {annual && '(billed annually)'}</span>
                  </div>
                  <div className="limits">
                    <span>
                      <PackageOpen /> Up to {plan.productLimit.toLocaleString()} products
                    </span>
                    <span>
                      <Users /> {plan.userLimit} staff seats
                    </span>
                    <span>
                      <Warehouse /> {plan.branchLimit} yard branch{plan.branchLimit > 1 ? 'es' : ''}
                    </span>
                    <span>
                      <ShieldCheck /> {plan.trialDays}-day full-feature trial
                    </span>
                  </div>
                  <Button
                    tone={isGrowth ? 'primary' : 'secondary'}
                    onClick={() => onNavigate('register', plan.code)}
                  >
                    Choose {plan.name} <ArrowRight size={16} />
                  </Button>
                  <ul>
                    {plan.featuresSummary
                      ? plan.featuresSummary.split(',').map((f) => (
                          <li key={f.trim()}>
                            <Check /> {f.trim()}
                          </li>
                        ))
                      : [
                          'Dual-UOM product conversion',
                          'Supplier delivery note receiving',
                          'Moving weighted-average costing',
                          'Traceable stock movement history',
                          'Low stock & reorder indicators',
                        ].map((feat) => (
                          <li key={feat}>
                            <Check /> {feat}
                          </li>
                        ))}
                  </ul>
                </article>
              );
            })}
          </div>
        )}

        <p className="sample-note">
          <AlertCircle /> Prices shown are billed in Ethiopian Birr (ETB / Br). All plans include free support and onboarding setup.
        </p>
      </section>

      {/* Feature Comparison Table */}
      <section className="comparison">
        <h2>Detailed feature comparison</h2>
        {[
          {
            feature: 'Base & packaging dual-unit conversion',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Supplier delivery note receiving',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Moving weighted-average cost valuation',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Immutable stock movement ledger',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Stock adjustments with reason codes',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Counter checkout stock deduction',
            starter: 'Included',
            growth: 'Included',
            enterprise: 'Included',
          },
          {
            feature: 'Multi-branch yard support',
            starter: '1 branch',
            growth: 'Up to 3 branches',
            enterprise: 'Up to 10 branches',
          },
          {
            feature: 'Multi-role user authorization',
            starter: '3 seats',
            growth: '10 seats',
            enterprise: '50 seats',
          },
          {
            feature: 'Onboarding & data migration support',
            starter: 'Self-guided',
            growth: 'Priority onboarding',
            enterprise: 'Dedicated manager',
          },
        ].map((row) => (
          <div className="compare-row" key={row.feature}>
            <strong>{row.feature}</strong>
            <span>
              {row.starter.includes('Included') ? (
                <>
                  <Check /> Included
                </>
              ) : (
                row.starter
              )}
            </span>
            <span>
              {row.growth.includes('Included') ? (
                <>
                  <Check /> Included
                </>
              ) : (
                row.growth
              )}
            </span>
            <span>
              {row.enterprise.includes('Included') ? (
                <>
                  <Check /> Included
                </>
              ) : (
                row.enterprise
              )}
            </span>
          </div>
        ))}
      </section>
    </main>
  );
};
