# Kuter — Development Progress & Roadmap

## Phase Checklist

| Phase | Description | Status | Evidence / Verification | Next Milestone |
| :---: | :--- | :---: | :--- | :--- |
| **Phase 1** | **Foundation, Architecture, Environment, & Verification** | **COMPLETED** | - Root structure initialized (`backend/`, `admin/`, `frontend/`, `docs/`)<br/>- Amoudi reference inspected & conventions documented<br/>- Figma design archive inspected (`docs/design-reference/`)<br/>- Spring Boot 3.4.3 backend verified with Java 21<br/>- Profiles (`dev`, `prod`, `test`) configured & verified<br/>- PostgreSQL 18 & Flyway baseline migration (`V1__initial_schema.sql`) executed<br/>- 7/7 backend unit/integration tests passed<br/>- `admin` and `frontend` React 19 apps built cleanly via Vite<br/>- Live REST API connectivity verified with CORS protection | Proceed to Phase 2 |
| **Phase 2** | **Backend Domain Architecture, Database Design, & Security Foundation** | **COMPLETED** | - Domain models & repositories created across 8 modules (tenancy, identity, subscriptions, onboarding, catalog, inventory, sales, audit)<br/>- Flyway `V2__core_domain_schema.sql` (19 tables) applied with strict UUID PKs and `NUMERIC(15, 4)` scale<br/>- Dual units of measure modeled with unit conversion multipliers<br/>- `TenantContext` & `TenantContextFilter` implemented with cross-tenant spoofing rejection (HTTP 403)<br/>- Spring Security 6 configured with BCrypt, `KuterUserPrincipal`, multi-tenant roles, and platform admin isolation<br/>- 24/24 backend tests passed (0 failures, 0 skipped)<br/>- React builds verified clean (`admin` 3.33s, `frontend` 3.74s) | Proceed to Phase 3 |
| **Phase 3** | **Authentication, Tenant Onboarding, & Tenant-Management APIs** | **COMPLETED** | - Flyway `V3__onboarding_enhancements.sql` applied on PostgreSQL 18.6 and H2<br/>- Stateless JWT authentication engine implemented using JJWT 0.12.6 (HMAC-SHA256)<br/>- `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`, `GET /api/v1/users/me`<br/>- Public onboarding registration (`POST /api/v1/onboarding-requests`) without initial password (aligned with Figma)<br/>- Applicant public status tracking (`GET /api/v1/onboarding-requests/status/{ref}`)<br/>- Platform admin review, pagination, search, approval, and rejection APIs (`/api/v1/admin/onboarding-requests/**`)<br/>- Direct tenant creation, listing, detail, and suspension APIs (`/api/v1/admin/tenants/**`)<br/>- Tenant settings (`/api/v1/tenants/me`) and staff membership CRUD (`/api/v1/tenants/{tenantId}/members/**`)<br/>- **48/48 backend tests passed** (0 failures, 0 errors, 0 skipped)<br/>- React builds verified clean (`admin` 2.69s, `frontend` 2.63s) | Proceed to Phase 4 (Catalog & Dual-UOM Engine) |
| **Phase 4** | **Product Catalog and Inventory Management APIs** | **COMPLETED** | - Category CRUD with case-insensitive tenant uniqueness & soft-deactivation<br/>- Unit definitions auto-seeding standard building units (`PCS`, `BAG`, `TON`, etc.)<br/>- Product catalog with SKU isolation, base units, image metadata & storage keys<br/>- Dual-UOM product-specific unit conversions (e.g. 1 PALLET = 50 BAG)<br/>- Inbound stock receiving with moving weighted-average cost (WAC) engine<br/>- Stock adjustments (shrinkage, breakage, target physical count audit reconciliations)<br/>- Negative stock prevention and cost preservation on downward adjustments<br/>- Pessimistic write locking (`SELECT ... FOR UPDATE`) on stock balances<br/>- Idempotency key replay protection on receipts and adjustments<br/>- Live inventory balances and immutable stock movement ledger<br/>- Valuation and low-stock replenishment reports<br/>- **80/80 backend integration tests passing** (0 failures, 0 skipped)<br/>- React builds verified clean (`admin` and `frontend` Vite builds 0 errors) | Proceed to Phase 5 (Sales, POS Counter Checkout & Invoicing) |
| **Phase 5** | **Counter Sales, POS Checkout & Invoicing APIs** | **COMPLETED** | - Flyway `V5__sales_enhancements.sql` (6 new columns, 5 indexes) applied on PostgreSQL 18.6 and H2<br/>- Atomic counter sales processing (`POST /api/v1/sales`) with multi-item cart, dual-UOM conversion, line discounts, and custom pricing<br/>- Pessimistic write locking with canonical UUID lock ordering preventing race conditions and deadlocks<br/>- Immutable moving WAC cost capture on line items and sale totals<br/>- Sale voiding (`POST /api/v1/sales/{id}/void`) with stock balance restoration, WAC recalculation, and compensating `RETURN`/`SALE_VOID` ledger movements<br/>- Idempotency key replay protection (`uk_sales_tenant_idempotency`)<br/>- Operational printable receipt generation (`GET /api/v1/sales/{id}/receipt`) with non-payment disclaimer<br/>- Multi-tenant sales summary (`GET /api/v1/sales/reports/summary`) and product ranking (`GET /api/v1/sales/reports/products`) reports with cashier cost redaction<br/>- Role-based security enforcing cashier void restriction and sensitive cost hiding<br/>- **106/106 backend tests passing** (26 sales tests, 0 failures, 0 skipped)<br/>- React builds verified clean (`admin` 5.06s, `frontend` 5.85s) | Proceed to Phase 6 (Subscriptions, Billing Quotas & Merchant Limits) |
| **Phase 6** | **Subscriptions, Billing Quotas & Merchant Limits** | **COMPLETED** | - Flyway `V6__subscription_enhancements.sql` applied on PostgreSQL 18.6 and H2<br/>- Subscription plan management CRUD (`/api/v1/subscription-plans/**`) with validation and code uniqueness<br/>- Public read-only plan listing (`GET /api/v1/public/subscription-plans`) without credentials<br/>- Tenant subscription assignment, lifecycle, and status management (`PENDING`, `TRIALING`, `ACTIVE`, `EXPIRED`, `SUSPENDED`, `CANCELLED`, `PAST_DUE`)<br/>- Server-side real-time effective status resolution (`getEffectiveStatus`) using injectable `Clock`<br/>- Quota limit enforcement: Product limit (409 Conflict) and User seat limit (409 Conflict)<br/>- Active subscription gating on sales, catalog, and memberships (403 Forbidden)<br/>- Plan downgrade data preservation (zero data deletion; new additions blocked)<br/>- Platform admin lifecycle management (assign, activate, trial, change-plan, extend with reason, suspend, reactivate, cancel)<br/>- Platform subscription summary metrics (`GET /api/v1/admin/subscriptions/summary`) with estimated MRR<br/>- Tenant self-inspection (`GET /api/v1/me/subscription`) and tenant history ledger<br/>- Immutable audit history ledger (`subscription_history`)<br/>- **141/141 backend tests passing** (35 subscription tests, 0 failures, 0 skipped)<br/>- React builds verified clean (`admin` 3.29s, `frontend` 3.63s) | Proceed to Phase 7 (Backend Reports, Audit, Operational Features, Comprehensive Testing, and API Freeze) |
| **Phase 7** | **Backend Reports, Audit, Operational Features, Comprehensive Testing, and API Freeze** | **COMPLETED** | - Flyway `V7__reports_and_audit_enhancements.sql` applied on PostgreSQL 18.6 & H2 (outcome column + 5 composite indexes)<br/>- Dual-UOM current inventory report (`GET /api/v1/inventory/reports/current`) with integral breakdown & stock status (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`)<br/>- Detailed stock valuation report (`GET /api/v1/inventory/reports/valuation/items`) with item-by-item breakdown and moving WAC basis<br/>- Movement ledger report (`GET /api/v1/inventory/movements`, alias `/reports/movements`) with date-range, reference type, and actor filtering<br/>- Daily interval sales report (`GET /api/v1/sales/reports/daily`) with daily aggregates & cashier cost redaction<br/>- Sales listing (`GET /api/v1/sales`) with `productId` filter<br/>- Authorized audit log query engine (`GET /api/v1/audit-logs` tenant-scoped, `GET /api/v1/admin/audit-logs` platform admin) with full multi-attribute filtering & outcome tracking<br/>- Standardized UTC date-range parsing & validation (`DateTimeUtils`) and pagination/sort safety<br/>- Platform overview aggregate metrics (`GET /api/v1/admin/metrics/overview`)<br/>- Decoupled operational status vs subscription lifecycle (`GET /api/v1/admin/tenants/{id}/status`, `GET /api/v1/tenants/me/status`)<br/>- SpringDoc OpenAPI 3.0 & Swagger UI documentation (`/v3/api-docs`, `/swagger-ui/index.html`) with JWT Bearer security scheme<br/>- **172/172 backend tests passing** (31 new tests, 0 failures, 0 errors, 0 skipped)<br/>- Fully frozen and documented REST API contract (`docs/api/frozen-contract.md`) | Proceed to Phase 8 (Platform Admin Portal) |
| **Phase 8** | **Platform Admin Portal** | **COMPLETED** | - Dedicated React 19 + TypeScript + Vite app in `admin/` on port 5174<br/>- Authenticated against `/api/v1/auth/login` with strict `ROLE_PLATFORM_ADMIN` verification<br/>- Onboarding approval queue with custom slug generation & mandatory rejection reason<br/>- Tenant directory, operational suspension/reactivation with audit logging<br/>- Subscription plan management (CRUD, quotas, deletion integrity defense)<br/>- Tenant subscription assignment, extension with reason, plan changes, and audit ledger<br/>- Multi-attribute audit log inspection engine with date ranges and outcome filters<br/>- **23/23 Vitest automated tests passing** in `admin/`<br/>- Production builds clean in 2.77s | Proceed to Phase 9 (Public Website & Registration) |
| **Phase 9** | **Public Website, Subscription Plans, and Shop Registration** | **COMPLETED** | - Dedicated React 19 + TypeScript + Vite app in `public/` adhering to Figma design system<br/>- Dynamic subscription plans fetched live from `GET /api/v1/public/subscription-plans`<br/>- Monthly/Yearly toggle with 15% discount calculation and quota indicators<br/>- 3-step registration wizard submitting to `POST /api/v1/onboarding-requests` with validation<br/>- Review-based access banner (no upfront passwords requested)<br/>- Success confirmation view with reference copy and visual timeline<br/>- Application status tracker querying `GET /api/v1/onboarding-requests/status/{ref}`<br/>- **16/16 Vitest automated unit tests passing** in `public/`<br/>- Production builds verified clean for `public/` (3.51s), `admin/` (4.02s), and `shop/` (6.07s)<br/>- **172/172 backend regression tests passing** | Proceed to Phase 10 (Shop Tenant Portal) |
| **Phase 10** | **Shop Tenant Portal (React)** | **COMPLETED** | - Dedicated React 19 + TypeScript + Vite app in `shop/` on port 5173<br/>- Multi-tenant authentication (`/api/v1/auth/login`, `/api/v1/auth/me`) with `X-Tenant-ID` scoping<br/>- Role-based views: `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`<br/>- Catalog & Dual-UOM packaging conversions (`/products/{id}/conversions`)<br/>- Inbound Delivery Note receiving (`/inventory/receipts`) with live conversion calculation<br/>- Stock adjustments (`/inventory/adjustments`) with mandatory reason codes<br/>- Counter POS checkout (`/sales`) with multi-item cart, price overrides, and idempotency protection<br/>- Printable operational sales receipts (`/sales/{id}/receipt`) with non-payment disclaimer<br/>- Sales history & atomic void workflow (`/sales/{id}/void`)<br/>- Immutable movement ledger (`/inventory/movements`)<br/>- Sales analytics & inventory valuation reports with cashier financial redaction<br/>- Subscription self-inspection (`/me/subscription`) & shop settings (`/tenants/me`)<br/>- **12/12 Vitest automated unit tests passing** in `shop/`<br/>- Production builds clean for `shop/` (4.73s), `admin/` (5.86s), `public/` (4.84s) | Proceed to Phase 11 (Full-System Integration & Verification) |
| **Phase 11** | **Full-System Integration, End-to-End Testing, Security Review, & Production Readiness** | **COMPLETED** | - End-to-end 21-step integration test suite (`FullSystemEndToEndIntegrationTests.java`) validating complete lifecycle<br/>- CORS configuration updated supporting public web origin (`http://localhost:5175`)<br/>- Admin Vite reverse-proxy mapped to `/api` on backend port `:8080`<br/>- Product dual-UOM `/conversions` alias mapped to `/units` preserving frozen API contract<br/>- DTO interoperability aliases added (`toBaseFactor`, `currentBalance`, `balanceAfter`, `quantityDelta`, `operationalDisclaimer`)<br/>- Tenant members endpoint (`GET /api/v1/tenants/me/members`) added for shop portal team management<br/>- Multi-tenant security audit: cryptographic JWT verification, mandatory `X-Tenant-ID` membership validation, cross-tenant IDOR/DOR rejection (404/403)<br/>- Cashier financial redaction verified (COGS, gross profit, margin percentage omitted from payloads)<br/>- **173/173 backend tests passing** (0 failures, 0 skipped)<br/>- **23/23 admin tests passing**<br/>- **12/12 shop tests passing**<br/>- **16/16 public tests passing**<br/>- **Total: 224/224 automated tests passing (100% pass rate)**<br/>- Clean production builds for all 3 frontends (`admin` 3.10s, `shop` 3.40s, `public` 2.46s)<br/>- Production deployment readiness confirmed | Proceed to Critical Addendum |
| **Critical Addendum** | **Shop Users, Permissions, Inter-Shop Lending, and Owner-Controlled Pricing** | **COMPLETED** | - 4-account limit (1 Owner + 3 Staff) with pessimistic locking and plan capacity validation<br/>- Non-delegable owner privileges (`PRICES_MANAGE`, `USERS_MANAGE`) & 14 granular permissions (`ShopPermission`)<br/>- Owner-controlled selling prices (`PATCH /products/{id}/selling-price`) and POS price tampering rejection (400 Bad Request)<br/>- Single-tenant Inter-Shop Lending & Borrowing with external counterparties, physical ledger movements, multi-step partial returns, and obligations summary<br/>- Flyway `V8__shop_users_permissions_and_lending.sql` applied cleanly on H2 and PostgreSQL 18.6<br/>- 13 new integration tests passing (186/186 backend tests total)<br/>- Shop frontend updated with 4-seat capacity meter, permission management, ownership transfer modal, and complete Lending & Borrowing module<br/>- **237/237 automated tests passing** across full system (186 backend, 23 admin, 12 shop, 16 public) | Addendum Complete; Await Final Review |

## Detailed Critical Requirements Addendum Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Shop User Limit** | 4-Account Limit (1 Owner + 3 Staff) | Verified | Enforced via pessimistic write lock (`findByIdForUpdate`) on `Tenant` in `TenantMembershipServiceImpl.java` and `EntitlementServiceImpl.java`; standard plans capped at 4 total accounts; Micro capped at 2; verified in `ShopUserLimitAndPermissionsTests.java` |
| **Seat Reactivation** | Capacity Validation on Reactivation | Verified | Reactivating a suspended account checks active account capacity; rejects with HTTP 409 Conflict if shop already has 4 active seats; verified in unit & integration tests |
| **Owner Privileges** | Non-Delegable Price & User Controls | Verified | `PRICES_MANAGE` and `USERS_MANAGE` strictly forbidden from being assigned to non-owner staff (HTTP 400 Bad Request); verified in `ShopUserLimitAndPermissionsTests.java` |
| **Ownership Transfer** | Secure Ownership Handshake | Verified | `POST /api/v1/tenants/me/transfer-ownership` transfers primary ownership to another active member; verifies current owner password; automatically demotes outgoing owner to `MANAGER`; protects last owner from deletion or demotion |
| **Permissions Engine** | 14 Fine-Grained Permissions | Verified | `ShopPermission` enum mapped to `tenant_membership_permissions` table; Spring Security `@tenantSecurity.hasPermission('...')` evaluation; safe role defaults seeded in Flyway `V8` |
| **Pricing Control** | Owner-Only Selling Price Modification | Verified | `PATCH /api/v1/products/{id}/selling-price` guarded by `PRICES_MANAGE` permission; non-owner staff attempts return HTTP 403 Forbidden; audits `PRODUCT_PRICE_CHANGED`; verified in `OwnerControlledPricingTests.java` |
| **POS Price Integrity** | Tamper-Proof Checkout Validation | Verified | `SaleServiceImpl.java` validates client-supplied line unit prices against owner's catalog selling price (with conversion factors applied); cashiers/staff attempting price overrides rejected with HTTP 400 Bad Request; verified in `OwnerControlledPricingTests.java` |
| **Inter-Shop Lending** | Simplified External Counterparty Design | Verified | Single-tenant recording of incoming borrowings (`POST /api/v1/inventory/borrowings`) and outgoing loans (`POST /api/v1/inventory/loans`) referencing local product SKUs against external counterparties |
| **Stock Adjustments** | Ledger Movement Integration | Verified | Outgoing loans decrease stock with `LOAN_DISPATCH`; incoming borrowings increase stock with `BORROW_RECEIPT`; returns trigger `LOAN_RETURN_RECEIPT` or `BORROW_RETURN_DISPATCH`; zero sales revenue/invoices created |
| **Partial Returns** | Multi-Step Return Tracking | Verified | `POST /api/v1/inventory/lending/{id}/returns` records partial/full returns in `stock_loan_returns`; rejects over-returns (HTTP 400); checks physical balance before returning borrowed goods; verified in `InventoryLendingAndBorrowingTests.java` |
| **Loan Cancellation** | Reversal of Unreturned Loans | Verified | `POST /api/v1/inventory/lending/{id}/cancel` allows cancellation if 0 returns made; reverses physical stock balance cleanly |
| **Obligations Summary** | Net Position per Product SKU | Verified | `GET /api/v1/inventory/lending/obligations` aggregates active borrowings, active loans, and net obligation per SKU (positive = we owe external shop; negative = external shop owes us) |
| **Database Migration** | Flyway V8 Schema Migration | Verified | `V8__shop_users_permissions_and_lending.sql` applied cleanly across PostgreSQL 18.6 and H2; cascading foreign keys on tenant, product, and unit |
| **Shop UI Updates** | Staff, POS, Catalog & Lending Modules | Verified | `ShopSettingsPage.tsx` with 4-seat capacity meter, permission checkboxes, and ownership transfer modal; `SalesPosPage.tsx` with locked price inputs for non-owners; `ProductsPage.tsx` with owner price update modal; `LendingPage.tsx` with full borrowing, loan, return, and obligations tracking |
| **Automated Testing** | 100% Pass Rate Across Full System | Verified | **186/186 backend tests passing**; **23/23 admin tests passing**; **12/12 shop tests passing**; **16/16 public tests passing**; **Total: 237/237 tests passing (100% green)** |

## Detailed Phase Eleven Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **System Architecture** | 4 Decoupled Root Applications | Verified | `backend/` (Spring Boot :8080), `admin/` (React :5174), `shop/` (React :5173), `public/` (React :5175); zero leaked runtime state; reverse-proxies configured in all Vite apps |
| **Cross-App Workflow** | 21-Step End-to-End Workflow | Verified | Verified in `FullSystemEndToEndIntegrationTests.java`: Public visitor registers -> Admin approves -> Tenant/User/Subscription provisioned -> Owner logs in -> Staff roster checked -> Catalog category & product created -> Dual-UOM configured (1 BNDL = 50 PCS) -> Secondary unit receiving (4 BNDL = 200 PCS @ $250) -> Moving WAC computed ($5.00) -> Breakage adjustment (-10 PCS -> 190 PCS) -> POS counter sale (2 BNDL + 5 PCS = 105 PCS -> 85 PCS) with idempotency key -> Operational receipt generated with non-payment disclaimer -> Sale voided with stock restoration back to 190 PCS -> Immutable movement ledger verified -> Cashier RBAC tested (void forbidden 403, financial summary redacted) -> Foreign tenant IDOR/DOR blocked (404/403) |
| **CORS Security** | Multi-Origin Whitelisting | Verified | Added `http://localhost:5175` to default origins in `application.yml` and `CorsConfig.java`; credentials, methods (GET, POST, PUT, DELETE, PATCH, OPTIONS), headers (`Authorization`, `Content-Type`, `X-Tenant-ID`, `X-Correlation-ID`) permitted |
| **Reverse Proxy** | Vite API Proxy Uniformity | Verified | Configured `/api` reverse proxy in `admin/vite.config.ts`, `shop/vite.config.ts`, and `public/vite.config.ts` targeting `http://localhost:8080` |
| **API Contract** | Dual-UOM Endpoint Interoperability | Verified | Mapped `@GetMapping`, `@PostMapping`, and `@DeleteMapping` with `{"/{productId}/units", "/{productId}/conversions"}` in `ProductController.java` to fulfill frozen API contract |
| **DTO Interop** | Client-Backend Model Alignment | Verified | Added backward/forward-compatible getters/setters: `toBaseFactor` in `CreateProductUnitConversionDto` & `ProductUnitConversionResponse`, `currentBalance` & `movingAverageCost` in `StockBalanceResponse`, `balanceAfter` & `quantityDelta` in `StockAdjustmentItemResponse`, `operationalDisclaimer` in `SaleReceiptResponseDto` |
| **Team Management** | Tenant Members Roster Endpoint | Verified | Implemented `GET /api/v1/tenants/me/members` in `TenantController.java` returning `TenantMemberDetailResponse[]` for the active tenant; wired in `shop/src/api/client.ts` and rendered in `ShopSettingsPage.tsx` |
| **Idempotency** | POS Checkout Replay Protection | Verified | Counter sale creation enforces `uk_sales_tenant_idempotency`; replaying the same idempotency key returns the existing sale transaction without deducting double inventory stock |
| **Operational Receipt** | Non-Payment Disclaimer | Verified | Operational receipt payload enforces disclaimer: *"Operational Sales Record - Not a Proof of Payment"*; zero payment card capture, billing webhooks, or proof-of-payment claims across system |
| **Multi-Tenancy** | IDOR / DOR Isolation Defenses | Verified | `TenantContextFilter` inspects `X-Tenant-ID` header; verifies authenticated user possesses active membership in requested tenant; spoofed requests rejected with `403 Forbidden`; direct access to foreign tenant resources returns `404 Not Found` |
| **RBAC Redaction** | Cashier Financial Segregation | Verified | `SalesSummaryReportDto` omits `totalCostOfGoodsSold`, `grossProfit`, and `profitMarginPercentage` via `@JsonInclude(NON_NULL)` when requested by `ROLE_TENANT_CASHIER`; verified in unit & E2E tests |
| **Automated Tests** | Backend Test Suite | Verified | **173/173 tests passing** (0 failures, 0 errors, 0 skipped, 1m elapsed) |
| **Automated Tests** | Admin Portal Vitest Suite | Verified | **23/23 tests passing** in 688ms |
| **Automated Tests** | Shop Portal Vitest Suite | Verified | **12/12 tests passing** in 442ms |
| **Automated Tests** | Public Website Vitest Suite | Verified | **16/16 tests passing** in 296ms |
| **Total Test Count** | Full-System Automated Suite | Verified | **224/224 automated tests passing (100% pass rate)** |
| **Production Builds** | Frontend TypeScript & Vite Bundles | Verified | `admin/` built in 3.10s (0 errors); `shop/` built in 3.40s (0 errors); `public/` built in 2.46s (0 errors) |
| **Database Isolation** | Clean Test Execution & DB Teardown | Verified | Added comprehensive `cleanupDatabase()` in `FullSystemEndToEndIntegrationTests` executed in `@BeforeEach` and `@AfterEach` to eliminate cross-test foreign key constraint violations |
| **Production Ready** | Environment & Operational Health | Verified | Health check endpoint `/api/v1/health` live (`status: UP`, `database: CONNECTED`); Graceful shutdown, Hikari pool tuning, Flyway validation in `application-prod.yml`; `.env.example` templates complete across all 4 directories |

## Detailed Phase Ten Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Architecture** | React 19 + TypeScript + Vite | Verified | Dedicated production application in `shop/` running on port 5173 with proxy to Spring Boot on `:8080`; zero shared runtime state with `admin/` or `public/` |
| **Authentication** | Tenant Session & Header Scoping | Verified | `POST /api/v1/auth/login` and `GET /api/v1/auth/me`; active tenant context extracted directly from validated server memberships; `X-Tenant-ID` header injected on all requests; verified in `auth.test.ts` |
| **RBAC** | Role-Based Access Controls | Verified | `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER` segregation; sensitive margins and unit costs redacted for Cashier; sales voiding restricted to Owner/Manager |
| **Dashboard** | Shop Overview & Quota Meters | Verified | 4 KPI cards (Total Products, Low Stock Alerts, Today's Sales Volume, Subscription Plan); product count and staff seat quota progress bars; recent sales and recent movements feeds |
| **Catalog** | Product Catalog & Categories | Verified | `GET/POST/PUT/DELETE /api/v1/categories` and `/products`; SKU isolation, base units, selling prices, cost prices, reorder thresholds; verified in `catalog.test.ts` |
| **Dual-UOM** | Multi-Unit Conversion Engine | Verified | `GET/POST/DELETE /api/v1/products/{id}/conversions`; configure conversion factors (e.g. 1 Bundle = 50 Pieces), derived selling prices, and default selling units; verified in `catalog.test.ts` |
| **Inventory** | Inbound Stock Receiving | Verified | `POST /api/v1/inventory/receipts`; delivery notes with supplier name, receiving in secondary conversion units (e.g. Bundles), automatic base quantity calculation, and unit cost capture; verified in `inventory.test.ts` |
| **Inventory** | Stock Adjustments & Audit | Verified | `POST /api/v1/inventory/adjustments`; adjustment deltas with mandatory reason codes (`DAMAGED`, `EXPIRED`, `BREAKAGE`, `COUNT_DISCREPANCY`, `CORRECTION`); verified in `inventory.test.ts` |
| **Ledger** | Immutable Stock Movements | Verified | `GET /api/v1/inventory/movements`; filter by movement type (`RECEIPT`, `SALE`, `ADJUSTMENT`, `RETURN`), reference, and date; verified in `inventory.test.ts` |
| **Sales** | Counter POS & Checkout | Verified | Multi-product cart, unit picker (base or converted units), price overrides, line discounts, customer metadata; idempotency key protection (`crypto.randomUUID()`); verified in `sales.test.ts` |
| **Receipts** | Printable Operational Receipts | Verified | `GET /api/v1/sales/{id}/receipt`; printable thermal/standard receipt modal with itemized lines and non-payment disclaimer: *"Operational Sales Record - Not a Proof of Payment"*; verified in `sales.test.ts` |
| **Sales** | Sales History & Void Workflow | Verified | `GET /api/v1/sales`; detail drawer; `POST /api/v1/sales/{id}/void` with mandatory cancellation reason; atomic inventory stock restoration verified in `sales.test.ts` |
| **Reporting** | Operational Analytics & Valuation | Verified | `GET /api/v1/sales/reports/summary`, `/daily`, and `/inventory/reports/valuation`; aggregate revenue, daily trends, category valuations; cashier financial redaction enforced |
| **Subscription** | Tenant Self-Inspection & Quotas | Verified | `GET /api/v1/me/subscription`; active plan details, period dates, days remaining, product capacity meter, user seat meter, and feature entitlements |
| **Settings** | Shop Profile & Team Members | Verified | `GET/PUT /api/v1/tenants/me` and `GET /api/v1/tenants/memberships`; shop contact details, operating currency, timezone, and staff team list |
| **Tests** | Automated Vitest Test Suite | Verified | **12/12 tests passing** across 4 test files (`auth.test.ts`, `catalog.test.ts`, `inventory.test.ts`, `sales.test.ts`) |
| **Builds** | Production Bundle Verification | Verified | `shop/` built in 4.73s (0 errors); `admin/` built in 5.86s (0 errors); `public/` built in 4.84s (0 errors) |
| **Backend** | Spring Boot & PostgreSQL Verification | Verified | Fixed PostgreSQL JPQL untyped null parameters in `CategoryRepository` & `ProductRepository` using `JpaSpecificationExecutor`; backend fully passing and live on `:8080` |

## Detailed Phase Nine Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Public Architecture** | React 19 + Vite Application | Verified | Independent React 19 app created in `public/` running on port 5175; configured with TypeScript, Vitest, and reverse-proxy to `:8080`; zero shared runtime state with admin or shop |
| **Design System** | Figma Tokens & Typography | Verified | DM Sans + Manrope geometric typography, color variables (`--ink: #102a3a`, `--teal: #16756c`, `--cream: #f6f4ed`, sand, borders), responsive mobile navigation header, accessible cards, and footer |
| **Home Page** | Marketing & Product Showcase | Verified | Hero headline, Dual-UOM trust strip (BAG, PCS, KG, BOX, L, M), interactive live inventory overview snapshot, 4 problem/solution pillars, 6 feature cards, 4-step onboarding flow, pricing preview, and FAQ accordion |
| **Features Page** | Deep Dive Capabilities | Verified | Detailed capability breakdowns: Dual-UOM Conversion Engine (`5 BOX × 200 PCS = 1,000 PCS`), Delivery Note Receiving, Moving WAC Valuation, Immutable Ledger, Accountable Adjustments, RBAC, Multi-tenant Data Isolation, Printable Valuation Sheets |
| **Pricing Page** | Dynamic Plan Comparison | Verified | Live fetch from `GET /api/v1/public/subscription-plans`; monthly vs. yearly billing toggle with 15% discount calculation; quota comparisons (`productLimit`, `userLimit`, `branchLimit`, `trialDays`); loading, error retry, and empty states; direct CTA leading into registration |
| **Shop Registration** | 3-Step Onboarding Wizard | Verified | Client-side validation: Step 1 (Shop info & currency: USD, GHS, NGN, KES), Step 2 (Account owner with explicit note that no password is required upfront due to review-based onboarding), Step 3 (Plan preference, notes, Terms consent); submits to `POST /api/v1/onboarding-requests`; handles duplicate email 409 conflict cleanly |
| **Registration Success** | Confirmation & Timeline | Verified | Displays generated reference code (`REQ-YYYY-XXXXX`), one-click copy button, 3-stage visual timeline (`Submitted` → `Administrator review` → `Account setup`), direct link to Status Checker |
| **Status Checker** | Real-Time Application Tracking | Verified | Querying `GET /api/v1/onboarding-requests/status/{referenceCode}`; displays status badge (`PENDING`, `APPROVED`, `REJECTED`, `MORE_INFO_REQUESTED`), submitted timestamp, shop name, and contextual guidance |
| **About & Contact** | Product Purpose & Inquiries | Verified | Honest product-focused mission explaining why generic POS fails with building materials; honest support contact channels (`support@kuterplatform.com`) with zero fabricated phone numbers or fake executive bios |
| **Automated Tests** | Vitest Test Suite | Verified | **16/16 Vitest tests passed** across 4 test files in 380ms (`public-api.test.ts`, `pricing.test.ts`, `registration.test.ts`, `status.test.ts`) |
| **Production Builds** | React 19 + Vite Builds | Verified | `public/` built in 3.51s (0 errors); `admin/` built in 4.02s (0 errors); `shop/` built in 6.07s (0 errors) |
| **Backend Integrity** | Spring Boot Regression Suite | Verified | **172/172 backend tests passed** (including `AuthenticationApiTests` and `OnboardingWorkflowTests`); demo data seeder initialized; backend running on `:8080` |

---

## Detailed Phase Eight Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Directory Reorganization** | Root Structure Migration | Verified | `frontend/` moved to `shop/` (`"kuter-shop"`), root `public/` directory established with specification README, zero file loss; both `admin` and `shop` verified building cleanly |
| **Admin Architecture** | Modular React Application | Verified | Modular architecture in `admin/src/` with `api/client.ts`, `api/types.ts`, `context/AuthContext.tsx`, reusable components (`Modal`, `ConfirmDialog`, `DataTable`, `PageHead`, `StatusBadge`, `Alert`), Figma design tokens in `tokens.css` & `admin.css` |
| **Authentication** | Platform Admin Gate | Verified | Authenticates against `POST /api/v1/auth/login`; verifies `isPlatformAdmin === true` (`ROLE_PLATFORM_ADMIN`); non-admins rejected with HTTP 403; Bearer token and `X-Correlation-ID` header auto-attached; verified in `auth-workflow.test.ts` & `api-client.test.ts` |
| **Platform Overview** | Metrics & Health Dashboard | Verified | Displays aggregate metrics from `GET /api/v1/admin/metrics/overview` (total shops, active, suspended, pending onboarding, active subscriptions, MRR); backend health pill from `GET /api/v1/health`; quick navigation hub; recent audit feed verified in `audit-logs.test.ts` |
| **Onboarding Queue** | Request Review, Approval & Rejection | Verified | Filterable table (`ALL`, `PENDING`, `APPROVED`, `REJECTED`) and search; detail drawer; atomic approval modal (`POST /api/v1/admin/onboarding-requests/{id}/approve`) with custom slug; rejection modal (`POST .../reject`) requiring mandatory reason verified in `onboarding.test.ts` |
| **Tenants Management** | Directory & Status Controls | Verified | Operational status tabs (`ALL`, `ACTIVE`, `SUSPENDED`); direct tenant creation modal (`POST /api/v1/admin/tenants`); operational suspension/reactivation modal (`PATCH .../status`) with audit reason; decoupled status inspection (`GET .../status`) verified in `tenants.test.ts` |
| **Subscription Plans** | Commercial Tier Management | Verified | Commercial tier table with quota limits; Create Plan modal with slug validation and quota inputs; Edit Plan modal; status toggle (`PATCH .../status`); delete with `409 Conflict` integrity defense verified in `plans.test.ts` |
| **Tenant Subscriptions** | Monitoring & Lifecycle Controls | Verified | Summary KPI cards; status filtering; Assign Plan modal; Extend Subscription modal with mandatory reason; Change Plan modal; administrative suspend, reactivate, and cancel actions; chronological audit history ledger (`GET .../{id}/history`) verified in `subscriptions.test.ts` |
| **Audit Logs** | Multi-Attribute Query Engine | Verified | Comprehensive query filters for `outcome` (`SUCCESS`, `FAILURE`, `DENIED`), `action`, `actorEmail`, `entityType`, UTC date ranges; paginated event table; safe event metadata inspection modal verified in `audit-logs.test.ts` |
| **Frontend Tests** | Vitest Test Suite | Verified | **23/23 frontend tests passed** across 7 test files in 513ms (`api-client.test.ts`, `auth-workflow.test.ts`, `onboarding.test.ts`, `tenants.test.ts`, `plans.test.ts`, `subscriptions.test.ts`, `audit-logs.test.ts`) |
| **Production Builds** | React 19 + Vite Builds | Verified | `admin/` built in 2.77s (0 errors); `shop/` built in 2.45s (0 errors) |
| **Backend Integrity** | Spring Boot Regression Suite | Verified | **172/172 tests passed** (0 failures, 0 errors, 0 skipped, 1m elapsed); zero backend modifications required |
| **Scope Adherence** | Non-Payment & Boundary Isolation | Verified | Zero shop portal operations or public marketing code built; plan prices remain operational metadata without payment processors or billing webhooks |

---

## Detailed Phase Seven Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Reports & Audit Enhancements Migration | Verified | `V7__reports_and_audit_enhancements.sql` adds `outcome` (default `SUCCESS`) to `audit_logs` and 5 composite performance indexes (`idx_audit_logs_action`, `idx_audit_logs_tenant_action`, `idx_audit_logs_created_at`, `idx_stock_movements_tenant_type_date`, `idx_stock_movements_tenant_date`) on PostgreSQL 18.6 and H2 |
| **Documentation** | Interactive SpringDoc OpenAPI 3.0 & Swagger UI | Verified | Added `springdoc-openapi-starter-webmvc-ui` 2.8.5; configured `OpenApiConfig.java` with JWT Bearer security scheme; permitted `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` in `SecurityConfig.java` |
| **Common** | Query Standardization & UTC Parsing | Verified | `DateTimeUtils` parses ISO-8601 strings to `Instant` in UTC with range validation; pagination clamped to max 100 items per page; sort whitelist protection across endpoints |
| **Inventory** | Current Inventory Dual-UOM Report | Verified | `GET /api/v1/inventory/reports/current` decomposes base stock into primary and secondary units with dynamic stock status (`IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`) verified in `InventoryReportingAndMovementTests` |
| **Inventory** | Detailed Valuation Report | Verified | `GET /api/v1/inventory/reports/valuation/items` provides itemized breakdown of quantities, moving average costs, and extended total valuations verified in `InventoryReportingAndMovementTests` |
| **Inventory** | Movement Ledger with Full Filters | Verified | `GET /api/v1/inventory/movements` (and alias `/reports/movements`) filters by `referenceType` (`RECEIPT`, `SALE`, `ADJUSTMENT`, `RETURN`), `startDate`, `endDate`, and `actorId` with pagination verified in `InventoryReportingAndMovementTests` |
| **Sales** | Sales Filter by Product | Verified | `GET /api/v1/sales` filters sales containing a specified `productId` with pagination and date ranges verified in `SalesReportingAndDailyBreakdownTests` |
| **Sales** | Daily Interval Sales Breakdown | Verified | `GET /api/v1/sales/reports/daily` groups completed sales by calendar day (UTC) providing daily transaction counts, total revenue, units sold, and COGS/gross profit (redacted for cashiers) verified in `SalesReportingAndDailyBreakdownTests` |
| **Sales** | Cashier Financial Margin Redaction | Verified | Sensitive margin fields (`totalCostOfGoodsSold`, `grossProfit`, `profitMarginPercentage`) omitted via `@JsonInclude(NON_NULL)` for `ROLE_TENANT_CASHIER` on both summary and daily reports verified in `SalesReportingAndDailyBreakdownTests` |
| **Sales** | Operational Printable Receipt | Verified | `GET /api/v1/sales/{id}/receipt` provides printable receipt data with prominent operational disclaimer ("Operational Sales Record - Not a Proof of Payment"); no payment processing claims verified in `SalesReportAndIsolationTests` |
| **Audit** | Tenant-Scoped Audit Log Query | Verified | `GET /api/v1/audit-logs` accessible by `OWNER` and `MANAGER` strictly filtered to authenticated tenant context; supports filtering by `action`, `entityType`, `actorEmail`, `outcome`, and date range verified in `AuditLogQueryAndSecurityTests` |
| **Audit** | Platform Admin Cross-Tenant Audit Query | Verified | `GET /api/v1/admin/audit-logs` accessible by `PLATFORM_ADMIN` across all tenants or filtered to specific tenant with full outcome and date-range filters verified in `AuditLogQueryAndSecurityTests` |
| **Audit** | Cashier Audit Query Restriction | Verified | Cashiers attempting to access `GET /api/v1/audit-logs` rejected with `HTTP 403 Forbidden` verified in `AuditLogQueryAndSecurityTests` |
| **Metrics** | Platform Overview Aggregates | Verified | `GET /api/v1/admin/metrics/overview` aggregates tenant counts by status, pending onboarding requests, active subscriptions, and estimated MRR verified in `PlatformOverviewAndStatusTests` |
| **Tenancy** | Operational Status vs Subscription Separation | Verified | `GET /api/v1/admin/tenants/{id}/status` and `GET /api/v1/tenants/me/status` return decoupled `TenantStatus` (`ACTIVE`, `SUSPENDED`, `INACTIVE`) alongside `SubscriptionStatus` (`ACTIVE`, `EXPIRED`, `TRIALING`) verified in `PlatformOverviewAndStatusTests` |
| **Calculations** | Unit Conversions & Moving WAC Formulas | Verified | Cartons/packets/pieces dual-UOM conversions and weighted-average costing formulas on receipt, zero stock, sales, downward, and upward adjustments verified in `UnitConversionAndCostingUnitTests` |
| **Security** | Cross-Tenant Direct Object Reference Isolation | Verified | Cross-tenant direct access strictly rejected (404/403) across products, balances, adjustments, sales, receipts, and voids verified in `CrossTenantSecurityComprehensiveTests` |
| **Test Suite** | Full Backend Test Suite | Verified | **172/172 tests passed** (31 new Phase 7 tests, 0 failures, 0 errors, 0 skipped, 52.593s) |
| **Integration** | Live PostgreSQL 18.6 Migrations | Verified | `PostgreSqlIntegrationTests` passed 3/3 validating clean migration V1 through V7 against native PostgreSQL 18.6 |
| **API Contract** | API Freeze Specification | Verified | Comprehensive frozen API contract published in `docs/api/frozen-contract.md` covering all 10 domain endpoint groups, schemas, headers, query parameters, and error envelopes |
| **Documentation** | Architectural Deliverables | Verified | `docs/api/frozen-contract.md`, `docs/phase-seven-report.md`, `docs/development-progress.md`, `README.md` |

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Subscription Enhancements Migration | Verified | `V6__subscription_enhancements.sql` adds currency, trial_days, lifecycle timestamps, is_current, optimistic versioning, `subscription_history` table, and 4 composite performance indexes on PostgreSQL 18.6 and H2 |
| **Plans** | Plan CRUD Management | Verified | `POST/GET/PUT/PATCH/DELETE /api/v1/subscription-plans` verified in `SubscriptionPlanTests` (creation, duplicate code rejection, validation failure, admin authorization) |
| **Plans** | Plan Deletion Protection | Verified | Plans referenced by active tenant subscriptions cannot be deleted (`409 Conflict`), unreferenced plans deleted cleanly in `SubscriptionPlanTests` |
| **Plans** | Public Pricing Directory | Verified | `GET /api/v1/public/subscription-plans` accessible without authentication (`permitAll()`), returns active & public plans only in `SubscriptionPlanTests` |
| **Lifecycle** | Direct Plan Assignment & Activation | Verified | `POST /api/v1/admin/subscriptions/assign` and `/activate` transition statuses and record history in `SubscriptionLifecycleTests` |
| **Lifecycle** | Free Trial Initiation | Verified | `POST /api/v1/admin/subscriptions/{id}/start-trial` sets `TRIALING`, `trial_start`, `trial_end` in `SubscriptionLifecycleTests` |
| **Lifecycle** | Plan Switching & Downgrade | Verified | `POST /api/v1/admin/subscriptions/{id}/change-plan` switches plan and applies new quotas in `SubscriptionLifecycleTests` |
| **Lifecycle** | Extension with Mandatory Reason | Verified | `POST /api/v1/admin/subscriptions/{id}/extend` requires non-blank reason, advances `current_period_end`, restores expired subs to `ACTIVE` in `SubscriptionLifecycleTests` |
| **Lifecycle** | Administrative Suspension & Reactivation | Verified | `POST .../{id}/suspend` freezes access with reason; `POST .../{id}/reactivate` restores status in `SubscriptionLifecycleTests` |
| **Lifecycle** | Cancellation (Immediate vs End-of-Period) | Verified | Immediate sets `CANCELLED` and `cancelled_at`; scheduled sets `cancel_at_period_end = true` in `SubscriptionLifecycleTests` |
| **Entitlements** | Real-Time Dynamic Expiry Cutoff | Verified | `EntitlementService.getEffectiveStatus(tenantId)` evaluates `EXPIRED` immediately when clock passes `current_period_end` without waiting for batch jobs in `SubscriptionExpiryAndEntitlementTests` |
| **Entitlements** | Operation Gating on Expiry | Verified | Expired shops receive `HTTP 403 Forbidden` on creating products, sales, or adding staff members in `SubscriptionExpiryAndEntitlementTests` |
| **Entitlements** | Extension Restores Access | Verified | Extending an expired subscription immediately re-enables product and sale creation in `SubscriptionExpiryAndEntitlementTests` |
| **Entitlements** | Active Product Limit Enforcement | Verified | Creating products beyond plan limit rejected with `HTTP 409 Conflict` ("Catalog product limit reached") in `SubscriptionExpiryAndEntitlementTests` |
| **Entitlements** | Staff User Seat Limit Enforcement | Verified | Adding members beyond plan user limit rejected with `HTTP 409 Conflict` ("Staff user seat limit reached") in `SubscriptionExpiryAndEntitlementTests` |
| **Entitlements** | Downgrade Data Preservation | Verified | Existing products and users remain active upon plan downgrade; creating above new ceiling blocked in `SubscriptionExpiryAndEntitlementTests` |
| **Scheduler** | Background Expiry Processing | Verified | `subscriptionService.processExpirations()` sweeps elapsed subscriptions to DB status `EXPIRED` and writes history in `SubscriptionExpiryAndEntitlementTests` |
| **Security** | Cross-Tenant Subscription Isolation | Verified | Tenant B cannot inspect Tenant A's subscription or history; context tampering with `X-Tenant-ID` blocked with `403 Forbidden` in `SubscriptionIsolationAndSecurityTests` |
| **Security** | Role-Based Access Control | Verified | Non-admin tenant users blocked from platform admin endpoints (`403 Forbidden`) in `SubscriptionIsolationAndSecurityTests` |
| **Portal** | Tenant Subscription Self-Inspection | Verified | `GET /api/v1/me/subscription` returns plan code, name, status, effectiveStatus, daysRemaining, limits, active counts, and features in `SubscriptionIsolationAndSecurityTests` |
| **Metrics** | Subscription Summary & MRR | Verified | `GET /api/v1/admin/subscriptions/summary` aggregates counts and calculates estimated monthly revenue in `SubscriptionIsolationAndSecurityTests` |
| **Audit** | Immutable Subscription History | Verified | Every state change records `subscription_history` entry with action, previous/new status, timestamps, reason, and actor in `SubscriptionLifecycleTests` |
| **Test Suite** | Full Backend Test Suite | Verified | **141/141 tests passed** (35 subscription tests, 0 failures, 0 skipped, 50.377s) |
| **Frontend** | React Admin Portal | Verified | `npm run build` completed cleanly in 3.29s (0 errors) |
| **Frontend** | React Client Portal | Verified | `npm run build` completed cleanly in 3.63s (0 errors) |
| **Documentation** | Architectural Deliverables | Verified | `docs/subscription-design.md`, `docs/api/phase-six.md`, `database-design.md`, `tenant-isolation.md`, `security-design.md`, `environment-configuration.md`, `architecture.md`, `phase-six-report.md` |

---

## Detailed Phase Five Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Sales Enhancements Migration | Verified | `V5__sales_enhancements.sql` adds cashier_id, idempotency_key, notes, void metadata to `sales`, product snapshot columns to `sale_items`, and 5 composite performance & idempotency indexes on PostgreSQL 18.6 and H2 |
| **Sales** | Counter Sale Processing & Cart | Verified | `POST /api/v1/sales` creates sale with auto-generated invoice number (`INV-YYYYMMDD-XXXX`), validates customer info, line items, and discounts verified in `SaleCreationAndDeductionTests` |
| **Sales** | Dual-UOM Sales Deduction | Verified | Selling in secondary units (e.g. 2 bundles = 40 bars) converts correctly and deducts canonical base units from inventory balance verified in `SaleCreationAndDeductionTests` |
| **Sales** | Custom Unit Pricing | Verified | Overriding catalog base price at point of sale correctly calculates line totals and discounts verified in `SaleCreationAndDeductionTests` |
| **Sales** | Insufficient Stock Rollback | Verified | Attempting to sell more stock than on hand fails with HTTP 400 and preserves existing balance verified in `SaleCreationAndDeductionTests` |
| **Sales** | Inactive Product Rejection | Verified | Products marked `INACTIVE` or `DISCONTINUED` rejected with HTTP 400 verified in `SaleCreationAndDeductionTests` |
| **Sales** | Idempotency Replay Protection | Verified | Replay of existing `idempotencyKey` returns original sale response without duplicate inventory deduction verified in `SaleCreationAndDeductionTests` |
| **Sales** | Canonical Lock Ordering | Verified | Concurrent multi-product checkouts in opposing cart item orders execute deadlock-free due to natural UUID sorting verified in `SaleConcurrencyTests` |
| **Sales** | Stock Concurrency Safety | Verified | 5 concurrent threads competing for scarce stock correctly allow 3 transactions and fail 2, leaving zero negative stock verified in `SaleConcurrencyTests` |
| **Sales** | Moving WAC Snapshotting | Verified | Line items capture real-time moving weighted-average cost at checkout; future deliveries do not alter historical margins verified in `SaleCreationAndDeductionTests` |
| **Sales** | Sale Voiding & Stock Restoration | Verified | `POST /api/v1/sales/{id}/void` transitions status to `VOIDED`, restores physical stock balances under lock, and logs compensating `RETURN` movement (`SALE_VOID`) verified in `SaleVoidingAndReversalTests` |
| **Sales** | Void Valuation Recalculation | Verified | Returned items recalculate moving weighted-average unit cost using returned cost valuation verified in `SaleVoidingAndReversalTests` |
| **Sales** | Double-Void & Validation Defense | Verified | Voiding an already-voided sale or omitting void reason rejected with HTTP 400 verified in `SaleVoidingAndReversalTests` |
| **Receipts** | Operational Printable Receipt | Verified | `GET /api/v1/sales/{id}/receipt` formats shop header, cashier name, line items, and disclaimer: *"Operational Sales Record - Not a Proof of Payment"* verified in `SalesReportAndIsolationTests` |
| **Reporting** | Sales Summary Report | Verified | `GET /api/v1/sales/reports/summary` aggregates total revenue, transactions, COGS, gross profit, and margin percentage verified in `SalesReportAndIsolationTests` |
| **Reporting** | Product Ranking Report | Verified | `GET /api/v1/sales/reports/products` ranks top-selling products by quantity and revenue with pagination verified in `SalesReportAndIsolationTests` |
| **Security** | Cashier Financial Redaction | Verified | Cashiers querying sales or reports receive JSON with `totalCostOfGoodsSold`, `grossProfit`, `profitMarginPercentage`, and line item costs completely omitted (`.doesNotExist()`) verified in `SalesReportAndIsolationTests` |
| **Security** | Cashier Void Restriction | Verified | Cashiers attempting to void a sale rejected with HTTP 403 Forbidden verified in `SaleVoidingAndReversalTests` |
| **Tenancy** | Cross-Tenant Data Isolation | Verified | Tenant B cannot access Tenant A sales, receipts, or reports (HTTP 404/403) verified in `SalesReportAndIsolationTests` |
| **Test Suite** | Full Backend Test Suite | Verified | **106/106 tests passed** (26 sales tests, 0 failures, 0 skipped, 57.371s) |
| **Frontend** | React Admin Portal | Verified | `npm run build` completed cleanly in 5.06s (0 errors) |
| **Frontend** | React Client Portal | Verified | `npm run build` completed cleanly in 5.85s (0 errors) |
| **Documentation** | Architectural Deliverables | Verified | `docs/sales-design.md`, `docs/api/phase-five.md`, `database-design.md`, `tenant-isolation.md`, `architecture.md`, `phase-five-report.md` |

---

## Detailed Phase Four Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Inventory Enhancements Migration | Verified | `V4__inventory_enhancements.sql` adds idempotency keys, image storage keys, and 5 performance indexes on PostgreSQL 18.6 and H2 |
| **Catalog** | Product Categories CRUD | Verified | `POST/GET/PUT/DELETE /api/v1/categories` with case-insensitive tenant uniqueness & soft-deactivation verified in `CategoryAndProductApiTests` |
| **Catalog** | Unit Definitions & Auto-Seeding | Verified | `POST/GET /api/v1/units` auto-seeds standard units (`PCS`, `BAG`, `TON`, `KG`, `MTR`, `BOX`, `BNDL`, `PALLET`, `SHT`) verified in `CategoryAndProductApiTests` |
| **Catalog** | Product Catalog CRUD & Search | Verified | `POST/GET/PUT/DELETE /api/v1/products` with SKU uniqueness, status transitions, and image keys verified in `CategoryAndProductApiTests` |
| **Catalog** | Dual-UOM Unit Conversions | Verified | `POST/GET/DELETE /api/v1/products/{id}/units` with conversion multipliers, base unit preservation verified in `CategoryAndProductApiTests` |
| **Inventory** | Stock Receiving & WAC Engine | Verified | Inbound delivery recording with dual-UOM conversion and real-time moving weighted-average costing verified in `StockReceivingAndCostingTests` |
| **Inventory** | Idempotency Replay Protection | Verified | Replay of identical `idempotencyKey` returns original receipt/adjustment without double-crediting balances verified in `StockReceivingAndCostingTests` & `StockAdjustmentAndLedgerTests` |
| **Inventory** | Stock Adjustments & Counts | Verified | Quantity deltas and target physical count audit reconciliations verified in `StockAdjustmentAndLedgerTests` |
| **Inventory** | Negative Stock Prevention | Verified | Adjustments causing on-hand quantity $< 0$ rejected with HTTP 400 in `StockAdjustmentAndLedgerTests` |
| **Inventory** | WAC Cost Preservation | Verified | Downward adjustments reduce inventory value while keeping unit average cost constant verified in `StockAdjustmentAndLedgerTests` |
| **Inventory** | Pessimistic Write Locking | Verified | `SELECT ... FOR UPDATE` query in `StockBalanceRepository` prevents concurrent modification race conditions |
| **Ledger** | Immutable Stock Movement Ledger | Verified | Every stock receipt and adjustment generates an immutable ledger movement record with running `balance_after` in `StockAdjustmentAndLedgerTests` |
| **Reporting** | Inventory Valuation Report | Verified | Live total valuation, aggregate quantities, and categorized breakdowns verified in `InventoryReportingAndIsolationTests` |
| **Reporting** | Low-Stock Replenishment Alerts | Verified | Filtering balances and dedicated low-stock report verified in `InventoryReportingAndIsolationTests` |
| **Security** | Role-Based Access Control | Verified | Cashier role forbidden from catalog mutation, receiving, or adjusting stock in `CategoryAndProductApiTests` & `StockReceivingAndCostingTests` |
| **Tenancy** | Cross-Tenant Data Isolation | Verified | Cross-tenant product receiving, adjustments, and report queries strictly blocked and isolated in `InventoryReportingAndIsolationTests` |
| **Test Suite** | Full Backend Test Suite | Verified | **80/80 tests passed** (`mvnw test` elapsed 55.188s, 0 failures, 0 skipped) |
| **Frontend** | React Admin Portal | Verified | `npm run build` completed cleanly (0 errors) |
| **Frontend** | React Client Portal | Verified | `npm run build` completed cleanly (0 errors) |
| **Documentation** | Architectural Deliverables | Verified | `docs/inventory-design.md`, `docs/api/phase-four.md`, `database-design.md`, `tenant-isolation.md`, `architecture.md`, `phase-four-report.md` |

---

## Detailed Phase Three Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Onboarding Schema Enhancements | Verified | Flyway `V3__onboarding_enhancements.sql` applied and validated on H2 and PostgreSQL 18.6 |
| **Authentication** | Stateless JWT Issuance & Parsing | Verified | `JwtService` issues HMAC-SHA256 tokens; verified in `AuthenticationApiTests` |
| **Authentication** | User Login & Profile Endpoints | Verified | `POST /api/v1/auth/login` and `GET /api/v1/users/me` verified in `AuthenticationApiTests` |
| **Authentication** | Account Enumeration Protection | Verified | Bad credentials return generic HTTP 401 error message in `AuthenticationApiTests` |
| **Authentication** | Account Lockout Enforcement | Verified | Suspended accounts rejected with HTTP 403 in `AuthenticationApiTests` |
| **Onboarding** | Public Registration Submission | Verified | `POST /api/v1/onboarding-requests` assigns `REQ-2026-XXXXX` in `OnboardingWorkflowTests` |
| **Onboarding** | Public Status Tracking | Verified | `GET /api/v1/onboarding-requests/status/{ref}` returns status without private data in `OnboardingWorkflowTests` |
| **Onboarding** | Duplicate Prevention | Verified | Duplicate pending email rejected with HTTP 409 Conflict in `OnboardingWorkflowTests` |
| **Onboarding** | Platform Admin Review & Approval | Verified | Approval provisions `Tenant`, `User`, `TenantMembership` (OWNER), `TenantSubscription` in `OnboardingWorkflowTests` |
| **Onboarding** | Platform Admin Rejection | Verified | Rejection records mandatory reviewer feedback in `OnboardingWorkflowTests` |
| **Tenancy** | Direct Tenant Creation | Verified | Platform admin direct provisioning verified in `TenantAndMembershipManagementTests` |
| **Tenancy** | Tenant Status Suspension | Verified | Suspending tenant blocks all subsequent user requests with HTTP 403 in `TenantAndMembershipManagementTests` |
| **Tenancy** | Tenant Settings Management | Verified | Tenant owner updating shop settings verified in `TenantAndMembershipManagementTests` |
| **Memberships** | Staff User Provisioning | Verified | Adding `MANAGER`, `CASHIER`, `STOREKEEPER` roles verified in `TenantAndMembershipManagementTests` |
| **Memberships** | Owner Protection | Verified | Attempting to delete the last active OWNER rejected in `TenantAndMembershipManagementTests` |
| **Audit** | Security & Onboarding Audit Trail | Verified | Immutable audit logs recorded for approvals, rejections, status changes, and logins |
| **Test Suite** | Full Backend Test Suite | Verified | **48/48 tests passed** (`mvnw test` elapsed 26.135s) |
| **Frontend** | React Admin Portal | Verified | `npm run build` completed cleanly in 2.69s |
| **Frontend** | React Client Portal | Verified | `npm run build` completed cleanly in 2.63s |
| **Documentation** | Architectural Deliverables | Verified | `docs/api/phase-three.md`, `security-design.md`, `tenant-isolation.md`, `architecture.md`, `phase-three-report.md` |

---

## Detailed Phase Two Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Database** | Core Domain Schema Migration | Verified | `V2__core_domain_schema.sql` applied 19 tables in PostgreSQL 18.6 and H2 |
| **Database** | Primary Key & Scale Strategy | Verified | All 19 tables use `UUID` PKs; currency and quantities use `NUMERIC(15, 4)` |
| **Database** | Dual Units of Measure (UOM) | Verified | `unit_definitions` and `product_unit_conversions` verified in `CatalogAndConversionTests` |
| **Security** | BCrypt Password Hashing | Verified | Salted BCrypt rounds verified in `AuthenticationAndSecurityTests` |
| **Security** | User Principal & Multi-Tenant Roles | Verified | `KuterUserPrincipal` loaded active roles (`OWNER`, `CASHIER`) across multiple shops |
| **Security** | Platform Admin Boundaries | Verified | Platform admins without tenant membership rejected with HTTP 403 on tenant context |
| **Tenancy** | Cross-Tenant Spoofing Defense | Verified | User in Shop A supplying Shop B header rejected with HTTP 403 in `TenantIsolationTests` |
| **Tenancy** | ThreadLocal Lifecycle Cleanup | Verified | `TenantContext.getTenantId()` verified empty after request in `TenantIsolationTests` |
| **Tenancy** | Tenant-Scoped Uniqueness | Verified | Same SKU `CEM-425` created in Tenant A and B; duplicate in Tenant A threw exception |
| **Tenancy** | Query Isolation | Verified | `ProductRepository.findByTenantIdAndId` returned `empty` across tenant boundary |
| **Inventory** | Stock Balance & Physical Counts | Verified | Balance tracked at 4-decimal precision in `InventoryLedgerTests` |
| **Inventory** | Append-Only Stock Ledger | Verified | `StockMovement` recorded `RECEIPT`, `SALE`, `ADJUSTMENT` with running balances |
| **Audit** | Immutable Audit Trail | Verified | `AuditService` created tamper-evident records linked to correlation ID |
| **Integration** | PostgreSQL 18 Native Execution | Verified | Full domain persistence verified on live PostgreSQL 18.6 in `PostgreSqlIntegrationTests` |
| **Test Suite** | Full Backend Test Suite | Verified | **24/24 tests passed** (`mvnw test` elapsed 22.102s) |
| **Frontend** | React Admin Portal | Verified | `npm run build` completed cleanly in 3.33s |
| **Frontend** | React Client Portal | Verified | `npm run build` completed cleanly in 3.74s |
| **Documentation** | Architectural Deliverables | Verified | `database-design.md`, `tenant-isolation.md`, `security-design.md`, `phase-two-report.md` |

---

## Detailed Phase One Verification Matrix

| Area | Component | Verification Status | Exact Evidence |
| :--- | :--- | :---: | :--- |
| **Environment** | Java 21 LTS | Verified | Oracle JDK `21.0.8+12-LTS-250` confirmed via `javac -version` |
| **Environment** | Maven & Wrapper | Verified | Maven 3.9.11 wrapper (`mvnw.cmd`) operational |
| **Environment** | Node.js & npm | Verified | Node.js `v22.20.0`, npm `10.9.3` confirmed |
| **Environment** | PostgreSQL 18 | Verified | PostgreSQL 18.6 running on `localhost:5432`, `kuter_dev` & `kuter_test` created |
| **Reference** | Amoudi Financial System | Inspected | Structure, `application.yml`, Flyway, MDC filters, and Lombok patterns analyzed |
| **Design** | Figma Design Export | Inspected | Extracted to `docs/design-reference/`, tokens mapped into `tokens.css` |
| **Backend** | Spring Boot Foundation | Verified | `mvnw test` passed (7 tests, 0 failures, 0 skipped) |
| **Backend** | Profile Configuration | Verified | `application.yml`, `application-dev.yml`, `application-prod.yml`, `application-test.yml` verified |
| **Backend** | Database Migrations | Verified | Flyway applied `V1__initial_schema.sql` on PostgreSQL 18 schema `public` |
| **Backend** | REST API Endpoints | Verified | `GET /api/v1/health` and `GET /api/v1/info` responded with HTTP 200 JSON |
| **Backend** | Security & CORS | Verified | HTTP 401 JSON on protected route; CORS allows `http://localhost:5173` & `5174`, blocks `evil.com` |
| **Admin Portal** | React 19 + Vite | Verified | `npm run build` completed cleanly (`tsc -b && vite build`) in 4.44s |
| **Frontend Portal** | React 19 + Vite | Verified | `npm run build` completed cleanly (`tsc -b && vite build`) in 2.90s |
| **Documentation** | Architectural Deliverables | Verified | `README.md`, `architecture.md`, `environment-configuration.md`, `design-reference.md`, `phase-one-report.md` |

---

## Known Blockers & Limitations

1. **Docker Desktop Daemon State:** Native PostgreSQL 18.6 service (`postgresql-x64-18`) on port 5432 was utilized for direct dev and test execution. `docker-compose.yml` is maintained for containerized deployment.
2. **Phase 8 Admin Portal Completion & Verification:** The Platform Administration Portal (`admin/`) is fully implemented with React 19, TypeScript, and Vite adhering to the Figma design system. All 23 automated Vitest tests pass across 7 test suites, the 172 backend Spring Boot tests remain 100% passing without regression, and both `admin` and `shop` production builds compile cleanly with zero errors.
3. **Phase 8 Scope Boundaries Honored:** External payment processing, payment gateways, card integrations, and automatic billing remain strictly out of scope; commercial subscription tiers and plan prices represent operational metadata. The shop portal (`shop/`) was safely migrated from `frontend/` and build-verified but untouched in feature scope; the public website (`public/`) exists solely as an architectural scaffold. Phase 9 (Shop Tenant Portal) is ready to commence upon explicit user confirmation.

