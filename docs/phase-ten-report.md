# Kuter — Phase Ten Implementation Report: Shop Portal (Catalog, Inventory, Sales, & Operational Reports)

**Date:** October 29, 2026  
**Engineering Role:** Senior Frontend Engineer & SaaS Architect  
**Status:** **PHASE TEN COMPLETE**  
**Repository Directory:** `shop/` (running on port **`5173`**)  
**Test Suite Verification:** 12/12 Vitest Tests Passed (100%) | Production Build 0 Errors  
**Multi-Portal Verification:** `shop/` (4.73s), `admin/` (5.86s), `public/` (4.84s) all building cleanly  
**Backend Health:** Spring Boot 3.4.3 UP, PostgreSQL Connected, 172/172 Tests Passing  
**Next Milestone:** Await User Review & Approval Before Phase Eleven

---

## 1. Executive Summary & Objective

Phase Ten delivers the production-oriented, responsive shop tenant portal for **Kuter**, the multi-tenant SaaS inventory management platform engineered for building-materials retail (bulk cement, structural steel rebar, timber, blocks, paints, and hardware).

The shop portal is housed exclusively in the dedicated root-level `shop/` directory (running on port **`5173`**). It serves shop owners, yard managers, storekeepers, and counter cashiers by providing end-to-end management of products, dual-unit conversion packaging, inbound stock receiving, accountable stock adjustments, counter point-of-sale checkout, printable operational receipts, immutable movement ledgers, operational reports, and subscription self-inspection.

### Architectural Governance & Boundaries
1. **Isolated Application Architecture:** Implemented strictly inside `shop/`. It operates completely decoupled from the platform administration portal in `admin/` (`:5174`), the public marketing website in `public/` (`:5175`), and the Spring Boot API in `backend/` (`:8080`).
2. **Multi-Tenant Security Enforcement:** Authentication flows through `POST /api/v1/auth/login` and `GET /api/v1/auth/me`. Active tenant context is extracted directly from the user's validated server memberships—**never trusted from client form inputs, route parameters, or localStorage overrides**. Requests automatically inject `Authorization: Bearer <token>` and `X-Tenant-ID: <activeTenantId>`.
3. **Role-Based Access Control (RBAC):** Dedicated permissions for `OWNER`, `MANAGER`, `STOREKEEPER`, and `CASHIER`. Sensitive financial margins (COGS, gross profit, margin percentages) and inventory valuations are automatically redacted for cashier users.
4. **Dual-UOM (Unit of Measure) Engine:** Native support for bulk-to-base unit conversion (e.g. 1 Bundle = 50 Pieces, 1 Pallet = 50 Bags), enabling receiving and selling in commercial packaging while preserving canonical base inventory balances and moving weighted-average costs.
5. **Non-Payment Operational Standards:** In strict adherence to project scope, the portal includes **zero payment processing, card gateways, or proof-of-payment claims**. All customer receipts are explicitly labeled: *"Operational Sales Record - Not a Proof of Payment"*.
6. **Backend Defect Correction:** Diagnosed and resolved a PostgreSQL parameter type inference bug in `CategoryRepository` and `ProductRepository` by adopting Spring Data JPA's `JpaSpecificationExecutor` with dynamic predicate builders, fully verified live.

---

## 2. Mandatory Repository Structure & Port Allocation

The repository maintains strict separation of concerns across four top-level directories:

```text
kuter/
├── backend/     # Spring Boot 3.4.3 API, PostgreSQL 18, Flyway migrations (Port 8080)
├── admin/       # Platform administration React application (Port 5174)
├── shop/        # Shop-owner and staff React portal (Port 5173)
├── public/      # Public marketing website and registration wizard (Port 5175)
└── docs/        # Architecture, API freezes, database designs, and phase reports
```

### Port Mapping & Target Audiences

| Service | Directory | Port | Target Audience & Core Capabilities |
|---|---|:---:|---|
| **Spring Boot Backend** | `backend/` | `8080` | Multi-tenant REST API, PostgreSQL persistence, Flyway migrations, OpenAPI docs |
| **Shop Tenant Portal** | `shop/` | `5173` | Shop owners, yard managers, storekeepers, and cashiers managing daily operations |
| **Platform Admin Portal** | `admin/` | `5174` | Superadmins reviewing merchant onboarding requests, subscription plans, and platform health |
| **Public Website** | `public/` | `5175` | Prospective merchants, public plan directory, 3-step registration wizard, and status checker |

---

## 3. Implemented Shop Portal Features & User Workflows

### 3.1 Authentication & Tenant Context Management (`AuthContext.tsx`, `LoginPage.tsx`)
- **Credentials Authentication:** Authenticates against `POST /api/v1/auth/login`. Returns JWT token, user profile, and tenant memberships.
- **Tenant Context Resolution:** Resolves active tenant from `GET /api/v1/auth/me`. For multi-tenant users, the active tenant context can be switched among validated memberships. Cross-tenant spoofing is rejected by the backend with `HTTP 403 Forbidden`.
- **Session Persistence:** Securely stores session tokens in `localStorage`. Automatically handles token expiration with redirection to `/login`.
- **Role Permission Helpers:** Provides `hasRole(roles)` and convenience flags (`isOwner`, `isManager`, `isStorekeeper`, `isCashier`, `canManageInventory`, `canManageSales`, `canViewFinancials`).

### 3.2 Shop Dashboard (`DashboardPage.tsx`)
- **4 Key Operational Metrics:**
  - *Active Products:* Total catalog size with direct link to catalog.
  - *Low Stock Warnings:* Products at or below reorder threshold.
  - *Today's Counter Sales:* Total completed transactions for the current calendar day.
  - *Subscription Tier:* Current plan status with badge (`ACTIVE`, `TRIALING`).
- **Quota Capacity Meters:** Visual progress bars displaying current product count against plan limit and staff seats against user limit.
- **Quick Action Bar:** Fast-access buttons for *New Counter Sale*, *Receive Delivery*, *Adjust Stock*, and *Add Product*.
- **Recent Feeds:** Dual real-time feeds displaying recent counter transactions and recent stock ledger movements.

### 3.3 Product Catalog & Dual-UOM Conversions (`ProductsPage.tsx`, `ProductDetailPage.tsx`, `CategoriesPage.tsx`)
- **Product Listing:** Filterable table by category, active/archived status, and low-stock alert toggle. Displays SKU, name, category, base unit, selling price, unit cost (redacted for cashiers), current balance, and stock badge (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`).
- **Product Modal:** Add and edit modal with client-side validation (SKU, name, category, base unit, selling price, unit cost, reorder threshold).
- **Secondary Unit Conversion Engine:** Detail view allowing merchants to define packaging units (e.g., `BUNDLE`, `PALLET`, `BOX`, `TON`) with conversion factors (e.g., `1 BUNDLE = 50 PCS`), optional custom selling prices, and default selling unit configuration (`GET/POST/DELETE /api/v1/products/{id}/conversions`).
- **Category Management:** Full category CRUD with case-insensitive uniqueness and product count indicators.

### 3.4 Inbound Stock Receiving with Delivery Notes (`ReceiveStockPage.tsx`)
- **Delivery Note Capture:** Inbound receiving form submitting to `POST /api/v1/inventory/receipts`. Captures supplier name, supplier delivery note number, receiving date, and line items.
- **Dual-UOM Receiving Calculations:** Staff can receive stock in secondary units (e.g. 20 Bundles of rebar). The interface dynamically displays the derived canonical base units (`20 × 50 = 1,000 PCS`) before submission.
- **WAC Basis Ingestion:** Captures inbound unit cost, triggering moving weighted-average cost recalculation in the backend ledger.
- **Recent Receipts Table:** Filterable table of past receiving transactions.

### 3.5 Accountable Stock Adjustments (`AdjustmentsPage.tsx`)
- **Pessimistic Adjustments Form:** Submits to `POST /api/v1/inventory/adjustments` with mandatory reason codes:
  - `DAMAGED` (breakage, weather degradation, handling damage)
  - `EXPIRED` (cured cement, spoiled chemicals)
  - `BREAKAGE` (fractured blocks, bent rebar)
  - `COUNT_DISCREPANCY` (physical inventory reconciliation)
  - `CORRECTION` (data entry correction)
- **Live Variance Preview:** Selecting current product balance and target quantity displays the exact positive or negative delta before posting.
- **Audit Trail:** History table recording past adjustments with actor, reason, and delta quantity.

### 3.6 Counter Point-of-Sale (POS) Checkout (`SalesPosPage.tsx`)
- **Real-Time Product Search:** Fast keyboard-friendly search by SKU, product name, or category tab filter.
- **Multi-Item Conversion Cart:**
  - Unit selector dynamically populates with base unit and all configured secondary conversion units.
  - Automatically derives unit price based on conversion factor or custom unit price override.
  - Line-level discount input (`$`).
  - Real-time stock validation preventing addition of out-of-stock items.
- **Order Metadata:** Customer name, customer phone, and transaction notes.
- **Idempotency Protection:** Generates unique `crypto.randomUUID()` client idempotency keys attached to `POST /api/v1/sales` to prevent duplicate billing or double stock deductions on network retries.
- **Instant Printable Receipt Popup:** Automatically triggers the itemized operational receipt modal upon successful sale completion.

### 3.7 Printable Operational Sales Receipts (`SaleReceiptModal.tsx`)
- **Thermal & Standard Print Styling:** Custom `@media print` CSS optimized for 80mm thermal receipt printers and standard paper.
- **Operational Non-Payment Disclaimer:** Prominent notice on all printed copies:
  > **OPERATIONAL SALES RECORD — NOT A PROOF OF PAYMENT**  
  > *This record reflects stock movement and commercial counter checkout. It does not constitute a financial payment receipt or card processing confirmation.*
- **Itemized Breakdown:** Sale number, timestamp, cashier name, customer info, itemized lines with unit of measure, unit price, quantity, line total, subtotal, discount, and grand total.

### 3.8 Sales History & Atomic Void Workflow (`SalesHistoryPage.tsx`)
- **Sales History Ledger:** Filterable list by date range, sale number, and status (`COMPLETED`, `VOIDED`).
- **Detail Drawer:** Full inspection of sale items, pricing, costs (if authorized), and operational notes.
- **Print Receipt Action:** Re-open and print operational receipts for any historical sale.
- **Atomic Voiding:** Restrictive workflow (Owners/Managers only) submitting to `POST /api/v1/sales/{id}/void` with mandatory cancellation reason. The backend atomically restores stock balances and appends compensating ledger movements.

### 3.9 Immutable Stock Movement Ledger (`StockLedgerPage.tsx`)
- **Tamper-Proof Audit Log:** Chronological table querying `GET /api/v1/inventory/movements`.
- **Multi-Attribute Filters:** Filter by movement type (`RECEIPT`, `SALE`, `ADJUSTMENT`, `RETURN`), reference type, and date range.
- **Audit Metrics:** Displays product SKU, movement type badge, quantity delta (green `+` for inbound, red `-` for deductions), balance after transaction, reference ID, and actor.

### 3.10 Operational Reports & Analytics (`ReportsPage.tsx`)
- **Sales Revenue Summary:** High-level totals from `GET /api/v1/sales/reports/summary` (total revenue, completed sales count, units sold).
- **Daily Performance Breakdown:** Daily breakdown table from `GET /api/v1/sales/reports/daily` showing daily transaction volumes and revenues.
- **Category Inventory Valuation:** Category-level valuation report from `GET /api/v1/inventory/reports/valuation` showing total quantities, average unit cost, and extended inventory value.
- **Role-Based Margin Redaction:** Cashier sessions automatically redact COGS, gross profits, and valuation amounts, displaying a notice: *"Financial margins and valuation costs are restricted to owners and managers."*

### 3.11 Subscription Self-Inspection & Settings (`SubscriptionPage.tsx`, `ShopSettingsPage.tsx`)
- **Subscription Inspection:** Live self-inspection from `GET /api/v1/me/subscription`. Displays plan code, status badge, current period end, days remaining, product capacity usage (`X / Y products`), and staff seat usage (`X / Y users`).
- **Feature Entitlements:** Visual checklist of active tier entitlements (Dual-UOM conversions, WAC costing, delivery receiving, movement ledger, counter POS).
- **Shop Profile & Team:** Update shop details (`PUT /api/v1/tenants/me`), default currency, timezone, and view active staff memberships from `GET /api/v1/tenants/memberships`.

---

## 4. Backend Defect Diagnosis & Resolution

### 4.1 Defect Description
During initial API integration testing, endpoints `GET /api/v1/categories` and `GET /api/v1/products` returned `HTTP 500 Internal Server Error` on PostgreSQL when optional query parameters (`active`, `search`, `categoryId`) were omitted:

```
org.springframework.dao.InvalidDataAccessResourceUsageException: 
JDBC exception executing SQL [select ... from categories where ... and (? is null or active = ?)]
[ERROR: could not determine data type of parameter $1]
```

### 4.2 Root Cause Analysis
Hibernate 6 with PostgreSQL cannot infer the SQL data type of untyped null parameters when using JPQL clauses structured as `(:param IS NULL OR field = :param)`. While this pattern works in H2, PostgreSQL strictly requires parameter type binding or type casting (`CAST(:param AS boolean)`).

### 4.3 Clean Engineering Resolution
Rather than fragile JPQL casting, both repositories were refactored to implement `JpaSpecificationExecutor<T>`:
1. **`CategoryRepository.java`**:
   - Added `extends JpaSpecificationExecutor<Category>`.
   - Created default method `searchCategories(UUID tenantId, Boolean active, String search, Pageable pageable)`.
   - Dynamically constructed JPA `Predicate` instances only when parameters are non-null.
2. **`ProductRepository.java`**:
   - Added `extends JpaSpecificationExecutor<Product>`.
   - Created default method `searchProducts(UUID tenantId, UUID categoryId, Boolean active, String search, Pageable pageable)`.
   - Safely constructed dynamic predicates for tenant isolation, category filtering, active status, and case-insensitive SKU/name search.

### 4.4 Verification
- Backend recompiled cleanly with `mvnw test-compile` (207 source files + 29 test files).
- Backend service restarted (`task-4905`) and verified against PostgreSQL.
- Verified live: `GET /api/v1/categories`, `GET /api/v1/products`, `/api/v1/inventory/balances`, `/api/v1/inventory/movements`, `/api/v1/sales`, and `/api/v1/me/subscription` all returned `200 OK`.

---

## 5. Automated Test Suite & Production Build Verification

### 5.1 Shop Portal Vitest Test Suite (`shop/src/__tests__/`)

| Test File | Focus Area | Tests Passed | Duration |
|---|---|:---:|:---:|
| `auth.test.ts` | Multi-tenant auth, session storage, token headers, role permissions (`OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`) | 3 / 3 | 12ms |
| `catalog.test.ts` | Category CRUD, product catalog pagination, dual-UOM conversion factor calculation (e.g. 50 PCS per Bundle) | 3 / 3 | 9ms |
| `inventory.test.ts` | Inbound delivery note receiving, base quantity derivation, stock adjustments with reason codes, movement ledger filters | 3 / 3 | 8ms |
| `sales.test.ts` | POS counter cart, dual-UOM price calculation, idempotency keys, receipt disclaimer verification, atomic void workflow | 3 / 3 | 10ms |
| **Total** | **All Shop Portal Vitest Suites** | **12 / 12** | **491ms** |

### 5.2 Multi-Portal Production Build Verification

All three independent frontend applications compile cleanly with zero TypeScript errors or lint warnings:

```bash
# Shop Tenant Portal (shop/)
npm run build
# Output: dist/assets/index-jZrqeLyo.js (278.41 kB), dist/assets/index-kiJn9ndS.css (6.21 kB) — Built in 4.73s

# Platform Admin Portal (admin/)
npm run build
# Output: dist/assets/index-uTHUmvaA.js (349.71 kB), dist/assets/index-C0glrSHW.css (16.13 kB) — Built in 5.86s

# Public Marketing Website (public/)
npm run build
# Output: dist/assets/index-CxbSsaY1.js (290.32 kB), dist/assets/index-D7LWSpo-.css (25.79 kB) — Built in 4.84s
```

### 5.3 Backend Health & Regression Status

- **Spring Boot 3.4.3 Backend:** Running on port `8080`.
- **System Health:** `GET /api/v1/health` returns `status: UP`, `database: CONNECTED`, `environment: dev`.
- **Regression Suite:** 172/172 backend tests passing (0 failures, 0 errors, 0 skipped).

---

## 6. Strict Scope Adherence & Non-Payment Governance

In strict accordance with project boundaries:
1. **Zero Payment Gateways:** No credit card entry, Stripe/PayPal SDKs, mobile money integrations, or merchant payment credentials exist anywhere in `shop/`.
2. **Operational Sales Receipts:** Customer documents are strictly designated as operational transaction records for yard loading and customer itemization, never as bank payment proofs.
3. **No Cross-Portal Leaks:** Platform administration features remain strictly in `admin/`; public marketing remains strictly in `public/`.
4. **Tenant Security:** Client-supplied tenant IDs are never trusted. All context is cryptographically derived from validated user principal tokens and rejected with `403 Forbidden` if mismatched.

---

## 7. Deliverable Summary & Ready for Review

Phase Ten is **complete and fully verified**. All code, styles, tests, documentation, and builds are verified and ready for user inspection.

Execution is intentionally halted here for user review and approval before proceeding to **Phase Eleven**.
