import React from 'react';
import { ArrowRight } from 'lucide-react';
import { Button } from '../components/Button';
import { PublicPage } from '../components/Header';

interface AboutPageProps {
  onNavigate: (page: PublicPage) => void;
}

export const AboutPage: React.FC<AboutPageProps> = ({ onNavigate }) => {
  return (
    <main className="subpage">
      <div className="subpage-hero">
        <div className="section-label">Built for the yard</div>
        <h1>
          Why we built Kuter.
        </h1>
        <p>
          General-purpose accounting tools and retail point-of-sale systems fail when faced
          with building materials. Kuter was designed from day one to handle the messy,
          heavy reality of independent timber and building supplies yards.
        </p>
      </div>

      <section className="section no-top" style={{ maxWidth: 960 }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 32, fontSize: 16, lineHeight: 1.8, color: '#3b4e57' }}>
          <div>
            <h2 style={{ fontSize: 26, marginBottom: 12, color: 'var(--ink)' }}>The Problem with Generic Software</h2>
            <p>
              In a building materials store, you purchase cement in metric tons or pallets, but
              sell by 50kg bags. You receive nails in 25kg bulk boxes, but contractors purchase
              them in single packets or loose kilograms. You stock reinforcement rebar in bundles,
              yet cut and deliver in individual meters or pieces.
            </p>
            <p style={{ marginTop: 12 }}>
              Standard retail inventory systems assume 1 item in = 1 item out. When staff try to
              shoehorn packaging conversions into retail software, stock counts rapidly drift,
              weighted costs become inaccurate, and yard managers lose trust in their system.
            </p>
          </div>

          <div style={{ background: 'white', border: '1px solid var(--border)', borderRadius: 'var(--radius)', padding: 32 }}>
            <h2 style={{ fontSize: 24, marginBottom: 16, color: 'var(--ink)' }}>The Three Pillars of Kuter</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 24 }}>
              <div>
                <strong style={{ color: 'var(--teal)', display: 'block', fontSize: 18, marginBottom: 8 }}>
                  1. Dual-UOM Conversion
                </strong>
                <p style={{ fontSize: 14, color: 'var(--muted)', margin: 0 }}>
                  Every item has a true physical base unit, with customizable packaging unit
                  conversion ratios that calculate automatically without manual math.
                </p>
              </div>
              <div>
                <strong style={{ color: 'var(--teal)', display: 'block', fontSize: 18, marginBottom: 8 }}>
                  2. Moving WAC Costing
                </strong>
                <p style={{ fontSize: 14, color: 'var(--muted)', margin: 0 }}>
                  Building supplies prices change with supplier shipments. Kuter updates weighted-average
                  costs dynamically so yard inventory valuation reflects real purchase costs.
                </p>
              </div>
              <div>
                <strong style={{ color: 'var(--teal)', display: 'block', fontSize: 18, marginBottom: 8 }}>
                  3. Accountable Ledger
                </strong>
                <p style={{ fontSize: 14, color: 'var(--muted)', margin: 0 }}>
                  An immutable, append-only record links every adjustment, receiving slip, and counter
                  sale to a specific employee, timestamp, and verifiable reason code.
                </p>
              </div>
            </div>
          </div>

          <div>
            <h2 style={{ fontSize: 26, marginBottom: 12, color: 'var(--ink)' }}>Multi-Tenant Architecture & Data Sovereignty</h2>
            <p>
              Kuter is built as a secure multi-tenant platform on Spring Boot and PostgreSQL.
              Each shop operates within an isolated tenant partition, ensuring merchant sales,
              supplier pricing, and inventory quantities are strictly confidential.
            </p>
          </div>
        </div>

        <div style={{ marginTop: 48, textAlign: 'center' }}>
          <Button onClick={() => onNavigate('register')}>
            Request access for your shop <ArrowRight size={16} />
          </Button>
        </div>
      </section>
    </main>
  );
};
