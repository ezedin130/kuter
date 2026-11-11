# Kuter — Multi-Tenant Isolation Architecture

## 1. Overview

**Kuter** uses a **Shared Database, Discriminator Column (`tenant_id`)** multi-tenancy model. Every tenant-scoped table explicitly includes a non-null, immutable foreign key column:

```sql
tenant_id UUID NOT NULL,
CONSTRAINT fk_<table>_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
```

Because building materials shops store proprietary pricing, supplier agreements, and stock levels, tenant boundaries must be rigorously defended across:
1. **HTTP Ingestion Layer:** Rejecting arbitrary client-supplied tenant headers.
2. **Context Resolution Layer:** Thread-safe, leak-free request scoping.
3. **Domain & Data Access Layer:** Tenant-scoped queries and unique constraints.
4. **Tenant Lifecycle Enforcement Layer:** Immediate blocking of requests for suspended tenants.

---

## 2. Tenant Context Resolution Flow

```mermaid
sequenceDiagram
    autonumber
    actor Client as Authenticated Client
    participant Filter as TenantContextFilter
    participant Sec as SecurityContext (KuterUserPrincipal)
    participant Ctx as TenantContext (ThreadLocal)
    participant Handler as Service / Repository
    participant DB as PostgreSQL Database

    Client->>Filter: HTTP Request [X-Tenant-ID: <Target-Tenant-UUID>]
    Filter->>Sec: Retrieve Authenticated Principal
    alt Principal has active membership in requested tenant
        Filter->>DB: Check Tenant status in database
        alt Tenant status is ACTIVE
            Filter->>Ctx: TenantContext.setTenantId(requestedTenantId)
            Filter->>Handler: filterChain.doFilter()
            Handler->>DB: Query WHERE tenant_id = :tenantId
            DB-->>Handler: Tenant-isolated records
            Handler-->>Client: 200 OK Response
        else Tenant status is SUSPENDED
            Filter-->>Client: 403 Forbidden ("Tenant shop is suspended...")
        end
    else Cross-tenant spoofing attempt (no membership)
        Filter-->>Client: 403 Forbidden ("Access denied: you do not hold an active membership...")
    end
    Note over Filter,Ctx: finally { TenantContext.clear(); }
```

---

## 3. Core Architectural Components

### 3.1 `TenantContext` (ThreadLocal Encapsulation)

[`TenantContext`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/TenantContext.java) encapsulates a static `ThreadLocal<UUID>`:
- `setTenantId(UUID)`: Binds tenant ID to the active request worker thread.
- `getTenantId()`: Returns `Optional<UUID>`.
- `requireTenantId()`: Returns `UUID` or throws `TenantNotFoundException` if unbound.
- `clear()`: Cleans the `ThreadLocal` immediately.

### 3.2 `TenantContextFilter` (Security & Lifecycle Enforcement)

[`TenantContextFilter`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/TenantContextFilter.java) executes inside the Spring Security filter chain immediately after [`JwtAuthenticationFilter`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/JwtAuthenticationFilter.java):

1. **Header Validation:** Inspects `X-Tenant-ID`. If invalid UUID syntax, immediately writes HTTP 403 JSON.
2. **Cryptographic Membership Check:** Validates whether the authenticated [`KuterUserPrincipal`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/KuterUserPrincipal.java) has an active membership in `tenant_memberships` for the requested tenant.
3. **Cross-Tenant Attack Defense:** If the user attempts to access a shop they do not belong to, the filter blocks request dispatch and writes standard 403 JSON:
   ```json
   {
     "status": 403,
     "error": "Forbidden",
     "message": "Access denied: you do not hold an active membership in the requested tenant shop",
     "path": "/api/v1/...",
     "correlationId": "42b47eb6-..."
   }
   ```
4. **Tenant Lifecycle Status Check:** If the tenant has `status == TenantStatus.SUSPENDED`, requests are immediately rejected with HTTP 403 Forbidden (`"Tenant shop is suspended. Please contact platform administration."`), preventing suspended accounts from accessing API services even if their JWT is otherwise valid.
5. **Single-Tenant Convenience:** If `X-Tenant-ID` is omitted but the user belongs to exactly one tenant shop, the filter automatically activates that tenant's context (provided the tenant is not suspended).
6. **Servlet Filter Registration Decoupling:** In Spring Boot, beans implementing `Filter` are registered by default in the root servlet container unless explicitly disabled. `SecurityConfig` defines a `FilterRegistrationBean<TenantContextFilter>` with `setEnabled(false)` so that `TenantContextFilter` executes exclusively inside Spring Security after JWT authentication has verified the principal.
7. **Leak-Proof Guarantee:** The entire request lifecycle is wrapped in `try { ... } finally { TenantContext.clear(); }`. Regardless of unhandled runtime exceptions or aborted requests, the thread local is purged before the thread returns to HikariCP / Tomcat pool.

### 3.3 Platform Administrator Boundaries

A critical architectural requirement is that SaaS operators (platform administrators) must not automatically see or query tenant business data:
- Platform administrators hold `isPlatformAdmin = true` and `ROLE_PLATFORM_ADMIN`.
- A platform administrator does **not** possess tenant memberships by default.
- If a platform admin attempts to pass `X-Tenant-ID` for a merchant yard without an explicit assigned membership, `TenantContextFilter` rejects the request with HTTP 403 Forbidden.
- Platform administration operations (reviewing registration requests, managing subscription plans, monitoring platform metrics) operate strictly via platform admin endpoints that do not rely on tenant context.
- Verified in [`TenantIsolationTests.java`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantIsolationTests.java).

---

## 4. Data Access Layer Safeguards

### 4.1 Base Entity Abstraction

Domain entities extend [`TenantAwareBaseEntity`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/common/TenantAwareBaseEntity.java):
```java
@MappedSuperclass
public abstract class TenantAwareBaseEntity extends BaseEntity implements TenantAware {
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
```
- `tenant_id` is marked `updatable = false` in Hibernate, preventing updates from moving an entity from one tenant to another.

### 4.2 Explicit Repository Scoping

Every repository query for tenant data requires the active tenant ID:
```java
Optional<Product> findByTenantIdAndId(UUID tenantId, UUID id);
Optional<Product> findByTenantIdAndSku(UUID tenantId, String sku);
Page<Product> findAllByTenantId(UUID tenantId, Pageable pageable);
```
Querying by entity ID alone across tenant boundaries returns `Optional.empty()`, preventing unauthorized cross-tenant data retrieval.

### 4.3 Tenant-Scoped Unique Constraints

Building materials yards use common, intuitive codes (e.g. `BAG` for 50kg bag, `CEM-425` for Portland cement, `INV-001` for the first invoice). Unique constraints are scoped strictly by `tenant_id`:

| Table | Constraint Name | Columns |
|---|---|---|
| `categories` | `uk_categories_tenant_name` | `(tenant_id, name)` |
| `unit_definitions` | `uk_units_tenant_code` | `(tenant_id, code)` |
| `products` | `uk_products_tenant_sku` | `(tenant_id, sku)` |
| `stock_balances` | `uk_stock_balance_tenant_product` | `(tenant_id, product_id)` |
| `stock_receivings` | `uk_receiving_tenant_number` | `(tenant_id, receiving_number)` |
| `stock_adjustments` | `uk_adj_tenant_number` | `(tenant_id, adjustment_number)` |
| `sales` | `uk_sales_tenant_invoice` | `(tenant_id, invoice_number)` |
| `sales` | `uk_sales_tenant_idempotency` | `(tenant_id, idempotency_key)` |

**Verification:** Verified in [`TenantIsolationTests.java`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantIsolationTests.java):
- Creating SKU `CEM-425` in Tenant Alpha and `CEM-425` in Tenant Beta simultaneously succeeds.
- Creating duplicate `CEM-425` within Tenant Alpha throws `DataIntegrityViolationException`.

---

## 5. Inventory & Catalog Role Authorization Matrix

Within an active tenant shop, fine-grained access control is enforced using Spring Method Security via `@tenantSecurity`:

| Resource & Operation | Endpoint | `@tenantSecurity` Check | `OWNER` | `SHOP_MANAGER` | `STOREKEEPER` | `CASHIER` | `PLATFORM_ADMIN` |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Catalog Read** | `GET /api/v1/categories`, `GET /api/v1/units`, `GET /api/v1/products` | `canReadInventory()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Catalog Mutate** | `POST/PUT/DELETE /api/v1/categories`, `products`, `units` | `canManageCatalog()` | ✅ | ✅ | ✅ | ❌ (`403`) | ✅ |
| **Unit Conversions**| `POST/DELETE /api/v1/products/{id}/units` | `canManageCatalog()` | ✅ | ✅ | ✅ | ❌ (`403`) | ✅ |
| **Inbound Receiving**| `POST /api/v1/inventory/receipts` | `canReceiveStock()` | ✅ | ✅ | ✅ | ❌ (`403`) | ✅ |
| **Stock Adjustments**| `POST /api/v1/inventory/adjustments` | `canAdjustStock()` | ✅ | ✅ | ✅ | ❌ (`403`) | ✅ |
| **Balance & Ledger**| `GET /api/v1/inventory/balances`, `movements` | `canReadInventory()` | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Inventory Reports**| `GET /api/v1/inventory/reports/...` | `canReadInventory()` | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 6. Sales & Operational Reporting Role Authorization Matrix (Phase Five)

Retail checkout and sales analytics enforce strict operational separation between counter sales staff and shop executives:

| Resource & Operation | Endpoint | `@tenantSecurity` Check | `OWNER` | `SHOP_MANAGER` | `STOREKEEPER` | `CASHIER` | `PLATFORM_ADMIN` |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Create Counter Sale** | `POST /api/v1/sales` | `canCreateSale()` | ✅ | ✅ | ❌ (`403`) | ✅ | ✅ |
| **List & View Sales** | `GET /api/v1/sales`, `GET /api/v1/sales/{id}` | `canReadSales()` | ✅ (Full) | ✅ (Full) | ❌ (`403`) | ✅ (Costs Redacted) | ✅ |
| **Printable Receipt** | `GET /api/v1/sales/{id}/receipt` | `canReadSales()` | ✅ | ✅ | ❌ (`403`) | ✅ | ✅ |
| **Void Completed Sale**| `POST /api/v1/sales/{id}/void` | `canVoidSale()` | ✅ | ✅ | ❌ (`403`) | ❌ (`403`) | ✅ |
| **Sales Summary Report**| `GET /api/v1/sales/reports/summary` | `canReadSales()` | ✅ (With COGS & Profit) | ✅ (With COGS & Profit) | ❌ (`403`) | ✅ (Financials Redacted) | ✅ |
| **Product Sales Ranking**| `GET /api/v1/sales/reports/products` | `canReadSales()` | ✅ (With COGS & Profit) | ✅ (With COGS & Profit) | ❌ (`403`) | ✅ (Financials Redacted) | ✅ |

### 6.1 Defense-in-Depth Verification
Automated test suites verify complete isolation and security enforcement:
- **`SaleCreationAndDeductionTests`:** Storekeepers cannot process counter sales (`403 Forbidden`); Cashiers receive completed sale responses with wholesale cost and profit margins stripped; cross-tenant inventory deductions are strictly prevented.
- **`SaleVoidingAndReversalTests`:** Cashiers cannot void sales (`403 Forbidden`); only owners/managers can void orders and execute compensating stock movements.
- **`SaleConcurrencyTests`:** Multi-threaded counter checkouts lock stock balances strictly within the tenant boundary in canonical product ID order, avoiding deadlocks and race conditions.
- **`SalesReportAndIsolationTests`:** Cross-tenant sales lookups, receipts, voids, and summary reports are strictly isolated (`404 Not Found` for unauthorized tenant requests); Cashier role financial redactions are verified.

---

## 7. Subscription & Entitlement Isolation (Phase Six)

### 7.1 Subscription Data Isolation
- **Tenant Self-Inspection:** `GET /api/v1/me/subscription` and `GET /api/v1/me/subscription/history` resolve the caller's active tenant from `TenantContext` (backed by the validated `X-Tenant-ID` header and user memberships).
- **Cross-Tenant Header Tampering Prevention:** A tenant user providing another tenant's ID in the `X-Tenant-ID` header is intercepted by `TenantContextFilter` and immediately rejected with `HTTP 403 Forbidden`.
- **History Isolation:** Subscription lifecycle history entries (`subscription_history`) are permanently partitioned by `tenant_id` and indexed via `idx_sub_history_tenant`. Tenants cannot view history events belonging to other shops.

### 7.2 Entitlement Scoping
- **Product Limits:** Active product counts are strictly tenant-scoped (`countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE)`). Creating products in Tenant A has zero effect on Tenant B's plan quota.
- **Seat Limits:** Active staff membership counts are strictly tenant-scoped (`countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)`).
- **Administrative Boundary Protection:** Platform subscription management endpoints (`/api/v1/subscription-plans`, `/api/v1/admin/subscriptions/**`) strictly enforce `hasRole('PLATFORM_ADMIN')`. Regular tenant owners and staff receive `403 Forbidden`.

### 7.3 Automated Verification
- **`SubscriptionIsolationAndSecurityTests`:** Validates cross-tenant subscription isolation, history partitioning, context tampering prevention, platform admin RBAC enforcement, and public plan catalog rules.
- **`SubscriptionExpiryAndEntitlementTests`:** Validates tenant-scoped product and user limit enforcement, dynamic expiry cutoff, and downgrade data preservation.

