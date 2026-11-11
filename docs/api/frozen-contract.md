# Kuter Backend — Frozen API Specification Contract
**Phase Seven Finalization & API Freeze**
**Status:** FROZEN — Backend Complete & Ready for React Frontends
**Version:** 1.0.0
**Base Path:** `/api/v1`

---

## 1. Global Architectural & Security Conventions

### 1.1 Authentication & Authorization
- **Header:** `Authorization: Bearer <jwt-token>`
- **Token Type:** Stateless HMAC-SHA256 Signed JSON Web Token.
- **Roles:**
  - Platform Level: `ROLE_PLATFORM_ADMIN`
  - Tenant Level: `ROLE_TENANT_OWNER`, `ROLE_TENANT_MANAGER`, `ROLE_TENANT_CASHIER`
- **Tenant Context Scoping:**
  - All tenant-scoped endpoints require the HTTP header `X-Tenant-ID: <uuid>` or authenticate via the JWT token's embedded tenant context.
  - The server verifies that the authenticated user holds an active membership for the requested tenant.
  - Cross-tenant requests are strictly rejected with `403 Forbidden` or `404 Not Found` (preventing ID enumeration).

### 1.2 Correlation & Observability
- **Header:** `X-Correlation-ID: <uuid>` (client may supply; server generates if missing).
- Every response echoes the correlation ID in the header and in the body wrapper.

### 1.3 Uniform Envelope Formats

#### Standard Success Envelope (`ApiResponse<T>`)
```json
{
  "success": true,
  "message": "Human-readable status message",
  "data": { ... },
  "timestamp": "2026-10-29T11:30:00Z",
  "correlationId": "0d5590ad-aba9-4d29-a2ce-c55fca924a66"
}
```

#### Standard Paginated Envelope (`ApiResponse<PageResponse<T>>`)
```json
{
  "success": true,
  "message": "Resource list retrieved successfully",
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 42,
    "totalPages": 3,
    "last": false,
    "first": true,
    "empty": false
  },
  "timestamp": "2026-10-29T11:30:00Z",
  "correlationId": "0d5590ad-aba9-4d29-a2ce-c55fca924a66"
}
```

#### Standard Error Envelope (`ApiResponse<Void>`)
```json
{
  "success": false,
  "message": "Validation failed: [field: error description]",
  "data": null,
  "timestamp": "2026-10-29T11:30:00Z",
  "correlationId": "0d5590ad-aba9-4d29-a2ce-c55fca924a66"
}
```

### 1.4 Date and Decimal Standards
- **Dates & Times:** UTC ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss'Z'`) or date format (`yyyy-MM-dd`). Start dates default to 00:00:00 UTC, end dates to 23:59:59.999999999 UTC.
- **Numbers:** Quantities, currency, prices, conversion factors, and valuations are formatted as high-precision decimals (`NUMERIC(15, 4)`).
- **Payment Gateway Disclaimer:** Kuter does not process card/gateway payments. Sales and receipts represent immutable operational records, not proof of financial payment.

### 1.5 Interactive OpenAPI Documentation
- **OpenAPI 3.0 JSON Spec:** `GET /v3/api-docs`
- **Swagger UI Interactive Console:** `GET /swagger-ui/index.html`

---

## 2. API Endpoint Matrix

### 2.1 System & Health

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/health` | Public | Returns database connection status, active profile, and system status |
| `GET` | `/actuator/health` | Public | Spring Boot Actuator liveness and readiness probes |

---

### 2.2 Public Website & Onboarding

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/public/plans` | Public | List published commercial subscription plans for the public pricing page |
| `POST` | `/api/v1/public/onboarding/request` | Public | Inbound registration request from shop owner on public landing page |
| `GET` | `/api/v1/public/onboarding/check-email` | Public | Check if owner email is already registered or has a pending onboarding request |

---

### 2.3 Identity & Authentication

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public / Admin | Register new user account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate with email/password; returns JWT access token and user memberships |
| `GET` | `/api/v1/auth/me` | Authenticated | Current user profile, platform admin flag, active tenant memberships |
| `GET` | `/api/v1/auth/memberships` | Authenticated | List all tenant memberships for the current user |

---

### 2.4 Platform Administration (`ROLE_PLATFORM_ADMIN`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/metrics/overview` | Platform overview: total/active/suspended shops, onboarding requests, active subscriptions, estimated MRR |
| `GET` | `/api/v1/admin/tenants` | List all tenant shops (filterable by `status`, `search`, pageable) |
| `POST` | `/api/v1/admin/tenants` | Directly provision a new tenant shop and initial owner |
| `GET` | `/api/v1/admin/tenants/{id}` | Detailed tenant shop view |
| `GET` | `/api/v1/admin/tenants/{id}/status` | Combined operational tenant status and subscription lifecycle details |
| `PUT` | `/api/v1/admin/tenants/{id}` | Update tenant metadata (name, currency, timezone, registration number) |
| `PATCH` | `/api/v1/admin/tenants/{id}/status` | Activate, suspend, or terminate a tenant shop |
| `GET` | `/api/v1/admin/onboarding` | List onboarding requests (filter by `status`, `search`, pageable) |
| `GET` | `/api/v1/admin/onboarding/{id}` | Retrieve single onboarding request |
| `POST` | `/api/v1/admin/onboarding/{id}/approve` | Approve onboarding request: provisions tenant shop, owner user, trial subscription |
| `POST` | `/api/v1/admin/onboarding/{id}/reject` | Reject onboarding request with mandatory reviewer reason |
| `GET` | `/api/v1/admin/plans` | List all subscription plans (public and internal) |
| `POST` | `/api/v1/admin/plans` | Create a new subscription tier |
| `PUT` | `/api/v1/admin/plans/{id}` | Update plan limits, price, public visibility, active status |
| `DELETE` | `/api/v1/admin/plans/{id}` | Soft-deactivate a subscription plan |
| `GET` | `/api/v1/admin/subscriptions` | Query tenant subscriptions platform-wide (filterable by `status`, `planId`, pageable) |
| `POST` | `/api/v1/admin/subscriptions/assign` | Manually assign or override a tenant's subscription plan |
| `GET` | `/api/v1/admin/audit-logs` | Platform audit log query (filter by `tenantId`, `action`, `entityType`, `outcome`, date range) |

---

### 2.5 Tenant Shop Self-Management & Memberships

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/tenants/me` | Authenticated Shop User | Get current shop details |
| `GET` | `/api/v1/tenants/me/status` | Authenticated Shop User | Current shop operational status, subscription lifecycle, effective status, days remaining |
| `PUT` | `/api/v1/tenants/me` | `OWNER`, `MANAGER` | Update shop name, currency, timezone |
| `GET` | `/api/v1/tenants/memberships` | `OWNER`, `MANAGER` | List staff memberships for the shop |
| `POST` | `/api/v1/tenants/memberships` | `OWNER` | Invite / add staff member (`MANAGER` or `CASHIER`) |
| `PATCH` | `/api/v1/tenants/memberships/{id}/role` | `OWNER` | Update staff member role |
| `PATCH` | `/api/v1/tenants/memberships/{id}/status` | `OWNER` | Activate or suspend staff membership |

---

### 2.6 Tenant Subscriptions & Entitlements

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/subscriptions/current` | Authenticated Shop User | Active subscription details, plan limits, expiry dates |
| `GET` | `/api/v1/subscriptions/usage` | Authenticated Shop User | Current resource consumption vs plan limits (products count, users count) |
| `POST` | `/api/v1/subscriptions/change-plan` | `OWNER` | Upgrade or switch subscription tier |
| `POST` | `/api/v1/subscriptions/cancel` | `OWNER` | Request cancellation at end of period or immediate |
| `POST` | `/api/v1/subscriptions/resume` | `OWNER` | Undo pending cancellation before current period expires |

---

### 2.7 Product Catalog & Dual-UOM Conversions

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/categories` | Shop User | List product categories |
| `POST` | `/api/v1/categories` | `OWNER`, `MANAGER` | Create product category |
| `GET` | `/api/v1/categories/{id}` | Shop User | Get category detail |
| `PUT` | `/api/v1/categories/{id}` | `OWNER`, `MANAGER` | Update category |
| `DELETE` | `/api/v1/categories/{id}` | `OWNER`, `MANAGER` | Soft-delete category |
| `GET` | `/api/v1/units` | Shop User | List tenant measurement units |
| `POST` | `/api/v1/units` | `OWNER`, `MANAGER` | Create measurement unit |
| `GET` | `/api/v1/products` | Shop User | List products (filter by `categoryId`, `status`, `search`, pageable) |
| `POST` | `/api/v1/products` | `OWNER`, `MANAGER` | Create product (enforces subscription product limit) |
| `GET` | `/api/v1/products/{id}` | Shop User | Get product detail |
| `PUT` | `/api/v1/products/{id}` | `OWNER`, `MANAGER` | Update product catalog details |
| `DELETE` | `/api/v1/products/{id}` | `OWNER` | Soft-delete product |
| `GET` | `/api/v1/products/{id}/conversions` | Shop User | List secondary units & conversion factors for product |
| `POST` | `/api/v1/products/{id}/conversions` | `OWNER`, `MANAGER` | Add secondary unit conversion factor |
| `DELETE` | `/api/v1/products/{id}/conversions/{unitId}` | `OWNER`, `MANAGER` | Remove secondary unit conversion |

---

### 2.8 Inventory Management, Costing & Stock Reports

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/v1/inventory/receipts` | `OWNER`, `MANAGER` | Inbound stock receiving (updates moving weighted-average unit cost, appends ledger) |
| `GET` | `/api/v1/inventory/receipts` | Shop User | List receiving history |
| `GET` | `/api/v1/inventory/receipts/{id}` | Shop User | Get receiving record with line items |
| `POST` | `/api/v1/inventory/adjustments` | `OWNER`, `MANAGER` | Record stock adjustment (damage, shrinkage, count delta; pessimistic lock) |
| `GET` | `/api/v1/inventory/adjustments` | Shop User | List adjustment history |
| `GET` | `/api/v1/inventory/adjustments/{id}` | Shop User | Get adjustment record with line items |
| `GET` | `/api/v1/inventory/balances/{productId}` | Shop User | Get current stock balance and weighted-average cost |
| `GET` | `/api/v1/inventory/movements` | Shop User | Query immutable stock movement ledger (`startDate`, `endDate`, `referenceType`, `actorId`) |
| `GET` | `/api/v1/inventory/reports/current` | Shop User | Current inventory report with stock status and dual-UOM conversion breakdown |
| `GET` | `/api/v1/inventory/reports/valuation` | Shop User | High-level inventory valuation report with category breakdowns |
| `GET` | `/api/v1/inventory/reports/valuation/items` | Shop User | Paged itemized stock valuation report with average costs and valuations |
| `GET` | `/api/v1/inventory/reports/low-stock` | Shop User | List items at or below reorder threshold |

---

### 2.9 Sales Processing & Sales Reporting

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `POST` | `/api/v1/sales` | `OWNER`, `MANAGER`, `CASHIER` | Create and finalize sale (atomic stock deduction, dual-UOM resolution, snapshot COGS) |
| `GET` | `/api/v1/sales` | Shop User | List sales (filter by `startDate`, `endDate`, `status`, `productId`, `cashierId`, `search`) |
| `GET` | `/api/v1/sales/{id}` | Shop User | Get sale detail with line items |
| `GET` | `/api/v1/sales/{id}/receipt` | Shop User | Printable operational sales receipt (includes disclaimer: not proof of payment) |
| `POST` | `/api/v1/sales/{id}/void` | `OWNER`, `MANAGER` | Void/cancel sale (restores stock balances and appends reversal ledger entries) |
| `GET` | `/api/v1/sales/reports/summary` | Shop User | Aggregate sales summary (cost and gross profit redacted for Cashier) |
| `GET` | `/api/v1/sales/reports/daily` | Shop User | Daily interval sales breakdown (`salesCount`, `salesAmount`, `costOfGoodsSold`) |
| `GET` | `/api/v1/sales/reports/products` | Shop User | Top-selling products ranking by volume and revenue |

---

### 2.10 User Activity & Audit Logging

| Method | Endpoint | Authorization | Description |
|---|---|---|---|
| `GET` | `/api/v1/audit-logs` | `OWNER`, `MANAGER` | Query tenant-scoped audit logs (filter by `action`, `entityType`, `outcome`, date range) |

---

## 3. Schema Freeze Commitment
All requests, responses, parameter names, error structures, status codes, and HTTP method mappings listed in this document are **frozen**. Frontends (Shop Web Portal, Admin Portal, and Public Marketing Site) can proceed to consume these endpoints with complete confidence against the backend implementation.
