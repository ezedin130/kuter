# Kuter — Public Marketing Website & Registration Portal

The public marketing website and shop registration portal for **Kuter**, a multi-tenant SaaS inventory management platform tailored for independent building-materials merchants.

Built with **React 19**, **TypeScript**, **Vite**, and styled following the official **Figma** design system tokens.

---

## Capabilities & Pages

- **Home Page (`/` or `#home`)**:
  - Hero headline: *"Know what’s in stock. Trust every number."*
  - Dual-UOM packaging highlights (BAG, PCS, KG, BOX, L, M)
  - Interactive live preview of building-materials stock ledger and yard valuation
  - Problem & solution pillars replacing handwritten adjustments
  - 4-step considered onboarding flow: Request Access → Admin Review → Setup → Inventory Building
  - Live subscription preview cards and common FAQs

- **Features Page (`#features`)**:
  - In-depth capability breakdown: Dual-UOM Conversion Engine, Delivery Note Receiving, Moving Weighted-Average Costing (WAC), Immutable Movement Ledger, Accountable Adjustments, Role-based Access, and Multi-tenant Data Sovereignty.
  - Interactive calculation demo (`5 BOX × 200 PCS = 1,000 PCS`).

- **Pricing Page (`#pricing`)**:
  - Dynamic fetch from Spring Boot `GET /api/v1/public/subscription-plans`.
  - Monthly vs. Yearly toggle with automatic 15% discount calculation.
  - Loading skeletons, empty states, and error retry handling.
  - Quota indicators (`productLimit`, `userLimit`, `branchLimit`, `trialDays`).
  - Comprehensive feature comparison matrix.
  - Direct CTA passing the chosen tier to registration.

- **Shop Registration Request Wizard (`#register`)**:
  - 3-step wizard with step navigation and client validation:
    - **Step 1 — Shop Details**: `shopName`, `preferredShopCode`, `businessAddress`, and `currency` (USD, GHS, NGN, KES).
    - **Step 2 — Account Owner**: `ownerFullName`, `ownerEmail`, `ownerPhone`. Explicit banner noting no password is collected upfront due to review-based onboarding.
    - **Step 3 — Plan & Consent**: Preselected plan tier, optional yard setup notes, and Terms of Service consent.
  - Submits to `POST /api/v1/onboarding-requests`.
  - Handles HTTP 409 conflict errors cleanly for existing pending requests.
  - **Success Confirmation View**: Displays generated reference code (`REQ-YYYY-XXXXX`), one-click copy button, 3-stage visual timeline, and direct link to Status Checker.

- **Application Status Checker (`#status`)**:
  - Real-time application tracking querying `GET /api/v1/onboarding-requests/status/{referenceCode}`.
  - Displays status badges (`PENDING`, `APPROVED`, `REJECTED`, `MORE_INFO_REQUESTED`), submitted timestamp, and contextual reviewer guidance.

- **About Page (`#about`)**:
  - Honest, product-focused explanation of why generic POS and retail tools fail with building materials packaging.

- **Contact Inquiries (`#contact`)**:
  - Direct platform support channel and inquiry form.

---

## Development & Testing

```bash
# Navigate to public website directory
cd public

# Run local development server (Port 5175, with proxy to :8080 backend)
npm run dev

# Run Vitest test suite
npm test

# Production build verification (TypeScript + Vite)
npm run build
```

## Multi-Portal Port Mapping

| Service | Directory | Port | Description |
|---|---|---|---|
| **Spring Boot Backend** | `backend/` | `8080` | Multi-tenant REST API, Flyway, PostgreSQL |
| **Shop Tenant Portal** | `shop/` | `5173` | Shop owner and staff inventory / POS portal |
| **Platform Admin Portal** | `admin/` | `5174` | Platform administration, approvals & plan management |
| **Public Website** | `public/` | `5175` | Marketing website, public plans & registration wizard |
