# Kuter — Phase Nine Implementation Report: Public Website, Subscription Plans, and Shop Registration

**Date:** October 29, 2026  
**Engineering Role:** Senior Frontend Engineer & SaaS Architect  
**Status:** **PHASE NINE COMPLETE**  
**Repository Directory:** `public/`  
**Test Suite Verification:** 16/16 Vitest Tests Passed (100%) | Production Build 0 Errors  
**Next Milestone:** Phase Ten — Shop Tenant Portal (`shop/`)

---

## 1. Executive Summary & Objective

Phase Nine delivers the polished, responsive public-facing website and merchant onboarding registration application for **Kuter**, the multi-tenant SaaS inventory management platform engineered for independent building-materials merchants (cement, reinforcement bar, timber, corrugated roofing, blocks, hardware).

The public web application is located exclusively in the dedicated root-level `public/` directory (running on port **`5175`**). It serves prospective shop merchants by explaining the product's core value proposition, displaying commercial subscription tiers fetched dynamically from the Spring Boot backend, providing a 3-step registration wizard, and enabling applicants to query review progress in real time.

All implementation boundaries were strictly maintained:
1. **Isolated Application Structure:** Implemented strictly inside `public/`. Platform administration remains isolated in `admin/` (`:5174`), the shop tenant portal remains in `shop/` (`:5173`), and backend persistence remains in `backend/` (`:8080`).
2. **Real Backend Integration:** Plans and onboarding status are fetched from the live Spring Boot API (`GET /api/v1/public/subscription-plans`, `POST /api/v1/onboarding-requests`, `GET /api/v1/onboarding-requests/status/{ref}`). No mock data or artificial endpoints were created.
3. **Figma Design System Fidelity:** Strict adherence to official design tokens, typography (`DM Sans` and `Manrope`), palette (`#102a3a` navy, `#16756c` teal, `#f6f4ed` cream, sand borders), responsive layouts, and accessible cards.
4. **Honest Marketing Standards:** Zero fabricated customer testimonials, fake review badges, artificial awards, fictitious company history, or simulated payment gateways.

---

## 2. Implemented Pages & User Workflows

### 2.1 Navigation & Header (`Header.tsx` & `Logo.tsx`)
- **Brand Mark:** SVG-styled logo mark with angled teal badge and white vertical bars, linked to home.
- **Navigation Links:** Features, How It Works (smooth-scroll anchor), Pricing, Application Status, and About.
- **Access Anchors:**
  - *"Shop login"* anchor linking directly to the Shop Tenant Portal (`http://localhost:5173`).
  - *"Request access"* prominent primary CTA button navigating to the registration wizard (`#register`).
- **Mobile Menu:** Collapsible hamburger drawer for small screens with aria attributes.

### 2.2 Home Page (`HomePage.tsx`)
- **Hero Banner:**
  - Eyebrow: `Inventory clarity, built for materials shops`
  - Headline: *"Know what’s in stock. Trust every number."*
  - Proof strip: *"No POS clutter"*, *"One shop, clearly managed"*, *"Approval-based access"*.
  - Primary CTA: *"Request access"* & secondary CTA: *"Explore features"*.
- **Interactive Yard Preview Widget:**
  - Real-time mockup displaying a sample building-materials yard (*Northstar Building Supply*).
  - Snapshot KPIs: Inventory Value (`$6,271.70`), Active Products (`126`), Attention alerts (`6 low · 2 out`).
  - Live product status rows: Portland Cement 50kg (`248 BAG`, In stock), Rebar 12mm (`86 PCS`, Low stock), Roofing Sheet 3m (`0 PCS`, Out of stock).
- **Trust Strip:**
  - Highlights physical units standard in the building supplies trade: `BAG`, `PCS`, `KG`, `BOX`, `L`, `M`.
- **Problem & Solution Pillars:**
  - 4 cards: `01 Unclear quantities`, `02 Mixed packaging`, `03 Unrecorded changes`, `04 Unknown value`.
- **Feature Grid:**
  - 6 capability highlights with custom SVG icons: Mixed-unit inventory, Controlled stock receiving, Traceable stock ledger, Reliable WAC valuation, Low-stock monitoring, and Role-based access.
- **How It Works (4-Step Considered Onboarding):**
  - Explains the review-first philosophy: `01 Request access` → `02 Administrator review` → `03 Set up your shop` → `04 Build your inventory`.
- **Pricing Preview & FAQ Accordion:**
  - Live plan preview cards and expandable details addressing units, approval requirements, multi-branch limits, and tenant data isolation.

### 2.3 Dedicated Features Page (`FeaturesPage.tsx`)
- **Interactive Calculation Hero:**
  - Visual breakdown demonstrating dual-UOM derivation: `5 BOX × 200 PCS per BOX = 1,000 PCS` added to base inventory balance.
- **Deep-Dive Technical Breakdown:**
  1. *Dual-UOM Conversion Engine:* Ratios between physical stocking units and commercial bulk packaging.
  2. *Controlled Stock Receiving:* Delivery note tracking, supplier invoice capture, and non-reversible posting.
  3. *Moving Weighted-Average Costing:* Automatic dynamic recalculation on new delivery rates.
  4. *Immutable Movement Ledger:* Append-only transaction log linking movements to actors, references, and timestamps.
  5. *Accountable Stock Adjustments:* Mandatory reason codes (breakage, damages, count variances).
  6. *Role-Based Access Control:* Dedicated permissions for Owner, Yard Manager, Storekeeper, and Cashier.
  7. *Strict Multi-Tenant Isolation:* Cryptographic JWT claims and repository tenant scopes.
  8. *Printable Stock & Valuation Sheets:* Clean print-ready formats for count audits and tax reconciliation.

### 2.4 Dynamic Pricing Page (`PricingPage.tsx`)
- **Live Spring Boot Integration:**
  - Fetches from `GET /api/v1/public/subscription-plans`.
  - Handles loading spinner, empty list fallback, and error retry state with refresh button.
- **Billing Interval Toggle:**
  - Monthly vs. Yearly toggle applying a 15% annual savings discount calculation.
- **Quota & Limit Transparency:**
  - Displays `productLimit`, `userLimit`, `branchLimit`, and `trialDays` per tier (Starter, Growth, Enterprise).
  - Recommended badge for Growth tier.
  - Direct *"Choose [Plan]"* CTA carrying the selected tier into the registration wizard.
- **Feature Comparison Table:**
  - 9-row granular breakdown comparing dual-UOM support, receiving, WAC costing, ledger, adjustments, counter checkout, branches, seats, and onboarding support.

### 2.5 3-Step Shop Registration Wizard (`RegistrationPage.tsx`)
- **Step 1 — Shop Details:**
  - `shopName` (required, 2-255 characters with inline validation error).
  - `preferredShopCode` (slug sanitizer allowing lowercase letters, numbers, and hyphens).
  - `currency` dropdown (USD, GHS, NGN, KES).
  - `businessAddress` (optional yard address).
- **Step 2 — Account Owner:**
  - `ownerFullName` (required, min 2 chars).
  - `ownerEmail` (required, regex-validated work email).
  - `ownerPhone` (optional contact phone).
  - **Review-Based Security Banner:** Explicitly notes that no password is asked at registration; access is review-based, and an invitation link will be emailed to create credentials once approved.
- **Step 3 — Plan Preference & Consent:**
  - `requestedPlanCode` (preselected or chosen from Starter, Growth, Enterprise).
  - `applicantNotes` (optional catalog size notes or conversion requirements).
  - Mandatory consent checkbox confirming Terms of Service and authorization.
- **Submission & Error Defense:**
  - Submits clean JSON payload to `POST /api/v1/onboarding-requests`.
  - Handles HTTP 409 conflict cleanly (e.g. if a registration is already pending for that email).
- **Success Confirmation View:**
  - Displays generated `referenceCode` (e.g. `REQ-2026-00002`).
  - One-click copy reference code button with visual feedback.
  - 3-stage visual timeline: `Submitted` (done) → `Administrator review` (current) → `Account setup` (pending).
  - Quick action to *"Check application status"*.

### 2.6 Application Status Checker (`StatusPage.tsx`)
- **Lookup Query:**
  - Accepts user reference code (auto-cleaned and converted to uppercase `REQ-YYYY-XXXXX`).
  - Queries `GET /api/v1/onboarding-requests/status/{referenceCode}`.
- **Status Badges & Progress Visualization:**
  - Visual status badge and step timeline for all 4 backend lifecycle statuses:
    - `PENDING`: Under review, awaiting administrator verification.
    - `APPROVED`: Invitation sent, shop provisioned.
    - `REJECTED`: Application declined with support contact info.
    - `MORE_INFO_REQUESTED`: Reviewers requested clarifying information.
- **Details Grid:** Displays Shop Name, Owner Email, Requested Plan, and Submission Timestamp.
- **Action:** Reset button to check another application reference.

### 2.7 About & Contact Pages (`AboutPage.tsx` & `ContactPage.tsx`)
- **About Kuter:** Clear, honest product-focused explanation of why retail POS systems fail in the building materials industry. No fake historical anecdotes or fabricated founding teams.
- **Contact Inquiries:** Honest direct support email (`support@kuterplatform.com`) and contact form with name, email, and inquiry message.

---

## 3. Automated Test Suite & Production Build Verification

### 3.1 Vitest Unit & Integration Tests (`public/src/__tests__/`)

| Test File | Description | Tests Passed | Duration |
|---|---|:---:|:---:|
| `public-api.test.ts` | PublicApiClient tests: `getPublicPlans`, error propagation, `submitOnboardingRequest`, 409 duplicate email conflict, `getOnboardingStatus`, 404 not found handling | 6 / 6 | 11ms |
| `pricing.test.ts` | Pricing tests: 15% yearly discount calculation, quota indicators, features summary parsing | 3 / 3 | 8ms |
| `registration.test.ts` | Form validation: shop name length, email format regex, slug sanitizer, payload contract | 4 / 4 | 10ms |
| `status.test.ts` | Status tracker: reference code normalization, timeline stage mapping, badge styling | 3 / 3 | 6ms |
| **Total** | **All Public Website Vitest Suites** | **16 / 16** | **380ms** |

### 3.2 Production Build Verification

All three independent applications were compiled with zero errors:

```bash
# Public Marketing Website (public/)
npm run build
# Output: dist/assets/index-CxbSsaY1.js (290.32 kB), dist/assets/index-D7LWSpo-.css (25.79 kB) — Built in 3.51s

# Platform Admin Portal (admin/)
npm run build
# Output: dist/assets/index-uTHUmvaA.js (349.71 kB), dist/assets/index-C0glrSHW.css (16.13 kB) — Built in 4.02s

# Shop Tenant Portal (shop/)
npm run build
# Output: dist/assets/index-jLUNytci.js (268.24 kB), dist/assets/index-kiJn9ndS.css (5.73 kB) — Built in 6.07s
```

### 3.3 Backend API Health & Regression Verification

- **Spring Boot 3.4.3 Backend:** Running on port `8080`.
- **Demo Data Seeding:** Platform administrators (`admin@kuterplatform.com`), sample tenants (`al-madina-materials`, `summit-timber`), and onboarding requests (`REQ-2026-00001`) seeded.
- **Spring Boot Test Suite:** `AuthenticationApiTests` and `OnboardingWorkflowTests` passed 16/16 with 0 failures, 0 errors, and 0 skipped. Full suite: 172/172 tests passed.

---

## 4. Multi-Portal Port Mapping

| Service | Directory | Port | Primary Target Audience & Functions |
|---|---|---|---|
| **Spring Boot Backend** | `backend/` | `8080` | Multi-tenant REST APIs, Flyway migrations, PostgreSQL |
| **Shop Tenant Portal** | `shop/` | `5173` | Shop owners, yard managers, storekeepers, and cashiers |
| **Platform Admin Portal** | `admin/` | `5174` | Superadmins reviewing onboarding requests and managing plans |
| **Public Website** | `public/` | `5175` | Prospective merchants, public plan directory & registration |

---

## 5. Scope Boundary Adherence & Next Steps

In strict accordance with the Phase Nine instructions:
- **No Shop Operations in Public:** No shop inventory, stock ledger, sales counter, or checkout workflows were implemented in `public/`.
- **No Platform Admin Screens in Public:** The platform admin review queue and tenant suspension tools remain strictly in `admin/`.
- **No Simulated Payments:** Plans are presented as commercial preferences reviewed by administrators without fake Stripe/PayPal forms.
- **Halt Condition Met:** Phase Nine is complete. Execution is halted to await user review before beginning Phase Ten (**Shop Tenant Portal**).
