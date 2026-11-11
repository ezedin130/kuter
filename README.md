# Kuter — Multi-Tenant Building Materials Inventory Management SaaS

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18%20%7C%2017-blue.svg)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-19-61dafb.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-6-646cff.svg)](https://vitejs.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue.svg)](https://www.typescriptlang.org/)

Kuter is a modern multi-tenant SaaS platform engineered specifically for the **building materials supply and retail industry** (bulk cement, structural steel rebar, timber, blocks, paints, and hardware).

The system natively resolves the core operational challenges of the materials trade:
- **Dual Units of Measure (UOM):** Receiving bulk materials by the tonne, container, pallet, or bundle while retailing at the sales counter by individual bags, bars, or pieces without unit rounding discrepancies.
- **Tenant Data Isolation:** Secure, dedicated yard contexts, pricing rules, storekeeper roles, and ledgers for each merchant.
- **Strict Stock Auditability:** Append-only stock movement ledger tracking receipts, counter sales, returns, and damage/breakage adjustments.
- **Merchant Onboarding & Provisioning:** Streamlined public registration, platform-admin approval pipeline, and automatic tenant, owner user, and subscription provisioning.

---

## 1. Directory Structure

```text
kuter/
├── backend/            # Spring Boot REST API, JPA, Flyway, PostgreSQL persistence
│   ├── src/main/java/com/kuter/
│   │   ├── KuterApplication.java      # Application entrypoint
│   │   ├── common/                    # ApiResponse, ErrorResponse, PageResponse, CorrelationIdFilter, GlobalExceptionHandler
│   │   ├── config/                    # SecurityConfig, JwtService, JwtAuthenticationFilter, TenantContextFilter, CorsConfig
│   │   ├── identity/                  # AuthService, TenantMembershipService, AuthController, UserController, TenantMembershipController
│   │   ├── onboarding/                # OnboardingService, PublicOnboardingController, AdminOnboardingController
│   │   ├── tenancy/                   # TenantManagementService, AdminTenantController, TenantController, TenantSecurityService
│   │   ├── subscriptions/             # SubscriptionPlan, TenantSubscription, SubscriptionHistory, EntitlementService, SubscriptionService (Controllers, DTOs)
│   │   ├── catalog/                   # Category, UnitDefinition, Product, ProductUnitConversion (Services, Controllers, DTOs)
│   │   ├── inventory/                 # StockBalance, StockMovement, StockReceiving, StockAdjustment (Services, Controllers, DTOs)
│   │   ├── sales/                     # Sale, SaleItem, SaleStatus, SaleService, SaleController, DTOs
│   │   ├── lending/                   # StockLoan, StockLoanReturn, LendingService, LendingController, DTOs (Inter-Shop Lending)
│   │   ├── audit/                     # AuditLog, AuditService, AuditServiceImpl
│   │   └── health/                    # SystemStatus entity, HealthController, HealthService
│   ├── src/main/resources/            # application.yml, application-{dev,prod,test}.yml
│   │   └── db/migration/              # Flyway schema migrations (V1, V2, V3, V4, V5, V6, V7, V8)
│   └── src/test/                      # Unit, API, and PostgreSQL integration tests (186 backend tests; 237 across system)
├── admin/              # React 19 + TypeScript + Vite platform administration portal (:5174)
│   ├── src/api/                       # ApiClient with typed health/info methods
│   ├── src/components/                # Sidebar, Header, StatusBadge
│   └── src/styles/                    # tokens.css, index.css
├── shop/               # React 19 + TypeScript + Vite shop-owner & shop-staff portal (:5173)
│   ├── src/api/                       # ShopApiClient with full typed API endpoints (catalog, inventory, sales, lending, reports)
│   ├── src/context/                   # AuthProvider, multi-tenant session & role/granular permission guards
│   ├── src/components/                # ShopSidebar, ShopTopbar, Modal, Alert, StatusBadge, SaleReceiptModal, PageHead
│   ├── src/pages/                     # Dashboard, SalesPos, SalesHistory, Products, ProductDetail, Categories, ReceiveStock, Adjustments, LendingPage, StockLedger, Reports, Subscription, ShopSettings
│   ├── src/styles/                    # tokens.css, shop.css, index.css
│   └── src/__tests__/                 # Vitest test suite (auth, catalog, inventory, sales)
├── public/             # React 19 + TypeScript + Vite public marketing website & registration (:5175)
│   ├── src/api/                       # PublicApiClient for public subscription plans and registration requests
│   ├── src/components/                # Header, Footer, Logo, Badge, Button
│   ├── src/pages/                     # HomePage, FeaturesPage, PricingPage, RegistrationPage, StatusPage, AboutPage, ContactPage
│   ├── src/styles/                    # tokens.css, public.css
│   └── src/__tests__/                 # Vitest test suite (public-api, pricing, registration, status)
├── docs/               # Architecture, design tokens, environment specs, and reports
│   ├── api/                           # REST API specifications (frozen-contract.md, phase-six.md, phase-five.md, etc.)
│   ├── design-reference/              # Extracted Figma Make prototype
│   ├── architecture.md                # System design, modular monolith & tenant strategy
│   ├── database-design.md             # ERD, tables, UUID PKs, dual-UOM schema, V7 indexes
│   ├── tenant-isolation.md            # Multi-tenant isolation, TenantContextFilter, role permissions
│   ├── security-design.md             # Stateless JWT, roles, permissions, audit
│   ├── inventory-design.md            # Dual-UOM math, moving WAC costing, locking, idempotency
│   ├── sales-design.md                # Counter checkout, canonical locking, voiding, receipts, reports
│   ├── subscription-design.md         # Subscription lifecycle, entitlements, dynamic expiry cutoff
│   ├── environment-configuration.md   # Profiles & environment variables
│   ├── design-reference.md            # Design tokens & screen analysis
│   ├── development-progress.md        # Persistent progress tracking matrix
│   ├── phase-one-report.md            # Phase One foundation report
│   ├── phase-two-report.md            # Phase Two domain & security report
│   ├── phase-three-report.md          # Phase Three auth & onboarding report
│   ├── phase-four-report.md           # Phase Four catalog & inventory report
│   ├── phase-five-report.md           # Phase Five sales & reporting report
│   ├── phase-six-report.md            # Phase Six subscriptions & platform-admin report
│   ├── phase-seven-report.md          # Phase Seven reports, audit, operational features, testing & API freeze report
│   ├── phase-eight-report.md          # Phase Eight platform admin portal report
│   ├── phase-nine-report.md           # Phase Nine public website & registration report
│   ├── phase-ten-report.md            # Phase Ten shop operations portal report
│   └── phase-eleven-report.md         # Phase Eleven full-system integration & readiness report
├── .gitignore          # Repository-wide protection for credentials and build artifacts
├── .env.example        # Reference environment variables template
└── docker-compose.yml  # Local development containerized PostgreSQL service
```

---

## 2. Prerequisites

- **Java JDK:** 21 LTS (Oracle JDK or Eclipse Temurin)
- **Node.js:** v20+ (tested with v22.20.0) & npm 10+
- **PostgreSQL:** 17 or 18 (or Docker / Docker Compose)
- **Maven:** 3.9+ (bundled via `mvnw` / `mvnw.cmd` wrapper)

---

## 3. Quick Start & Development Setup

### Step 1: Environment Variables Setup

Copy the template to `.env` in the repository root:

```bash
cp .env.example .env
```

Verify your local PostgreSQL settings in `.env`:
```ini
DB_HOST=localhost
DB_PORT=5432
DB_NAME=kuter_dev
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

### Step 2: Database Setup

#### Option A: Native PostgreSQL (Installed locally)
Create the local development and test databases:
```sql
CREATE DATABASE kuter_dev;
CREATE DATABASE kuter_test;
```

#### Option B: Docker Compose
```bash
docker compose up -d
```

### Step 3: Start the Backend REST API

```bash
cd backend

# Windows PowerShell:
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Linux / macOS:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
The REST API starts on `http://localhost:8080`.
Flyway automatically applies `V1__initial_schema.sql`, `V2__core_domain_schema.sql`, `V3__onboarding_requests.sql`, `V4__inventory_enhancements.sql`, `V5__sales_enhancements.sql`, `V6__subscription_enhancements.sql`, and `V7__reports_and_audit_enhancements.sql` on startup.

Verify the foundation endpoint:
```bash
curl http://localhost:8080/api/v1/health
```

Explore interactive API documentation:
- **Swagger UI:** [`http://localhost:8080/swagger-ui/index.html`](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI 3.0 Spec:** [`http://localhost:8080/v3/api-docs`](http://localhost:8080/v3/api-docs)

### Step 4: Start the Platform Admin Portal

```bash
cd admin
npm install
npm run dev
```
Accessible at: **`http://localhost:5174`**

### Step 5: Start the Shop Portal

```bash
cd shop
npm install
npm run dev
```
Accessible at: **`http://localhost:5173`**

### Step 6: Start the Public Marketing Website & Registration

```bash
cd public
npm install
npm run dev
```
Accessible at: **`http://localhost:5175`**

---

## 4. Testing & Verification

### Run Backend Test Suite

```bash
cd backend

# Windows PowerShell:
.\mvnw.cmd test

# Linux / macOS:
./mvnw test
```
Executes all unit, API, security boundary, and real PostgreSQL integration tests against `kuter_test` (**172 tests, 0 failures, 0 errors, 0 skipped**).

### Run Frontend Builds & Typechecks

```bash
# Public Marketing Website (:5175)
cd public
npm test
npm run build

# Platform Admin Portal (:5174)
cd admin
npm test
npm run build

# Shop Portal (:5173)
cd shop
npm run build
```

---

## 5. Architectural & Design Documentation

For in-depth technical documentation, refer to the `docs/` directory:

- [docs/api/frozen-contract.md](file:///c:/D%20One/Projects/Kuter/docs/api/frozen-contract.md) — Complete frozen REST API contract covering all 10 domain areas, request/response models, query parameters, error envelopes, and HTTP status codes.
- [docs/api/phase-six.md](file:///c:/D%20One/Projects/Kuter/docs/api/phase-six.md) — REST API contract for subscription plans, public directory, tenant subscriptions, and admin metrics.
- [docs/api/phase-five.md](file:///c:/D%20One/Projects/Kuter/docs/api/phase-five.md) — REST API contract for sales processing, receipts, voiding, and operational reporting.
- [docs/api/phase-four.md](file:///c:/D%20One/Projects/Kuter/docs/api/phase-four.md) — REST API contract for product catalog and inventory management endpoints.
- [docs/api/phase-three.md](file:///c:/D%20One/Projects/Kuter/docs/api/phase-three.md) — REST API contract for authentication, onboarding, tenant, and membership endpoints.
- [docs/subscription-design.md](file:///c:/D%20One/Projects/Kuter/docs/subscription-design.md) — Subscription lifecycle, state machine, dynamic real-time expiry cutoff, and quota limits.
- [docs/sales-design.md](file:///c:/D%20One/Projects/Kuter/docs/sales-design.md) — Counter checkout mechanics, canonical locking, moving WAC costing, voiding formulas, and cashier privacy.
- [docs/inventory-design.md](file:///c:/D%20One/Projects/Kuter/docs/inventory-design.md) — Dual-UOM architecture, conversion math, moving weighted-average costing, locking, and idempotency.
- [docs/architecture.md](file:///c:/D%20One/Projects/Kuter/docs/architecture.md) — Modular monolith structure, bounded contexts, and clean architectural guidelines.
- [docs/database-design.md](file:///c:/D%20One/Projects/Kuter/docs/database-design.md) — ERD, table definitions, UUID rationale, dual-UOM schema, V7 indexes, and concurrency.
- [docs/tenant-isolation.md](file:///c:/D%20One/Projects/Kuter/docs/tenant-isolation.md) — Discriminator column strategy, `TenantContextFilter`, spoofing defense, and role permissions.
- [docs/security-design.md](file:///c:/D%20One/Projects/Kuter/docs/security-design.md) — Stateless JWT architecture, `KuterUserPrincipal`, multi-tenant roles, and audit logging.
- [docs/environment-configuration.md](file:///c:/D%20One/Projects/Kuter/docs/environment-configuration.md) — Details on `dev`, `prod`, and `test` profiles and environment variables.
- [docs/design-reference.md](file:///c:/D%20One/Projects/Kuter/docs/design-reference.md) — Design tokens, color palette, typography, and screen catalog extracted from the Figma prototype.
- [docs/development-progress.md](file:///c:/D%20One/Projects/Kuter/docs/development-progress.md) — Progress tracking matrix and phase verification evidence.
- [docs/phase-one-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-one-report.md) — Phase One foundation completion report.
- [docs/phase-two-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-two-report.md) — Phase Two domain architecture, database design, and security completion report.
- [docs/phase-three-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-three-report.md) — Phase Three authentication, onboarding, and tenant-management completion report.
- [docs/phase-four-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-four-report.md) — Phase Four product catalog and inventory management completion report.
- [docs/phase-five-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-five-report.md) — Phase Five sales processing, stock deduction, and sales reporting completion report.
- [docs/phase-six-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-six-report.md) — Phase Six subscription management and platform-administration completion report.
- [docs/phase-seven-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-seven-report.md) — Phase Seven backend reports, audit, operational features, comprehensive testing, and API freeze completion report.
- [docs/phase-eight-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-eight-report.md) — Phase Eight platform admin portal completion report.
- [docs/phase-nine-report.md](file:///c:/D%20One/Projects/Kuter/docs/phase-nine-report.md) — Phase Nine public website, subscription plans, and shop registration completion report.

---

## 6. License & Security

Internal project repository. Secret keys and production credentials must never be committed to source control.
