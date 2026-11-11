import React, { useEffect, useState } from 'react';
import {
  ArrowRight,
  Boxes,
  CheckCircle2,
  History,
  PackageOpen,
  Plus,
  ReceiptText,
  ShieldCheck,
  Sparkles,
  TrendingUp,
  TriangleAlert,
  Users,
  Check,
} from 'lucide-react';
import { Button } from '../components/Button';
import { Badge } from '../components/Badge';
import { Logo } from '../components/Logo';
import { PublicPage } from '../components/Header';
import { PublicSubscriptionPlanDto } from '../types/api';
import { publicApi } from '../api/client';

interface HomePageProps {
  onNavigate: (page: PublicPage, selectedPlan?: string) => void;
}

export const HomePage: React.FC<HomePageProps> = ({ onNavigate }) => {
  const [plans, setPlans] = useState<PublicSubscriptionPlanDto[]>([]);
  const [plansLoading, setPlansLoading] = useState(false);

  useEffect(() => {
    let active = true;
    setPlansLoading(true);
    publicApi
      .getPublicPlans()
      .then((data) => {
        if (active) setPlans(data);
      })
      .catch(() => {
        // Fallback plans if backend is unreachable during demo
        if (active) {
          setPlans([
            {
              id: 'fallback-starter',
              code: 'starter',
              name: 'Starter',
              description: 'Essential dual-unit inventory control for single yard building materials shops.',
              price: 49,
              currency: 'USD',
              billingInterval: 'MONTHLY',
              productLimit: 500,
              userLimit: 3,
              branchLimit: 1,
              trialDays: 14,
              featuresSummary: 'Up to 500 products, 3 staff seats, counter checkout, basic UOM conversion',
            },
            {
              id: 'fallback-growth',
              code: 'growth',
              name: 'Growth',
              description: 'High-volume materials distribution, multiple warehouse locations, and advanced conversion rules.',
              price: 119,
              currency: 'USD',
              billingInterval: 'MONTHLY',
              productLimit: 2500,
              userLimit: 10,
              branchLimit: 3,
              trialDays: 14,
              featuresSummary: 'Up to 2,500 products, 10 staff seats, delivery note receiving, multi-location stock ledger',
            },
            {
              id: 'fallback-enterprise',
              code: 'enterprise',
              name: 'Enterprise',
              description: 'Custom limits, dedicated support, and enterprise audit capabilities.',
              price: 279,
              currency: 'USD',
              billingInterval: 'MONTHLY',
              productLimit: 10000,
              userLimit: 50,
              branchLimit: 10,
              trialDays: 14,
              featuresSummary: 'Up to 10,000 products, 50 staff seats, unlimited branches, priority phone support',
            },
          ]);
        }
      })
      .finally(() => {
        if (active) setPlansLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <main>
      {/* Hero Section */}
      <section className="hero">
        <div className="hero-copy">
          <div className="eyebrow">
            <Sparkles size={15} /> Inventory clarity, built for materials shops
          </div>
          <h1>
            Know what’s in stock.
            <br />
            <em>Trust every number.</em>
          </h1>
          <p>
            Track quantities across units, monitor moving weighted-average costs,
            receive delivery notes, and keep an immutable stock ledger—built specifically
            for independent building materials merchants.
          </p>
          <div className="hero-actions">
            <Button
              onClick={() => onNavigate('register')}
              icon={<ArrowRight size={17} />}
            >
              Request access
            </Button>
            <Button tone="secondary" onClick={() => onNavigate('features')}>
              Explore features
            </Button>
          </div>
          <div className="hero-proof">
            <span>
              <CheckCircle2 /> No POS clutter
            </span>
            <span>
              <CheckCircle2 /> One shop, clearly managed
            </span>
            <span>
              <CheckCircle2 /> Approval-based access
            </span>
          </div>
        </div>

        <div className="hero-visual">
          <div
            className="material-photo"
            role="img"
            aria-label="Metal building materials stored neatly in warehouse racks"
          />
          <div className="dashboard-preview">
            <div className="preview-top">
              <Logo />
              <span>Northstar Building Supply</span>
              <span className="avatar">NB</span>
            </div>
            <div className="preview-content">
              <div className="preview-kicker">INVENTORY OVERVIEW</div>
              <div className="preview-heading">Yard Stock Snapshot</div>
              <div className="preview-stats">
                <div>
                  <small>Inventory value</small>
                  <strong>$6,271.70</strong>
                  <span className="up">+4.8% this month</span>
                </div>
                <div>
                  <small>Active products</small>
                  <strong>126</strong>
                  <span>Across 11 categories</span>
                </div>
                <div>
                  <small>Needs attention</small>
                  <strong>8</strong>
                  <span className="warning">6 low · 2 out</span>
                </div>
              </div>
              <div className="preview-table">
                <div className="preview-row head">
                  <span>Product</span>
                  <span>On hand</span>
                  <span>Status</span>
                </div>
                <div className="preview-row">
                  <span>Portland Cement 50kg</span>
                  <b>248 BAG</b>
                  <Badge status="IN STOCK">In stock</Badge>
                </div>
                <div className="preview-row">
                  <span>Reinforcement Bar 12mm</span>
                  <b>86 PCS</b>
                  <Badge status="LOW STOCK">Low stock</Badge>
                </div>
                <div className="preview-row">
                  <span>Roofing Sheet 3m</span>
                  <b>0 PCS</b>
                  <Badge status="OUT OF STOCK">Out of stock</Badge>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Trust Strip */}
      <section className="trust-strip">
        <span>Designed for the reality of building materials</span>
        <div>
          <strong>BAG</strong>
          <strong>PCS</strong>
          <strong>KG</strong>
          <strong>BOX</strong>
          <strong>L</strong>
          <strong>M</strong>
        </div>
      </section>

      {/* Problem Section */}
      <section className="section problem-section">
        <div className="section-label">Less uncertainty. More control.</div>
        <div className="section-split">
          <h2>Inventory work is hard enough without disconnected records.</h2>
          <p>
            Kuter replaces handwritten adjustments, damaged-packet guesswork, and
            scattered spreadsheets with a shared, accountable inventory ledger your
            whole yard team can understand.
          </p>
        </div>
        <div className="problem-grid">
          {[
            [
              '01',
              'Unclear quantities',
              'See true on-hand balance in base units and packaging units simultaneously.',
            ],
            [
              '02',
              'Mixed packaging',
              'Convert boxes, bags, pieces, and weight per product with defined ratios.',
            ],
            [
              '03',
              'Unrecorded changes',
              'Require a reason, actor, and source document for every single movement.',
            ],
            [
              '04',
              'Unknown value',
              'Track moving weighted-average cost automatically as purchase prices change.',
            ],
          ].map(([n, title, body]) => (
            <article key={n}>
              <span>{n}</span>
              <h3>{title}</h3>
              <p>{body}</p>
            </article>
          ))}
        </div>
      </section>

      {/* Features Section */}
      <section className="section features-section">
        <div className="section-heading">
          <div>
            <div className="section-label">One focused inventory workspace</div>
            <h2>
              Everything your yard team needs.
              <br />
              Nothing they don’t.
            </h2>
          </div>
          <Button tone="secondary" onClick={() => onNavigate('features')}>
            View all features <ArrowRight size={16} />
          </Button>
        </div>
        <div className="feature-grid">
          {[
            {
              icon: <Boxes />,
              title: 'Mixed-unit inventory',
              body: 'Stock in base units while receiving and selling in bags, boxes, kilograms, bundles, or pieces.',
              featured: true,
            },
            {
              icon: <ReceiptText />,
              title: 'Controlled stock receiving',
              body: 'Draft, review, and post delivery receipts with visible conversion factors and supplier costs.',
            },
            {
              icon: <History />,
              title: 'Traceable stock ledger',
              body: 'Every balance change records its source document, responsible actor, timestamp, and unit cost.',
            },
            {
              icon: <TrendingUp />,
              title: 'Reliable WAC valuation',
              body: 'Keep inventory value accurate through weighted-average costing recalculated at every receiving.',
            },
            {
              icon: <TriangleAlert />,
              title: 'Low-stock monitoring',
              body: 'Set reorder levels per item and spot stockouts before contractor orders are impacted.',
            },
            {
              icon: <Users />,
              title: 'Role-based access',
              body: 'Strict permissions for shop owners, branch managers, storekeepers, and cashiers.',
            },
          ].map((item) => (
            <article
              key={item.title}
              className={`feature-card ${item.featured ? 'featured' : ''}`}
            >
              <div className="feature-icon">{item.icon}</div>
              <h3>{item.title}</h3>
              <p>{item.body}</p>
              <span
                className="learn"
                onClick={() => onNavigate('features')}
                role="button"
                tabIndex={0}
              >
                Learn more <ArrowRight size={14} />
              </span>
            </article>
          ))}
        </div>
      </section>

      {/* How it Works Section */}
      <section className="section how-section" id="how-it-works">
        <div className="how-intro">
          <div className="section-label light">A considered start</div>
          <h2>From application to confident inventory control.</h2>
          <p>
            Every shop application is reviewed by our administration team before
            access is provisioned. That guarantees high data isolation, verified
            account ownership, and tailored setup.
          </p>
        </div>
        <div className="steps">
          {[
            [
              '01',
              'Request access',
              'Tell us about your shop, currency, and choose your preferred tier.',
            ],
            [
              '02',
              'Administrator review',
              'Our team reviews your submission and provisions your isolated tenant database.',
            ],
            [
              '03',
              'Set up your shop',
              'Receive your secure setup invite to configure products and unit conversion rules.',
            ],
            [
              '04',
              'Build your inventory',
              'Receive opening stock, print valuation sheets, and invite your yard staff.',
            ],
          ].map(([n, title, body]) => (
            <div className="step" key={n}>
              <span>{n}</span>
              <h3>{title}</h3>
              <p>{body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Pricing Preview */}
      <section className="section pricing-preview">
        <div className="section-heading">
          <div>
            <div className="section-label">Clear, flexible plans</div>
            <h2>Start with the capacity your shop needs.</h2>
          </div>
          <button className="text-link" onClick={() => onNavigate('pricing')}>
            Compare every feature <ArrowRight size={15} />
          </button>
        </div>

        {plansLoading ? (
          <div className="loading-box">
            <span className="spinner" />
            <p>Loading available subscription tiers...</p>
          </div>
        ) : (
          <div className="plan-grid">
            {plans.map((plan) => {
              const isGrowth = plan.code.toLowerCase() === 'growth';
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
                    <strong>${plan.price}</strong>
                    <span>/ {plan.billingInterval.toLowerCase()}</span>
                  </div>
                  <div className="limits">
                    <span>
                      <PackageOpen /> Up to {plan.productLimit.toLocaleString()} products
                    </span>
                    <span>
                      <Users /> {plan.userLimit} staff seats
                    </span>
                    <span>
                      <ShieldCheck /> {plan.trialDays}-day free trial
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
                          'Dual-UOM conversions',
                          'Delivery note receiving',
                          'Immutable movement ledger',
                          'Role-based permissions',
                        ].map((f) => (
                          <li key={f}>
                            <Check /> {f}
                          </li>
                        ))}
                  </ul>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* FAQ Section */}
      <section className="section faq-section">
        <div>
          <div className="section-label">Common questions</div>
          <h2>Answers before you get started.</h2>
          <p>
            Have a question not covered here? Check application status or contact
            our platform team.
          </p>
        </div>
        <div className="faq-list">
          {[
            [
              'How does shop onboarding work?',
              'Submit a registration request using the Request Access button. Once our administrators review and approve your application, you receive a private invitation link to complete setup.',
            ],
            [
              'Why does registration require review rather than instant checkout?',
              'Building materials shops have custom requirements around currencies, multiple yards, and stock balances. Approval prevents fraudulent registrations and guarantees personal setup support.',
            ],
            [
              'Can I sell in pieces while stocking in cartons or bundles?',
              'Yes! Each product has a configured base unit (e.g. PCS) and can define alternate conversion units (e.g. 1 BOX = 200 PCS, 1 BUNDLE = 10 PCS). Stocks and costs convert accurately automatically.',
            ],
            [
              'Is my shop data completely isolated from other merchants?',
              'Yes. Kuter enforces strict multi-tenant isolation at the database layer with cryptographic JWT claims and verified tenant scopes on every API request.',
            ],
          ].map(([q, a], i) => (
            <details key={q} open={i === 0}>
              <summary>
                {q}
                <Plus size={18} />
              </summary>
              <p>{a}</p>
            </details>
          ))}
        </div>
      </section>
    </main>
  );
};
