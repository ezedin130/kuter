import React from 'react';
import {
  ArrowRight,
  Boxes,
  Check,
  FileSpreadsheet,
  History,
  ReceiptText,
  ShieldCheck,
  TrendingUp,
  TriangleAlert,
  Users,
} from 'lucide-react';
import { Button } from '../components/Button';
import { Badge } from '../components/Badge';
import { PublicPage } from '../components/Header';

interface FeaturesPageProps {
  onNavigate: (page: PublicPage) => void;
}

export const FeaturesPage: React.FC<FeaturesPageProps> = ({ onNavigate }) => {
  const detailedFeatures = [
    {
      icon: <Boxes />,
      title: 'Dual-UOM Conversion Engine',
      description:
        'Manage building materials in their physical stocking unit (BAG, PCS, M, KG) while receiving or selling in bulk packaging (BOX, BUNDLE, PALLET). Fractional and decimal conversions are calculated cleanly.',
      bullets: [
        'Configurable product-specific unit ratios',
        'Automatic base-quantity derivation',
        'Zero-error quantity rounding controls',
      ],
    },
    {
      icon: <ReceiptText />,
      title: 'Controlled Stock Receiving',
      description:
        'Receive supplier shipments against delivery notes. Record supplier invoice numbers, unit costs, and package quantities with full draft, review, and finalization workflows.',
      bullets: [
        'Delivery note and supplier reference tracking',
        'Immediate cost impact visibility before posting',
        'Non-reversible posted receipts for audit integrity',
      ],
    },
    {
      icon: <TrendingUp />,
      title: 'Moving Weighted-Average Costing',
      description:
        'Building materials prices fluctuate constantly. Kuter recalculates your inventory weighted-average cost automatically every time new stock arrives at different purchase rates.',
      bullets: [
        'Accurate real-time total inventory valuation',
        'Transparent cost-of-goods-sold calculation',
        'Historical cost snapshots on every movement',
      ],
    },
    {
      icon: <History />,
      title: 'Immutable Movement Ledger',
      description:
        'Every single stock change creates an append-only ledger transaction. No phantom inventory edits, no unexplained deletions, and no untraceable quantity adjustments.',
      bullets: [
        'Every transaction records timestamp, actor, and reason',
        'Filterable by movement type (RECEIVING, ADJUSTMENT, SALE, RETURN)',
        'Full historical balance reconstruction for audits',
      ],
    },
    {
      icon: <TriangleAlert />,
      title: 'Accountable Stock Adjustments',
      description:
        'Materials break, get damaged, or encounter physical count variance. Kuter requires an explicit reason code, mandatory notes, and authorized permissions for any manual adjustment.',
      bullets: [
        'Damaged, broken, expired, or shrinkage classifications',
        'Visual preview of balance change before confirming',
        'Restricted storekeeper authorization controls',
      ],
    },
    {
      icon: <Users />,
      title: 'Role-Based Access Control',
      description:
        'Protect your business data by giving your team members strictly what they need. Shop owners, yard managers, storekeepers, and cashiers have distinct role boundaries.',
      bullets: [
        'Owners oversee valuation and financial settings',
        'Storekeepers handle physical receiving and counts',
        'Cashiers process counter sales without cost exposure',
      ],
    },
    {
      icon: <ShieldCheck />,
      title: 'Strict Multi-Tenant Isolation',
      description:
        'Your shop records are guarded by cryptographically signed JWT tokens and tenant-scoped repository queries. One shop can never view or modify another shop’s products.',
      bullets: [
        'UUID-isolated tenant databases and schemas',
        'No client-provided tenant identifiers in business logic',
        'Enterprise-grade PostgreSQL ACID transactions',
      ],
    },
    {
      icon: <FileSpreadsheet />,
      title: 'Printable Stock & Valuation Sheets',
      description:
        'Generate focused inventory summaries for physical counts, supplier reconciliations, and tax reviews. Print ready-to-use lists directly or export data cleanly.',
      bullets: [
        'Current on-hand stock and reorder point alerts',
        'Category-based inventory valuation reports',
        'Printer-friendly clean layout formatting',
      ],
    },
  ];

  return (
    <main className="subpage">
      {/* Feature Hero */}
      <div className="feature-hero">
        <div>
          <div className="section-label">Purpose-built for materials shops</div>
          <h1>A clearer record from delivery to shelf.</h1>
          <p>
            Product-specific units, controlled posting, moving weighted-average costing,
            and audit-friendly history make every stock number explainable.
          </p>
          <Button onClick={() => onNavigate('register')}>
            Request access <ArrowRight size={16} />
          </Button>
        </div>

        <div className="feature-illustration">
          <div className="conversion-card">
            <Badge tone="success">Live calculation</Badge>
            <h3>5 BOX</h3>
            <span>× 200 PCS per BOX</span>
            <strong>= 1,000 PCS</strong>
            <small>Base quantity added to inventory balance</small>
          </div>
        </div>
      </div>

      {/* Feature Details Grid */}
      <section className="section">
        <div className="feature-detail-grid">
          {detailedFeatures.map((feat, idx) => (
            <article key={feat.title}>
              <span className="detail-number">
                {String(idx + 1).padStart(2, '0')}
              </span>
              <div className="feature-icon">{feat.icon}</div>
              <h2>{feat.title}</h2>
              <p>{feat.description}</p>
              <ul>
                {feat.bullets.map((b) => (
                  <li key={b}>
                    <Check /> {b}
                  </li>
                ))}
              </ul>
            </article>
          ))}
        </div>
      </section>

      {/* Bottom CTA */}
      <section className="section" style={{ textAlign: 'center', paddingTop: 0 }}>
        <h2>Ready to organize your building materials inventory?</h2>
        <p style={{ maxWidth: 600, margin: '16px auto 28px', color: 'var(--muted)' }}>
          Submit your shop registration request in three simple steps. We review and
          configure each shop before providing onboarding credentials.
        </p>
        <Button onClick={() => onNavigate('register')}>
          Request access for your shop <ArrowRight size={16} />
        </Button>
      </section>
    </main>
  );
};
