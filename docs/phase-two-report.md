# Kuter — Phase Two Completion Report

## 1. Executive Summary

Phase Two of the **Kuter Multi-Tenant Building Materials Inventory Management SaaS** project is complete. This phase established the backend's core domain architecture, relational database schema, Flyway migration governance, multi-tenant isolation safeguards, authentication and authorization foundations, and a comprehensive automated test suite.

All deliverables were designed and implemented following a **modular monolith** architecture in Java 21 and Spring Boot 3.4.3, adhering strictly to enterprise isolation constraints and production-grade PostgreSQL design patterns.

---

## 2. Architecture & Modular Domain Packages

The backend codebase adheres to clear package and dependency boundaries located under `com.kuter`:

```text
com.kuter/
├── common/             # Base entities, tenant interfaces, constants, exception handlers, response envelopes
├── config/             # Spring Security, CORS policies, TenantContextFilter
├── tenancy/            # Tenant entity, repository, TenantContext ThreadLocal, tenant providers
├── identity/           # User, TenantMembership, TenantRole, CustomUserDetailsService, KuterUserPrincipal
├── catalog/            # Category, UnitDefinition, Product, ProductUnitConversion entities and repositories
├── inventory/          # StockBalance, StockMovement (ledger), StockReceiving, StockAdjustment
├── sales/              # Sale, SaleItem domain entities and repositories
├── subscriptions/      # SubscriptionPlan, TenantSubscription entities and repositories
├── onboarding/         # TenantOnboardingRequest entity and repository
└── audit/              # AuditLog entity, repository, and AuditService
```

### Core Domain Entities Implemented

| Domain Module | Entities Implemented | Primary Responsibilities |
| :--- | :--- | :--- |
| **Common** | [`BaseEntity`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/common/BaseEntity.java)<br>[`TenantAwareBaseEntity`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/common/TenantAwareBaseEntity.java) | Abstract mapped superclasses with UUID identity, audit timestamps (`createdAt`, `updatedAt`), and immutable `tenantId`. |
| **Tenancy** | [`Tenant`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/Tenant.java) | Represents a shop tenant with business metadata, slug, contact details, currency, and operational status. |
| **Identity** | [`User`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/User.java)<br>[`TenantMembership`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/TenantMembership.java) | Platform users, bcrypt password hashing, platform admin flag, and tenant-scoped memberships with roles (`OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER`). |
| **Catalog** | [`Category`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/catalog/Category.java)<br>[`UnitDefinition`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/catalog/UnitDefinition.java)<br>[`Product`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/catalog/Product.java)<br>[`ProductUnitConversion`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/catalog/ProductUnitConversion.java) | Product hierarchy, tenant-scoped SKUs, base measurement units, and product-specific dual-unit conversion factors (`to_base_factor`, `from_base_factor`). |
| **Inventory** | [`StockBalance`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockBalance.java)<br>[`StockMovement`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockMovement.java)<br>[`StockReceiving`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockReceiving.java)<br>[`StockReceivingItem`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockReceivingItem.java)<br>[`StockAdjustment`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockAdjustment.java)<br>[`StockAdjustmentItem`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/inventory/StockAdjustmentItem.java) | Fast balance lookups in base units, immutable append-only movement history (`RECEIPT`, `SALE`, `ADJUSTMENT`, `RETURN`), inbound supplier receiving batches, and stock adjustments. |
| **Sales** | [`Sale`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/sales/Sale.java)<br>[`SaleItem`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/sales/SaleItem.java) | Sales transactions, receipt numbers, customer tracking, line items with sold unit and base quantity calculations. |
| **Subscriptions**| [`SubscriptionPlan`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/subscriptions/SubscriptionPlan.java)<br>[`TenantSubscription`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/subscriptions/TenantSubscription.java) | Tiered pricing plans (e.g., Starter, Pro, Enterprise) and active tenant subscription periods. |
| **Onboarding** | [`TenantOnboardingRequest`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/onboarding/TenantOnboardingRequest.java) | Public registration requests awaiting platform administrator review and provisioning. |
| **Audit** | [`AuditLog`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/audit/AuditLog.java) | Append-only security and operational audit trail recording actor, action, entity, tenant, IP, and changes. |

---

## 3. Database Schema Design & Flyway Migrations

The database schema is strictly managed via Flyway versioned migrations. Flyway migration [`V2__core_domain_schema.sql`](file:///c:/D%20One/Projects/Kuter/backend/src/main/resources/db/migration/V2__core_domain_schema.sql) defines 18 new tables, bringing the total relational tables to 19 (including the Phase One `system_status` verification table).

### Database Engineering Standards Applied

1. **Uniform UUID Primary Keys:**
   - Every table uses `UUID DEFAULT gen_random_uuid() PRIMARY KEY`.
   - Prevents auto-increment enumeration attacks, simplifies cross-system synchronization, and supports distributed data generation.

2. **Fixed-Point High Precision (`NUMERIC(15, 4)`):**
   - All quantities, conversion factors, stock balances, unit prices, discounts, taxes, and monetary totals use `NUMERIC(15, 4)`.
   - Completely eliminates IEEE 754 floating-point drift in inventory reconciliations and building-material bulk conversions.

3. **Tenant-Scoped Unique Constraints:**
   - Product SKUs, barcodes, categories, and stock balances are constrained per tenant:
     - `uk_products_tenant_sku`: `(tenant_id, sku)`
     - `uk_products_tenant_barcode`: `(tenant_id, barcode)`
     - `uk_categories_tenant_name`: `(tenant_id, name)`
     - `uk_stock_balances_tenant_product`: `(tenant_id, product_id)`
   - Allows independent shops to define identical SKUs (e.g., `CEM-50KG`) without collisions.

4. **Referential Integrity & Cascading Policies:**
   - Structural records (tenants, products, users) restrict deletion if referenced by financial or ledger tables (`ON DELETE RESTRICT`).
   - Line items strictly cascade from their parent document headers (`ON DELETE CASCADE` from `sales` to `sale_items`, `stock_receivings` to `stock_receiving_items`, `stock_adjustments` to `stock_adjustment_items`).

5. **Covering Indexes for Multi-Tenant Queries:**
   - B-tree indexes placed on `tenant_id` for every tenant-owned table.
   - Compound indexes placed on frequent lookup paths: `(tenant_id, status)`, `(tenant_id, created_at)`, `(tenant_id, product_id)`.

---

## 4. Multi-Tenant Isolation Architecture

Multi-tenancy in Kuter V1 uses a **Shared Database, Shared Schema with Discriminator Column (`tenant_id UUID NOT NULL`)** strategy, fortified by an automated defense-in-depth model:

```
[ Incoming HTTP Request ]
          │
          ▼
┌────────────────────────────────────────────────────────┐
│ TenantContextFilter (OncePerRequestFilter)             │
│ 1. Extract X-Tenant-ID header                          │
│ 2. Check SecurityContext Authentication                │
│ 3. If unauthenticated / public endpoint ──► Continue   │
│ 4. If platform admin:                                  │
│    - Deny X-Tenant-ID spoofing if no membership (403)  │
│ 5. If authenticated user:                              │
│    - Verify user has active TenantMembership           │
│    - Deny access to foreign tenant ID (403 Forbidden)  │
│    - Auto-activate tenant if user has single tenant    │
│ 6. Set ThreadLocal TenantContext.setTenantId(id)       │
└────────────────────────────────────────────────────────┘
          │
          ▼
┌──────────────────────────────────┐
│ Business Execution / Service     │
│ - TenantContext.requireTenantId()│
│ - SecurityContextTenantProvider  │
└──────────────────────────────────┘
          │
          ▼
┌────────────────────────────────────────────────────────┐
│ try { ... } finally { TenantContext.clear(); }         │
│ Memory leak & thread pool pollution prevention         │
└────────────────────────────────────────────────────────┘
```

### Isolation Guarantees Verified by Tests

- **Cross-Tenant Spoofing Defense:** An authenticated user from Shop Alpha attempting to provide `X-Tenant-ID` for Shop Beta receives an immediate `403 FORBIDDEN` before reaching any business logic.
- **Platform Admin Data Boundary:** A platform administrator (`ROLE_PLATFORM_ADMIN`) cannot query tenant business data by simply injecting a tenant header unless explicitly granted membership in that shop.
- **Single-Tenant Ergonomics:** A user with a single active tenant membership automatically has their `TenantContext` populated even if the client omits the `X-Tenant-ID` header.
- **Thread Pool Hygiene:** `TenantContextFilter` wraps request execution in a `try-finally` block that guarantees `TenantContext.clear()` executes upon response completion.

---

## 5. Security & Authentication Foundation

Spring Security has been elevated from the Phase One basic structure to support role-based multi-tenant authorization:

- **Stateful Security Components:**
  - [`KuterUserPrincipal`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/KuterUserPrincipal.java): Custom `UserDetails` carrying user identity, platform administrator status, and a list of active tenant memberships.
  - [`CustomUserDetailsService`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/CustomUserDetailsService.java): Database-backed user loader verifying user status and constructing granted authorities (`ROLE_PLATFORM_ADMIN`, `ROLE_TENANT_<ROLE>`).
  - [`SecurityConfig`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/SecurityConfig.java): Configured with `BCryptPasswordEncoder` (work factor 10), `DaoAuthenticationProvider`, stateless session management (`SessionCreationPolicy.STATELESS`), and `TenantContextFilter` registered after `SecurityContextHolderFilter`.
- **Tenant Role Matrix:**
  - `OWNER`: Full tenant administrative, financial, and inventory authority.
  - `MANAGER`: Catalog management, inventory approvals, and sales operations.
  - `STOREKEEPER`: Inbound receipts, stock balance adjustments, and warehouse counts.
  - `CASHIER`: Sales checkout, invoice generation, and customer payments.
  - `VIEWER`: Read-only reporting access.

---

## 6. Automated Testing Suite & Verification Matrix

The automated testing suite was expanded with 15 new domain, isolation, security, and integration tests, bringing the total suite to **24 automated tests**. All tests pass with zero failures and zero warnings.

```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  19.146 s
```

### Test Suite Execution Breakdown

| Test Suite Class | Tests Run | Target Domain & Scenarios Verified |
| :--- | :---: | :--- |
| [`TenantIsolationTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantIsolationTests.java) | 5 | - Rejection of cross-tenant ID spoofing with HTTP 403 Forbidden.<br>- Rejection of platform admin tenant spoofing without membership.<br>- Success path with valid tenant membership.<br>- Tenant-scoped SKU uniqueness enforcement.<br>- Strict repository query boundary isolation between tenants. |
| [`CatalogAndConversionTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/catalog/CatalogAndConversionTests.java) | 3 | - Product creation with base units and categories.<br>- Dual-unit conversion factor arithmetic (`Piece` vs `Box` at factor 24.0).<br>- Duplicate conversion definition rejection. |
| [`InventoryLedgerTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/inventory/InventoryLedgerTests.java) | 3 | - Atomic stock balance tracking per tenant and product.<br>- Append-only stock movement ledger history across transaction types.<br>- Inbound receiving document with multi-item line persistence. |
| [`AuthenticationAndSecurityTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/identity/AuthenticationAndSecurityTests.java) | 5 | - BCrypt password hashing and verification.<br>- Multi-tenant role authority generation (`ROLE_TENANT_OWNER`, `ROLE_TENANT_CASHIER`).<br>- Unknown user lookup failure handling.<br>- Platform administrator authority assignment (`ROLE_PLATFORM_ADMIN`).<br>- Append-only audit log persistence and correlation linking. |
| [`PostgreSqlIntegrationTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/PostgreSqlIntegrationTests.java) | 2 | - Execution of `V1` and `V2` Flyway migrations on live PostgreSQL 18.6 (`kuter_dev`).<br>- JPA entity schema validation (`ddl-auto: validate`) against PostgreSQL tables. |
| [`HealthControllerTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/HealthControllerTests.java) | 3 | - Health check database verification.<br>- Application info metadata.<br>- Correlation ID propagation. |
| [`SecurityAndErrorHandlingTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/SecurityAndErrorHandlingTests.java) | 2 | - Protected endpoint rejection (401 Unauthorized).<br>- Method not allowed rejection (405 Method Not Allowed). |
| [`KuterApplicationTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/KuterApplicationTests.java) | 1 | - Spring Boot ApplicationContext startup test. |

---

## 7. Documentation Deliverables Created

| Document | Purpose |
| :--- | :--- |
| [`docs/database-design.md`](file:///c:/D%20One/Projects/Kuter/docs/database-design.md) | Comprehensive database design document including ERD entity relationships, table specifications, UUID rationale, `NUMERIC(15, 4)` scale precision, dual-unit conversion math, and index strategy. |
| [`docs/tenant-isolation.md`](file:///c:/D%20One/Projects/Kuter/docs/tenant-isolation.md) | Architectural document explaining tenant context resolution, filter flow diagrams, ThreadLocal lifecycle, memory leak defense, and spoofing prevention rules. |
| [`docs/security-design.md`](file:///c:/D%20One/Projects/Kuter/docs/security-design.md) | Security blueprint detailing authentication architecture, user principal models, tenant role hierarchies, BCrypt password hashing, and audit trails. |
| [`docs/architecture.md`](file:///c:/D%20One/Projects/Kuter/docs/architecture.md) | Updated Section 3 with the complete 10-module backend package structure and design responsibilities. |
| [`docs/development-progress.md`](file:///c:/D%20One/Projects/Kuter/docs/development-progress.md) | Updated roadmap tracking matrix marking Phase Two complete with test counts and verification evidence. |
| [`README.md`](file:///c:/D%20One/Projects/Kuter/README.md) | Updated central project index with direct links to all Phase Two design and architecture documents. |

---

## 8. Phase Three Readiness & Handover

Phase Two is strictly bounded to domain architecture, database design, tenant isolation, and security foundations. **No business API endpoints (product CRUD, inventory receiving endpoints, sales checkout, subscription billing APIs, onboarding approval workflows) or React UI screens have been implemented prematurely.**

The project is in a clean, fully verified state and ready for **Phase Three: Core Business Logic, API Endpoints, and Portal Integration**.
