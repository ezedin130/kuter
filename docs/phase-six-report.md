# Kuter — Phase Six Completion Report

## 1. Executive Summary

Phase Six of the **Kuter Multi-Tenant Building Materials Inventory Management SaaS** project is complete. This phase designed, implemented, thoroughly tested, and documented the subscription management and platform-administration backend foundations, including subscription plan catalog management, public pricing directory, tenant subscription lifecycle management, real-time dynamic expiry resolution, plan-based entitlement and quota enforcement (products, users, active subscription gating), downgrade data preservation, platform-admin subscription overview with estimated MRR calculations, tenant-scoped subscription self-inspection, and an immutable subscription audit history ledger.

### Core Capabilities Delivered:

1. **Subscription Plan Catalog Management:** Full administrative CRUD (`/api/v1/subscription-plans/**`) with validation, unique code enforcement, active plan deletion protection, and monetary scale precision (`NUMERIC(15, 4)`).
2. **Public Pricing Directory API:** Read-only endpoint (`GET /api/v1/public/subscription-plans`) configured with Spring Security `permitAll()`, returning active and public plans for marketing/pricing screens without exposing administrative metadata.
3. **Tenant Subscription Lifecycle State Machine:** End-to-end status transitions across `PENDING`, `TRIALING`, `ACTIVE`, `EXPIRED`, `SUSPENDED`, `CANCELLED`, and `PAST_DUE`. Supported operations include direct plan assignment, activation, free trial initiation, plan switching, duration extension, suspension, reactivation, and immediate/end-of-period cancellation.
4. **Real-Time Dynamic Expiry Cutoff:** Evaluation of effective subscription status at point of request execution (`EntitlementService.getEffectiveStatus(tenantId)`) by comparing an injectable `java.time.Clock` against `current_period_end`. This guarantees zero latency access cutoff for expired shops without relying on background batch jobs.
5. **Deterministic Expiry Scheduler:** Hourly background reconciliation job (`SubscriptionExpiryScheduler`) that queries for elapsed active/trialing subscriptions, transitions their database status to `EXPIRED`, logs immutable audit records, and supports configuration toggles.
6. **Plan-Based Quota Limit Enforcement:**
   - Active catalog product limit enforced during product creation (`POST /api/v1/products`) returning `HTTP 409 Conflict` when reached.
   - Staff user seat limit enforced during staff member invitation (`POST /api/v1/tenants/{tenantId}/members`) returning `HTTP 409 Conflict` when reached.
   - Active subscription requirement enforced across sales checkout (`POST /api/v1/sales`), product creation, and staff additions returning `HTTP 403 Forbidden` if the subscription is not in an active or trialing state.
7. **Downgrade Data Preservation Policy:** When a tenant is switched or downgraded to a lower plan tier whose limits are lower than current usage, existing products and users remain active; creating additional resources beyond the new limit is strictly blocked.
8. **Platform-Admin Subscription Overview & Metrics:** Aggregated overview endpoint (`GET /api/v1/admin/subscriptions/summary`) reporting total subscriptions, active count, trialing count, expired count, suspended count, cancelled count, and estimated Monthly Recurring Revenue (MRR).
9. **Tenant Subscription Self-Inspection:** Shop-level self-inspection endpoint (`GET /api/v1/me/subscription`) providing plan name, effective status, days remaining, product and user usage counts against limits, and feature flags.
10. **Immutable Subscription Audit Ledger:** Append-only history table (`subscription_history`) capturing every state transition, plan change, extension, suspension, cancellation, mandatory justification reason, and actor identifier.

### Verification Results:

- **Backend Test Suite:** **141/141 tests passing** (`mvnw test` elapsed 50.377s, 0 failures, 0 errors, 0 skipped).
  - 35 comprehensive Phase Six subscription tests across 4 dedicated test suites.
  - 106 regression tests across Foundation, Security, Onboarding, Tenancy, Catalog, Inventory, and Sales modules.
- **React Admin Portal (`admin/`):** Clean production build via Vite 6.4.5 (`tsc -b && vite build` in 3.29s, 0 errors).
- **React Client Portal (`frontend/`):** Clean production build via Vite 6.4.5 (`tsc -b && vite build` in 3.63s, 0 errors).

---

## 2. Completed Scope & Deliverables

### 2.1 Subscription Plan Management APIs (`/api/v1/subscription-plans/**`)

- `POST /api/v1/subscription-plans`: Create plan with unique code, name, description, price, billing interval (`MONTHLY`, `ANNUAL`), currency (`USD`), trial days, product limit, user limit, branch limit, features list, public visibility, and active status.
- `GET /api/v1/subscription-plans`: Paginated, multi-filter list of plans with optional filtering by `status` and `isPublic`.
- `GET /api/v1/subscription-plans/{id}`: Detailed plan representation.
- `PUT /api/v1/subscription-plans/{id}`: Full update of plan configuration, pricing, and limits.
- `PATCH /api/v1/subscription-plans/{id}/status`: Toggle plan status (`ACTIVE`, `INACTIVE`, `ARCHIVED`).
- `DELETE /api/v1/subscription-plans/{id}`: Safe deletion of unreferenced plans. If referenced by any tenant subscription, returns `HTTP 409 Conflict`.

### 2.2 Public Pricing Directory API (`GET /api/v1/public/subscription-plans`)

- Publicly accessible without authentication (`permitAll()` in `SecurityConfig`).
- Filters plans strictly to `status = ACTIVE` and `isPublic = true`.
- Omits internal administrative fields and returns clean commercial pricing metadata.

### 2.3 Platform-Admin Subscription Lifecycle APIs (`/api/v1/admin/subscriptions/**`)

- `POST /api/v1/admin/subscriptions/assign`: Assigns a plan to a tenant with billing interval and optional trial period.
- `POST /api/v1/admin/subscriptions/{id}/activate`: Explicitly transitions pending or trialing subscription to `ACTIVE`.
- `POST /api/v1/admin/subscriptions/{id}/start-trial`: Starts a free trial with customized duration or plan default.
- `POST /api/v1/admin/subscriptions/{id}/change-plan`: Switches subscription plan tier and reapplies feature quotas.
- `POST /api/v1/admin/subscriptions/{id}/extend`: Extends validity by $N$ days with a mandatory audit reason. If previously `EXPIRED`, immediately restores status to `ACTIVE`.
- `POST /api/v1/admin/subscriptions/{id}/suspend`: Administratively suspends access with a mandatory reason.
- `POST /api/v1/admin/subscriptions/{id}/reactivate`: Restores a suspended subscription to active status.
- `POST /api/v1/admin/subscriptions/{id}/cancel`: Cancels subscription immediately or schedules cancellation at period end (`cancelAtPeriodEnd = true`).
- `GET /api/v1/admin/subscriptions`: Paginated listing of all tenant subscriptions with status and tenant filters.
- `GET /api/v1/admin/subscriptions/{id}`: Comprehensive detail of subscription, plan metadata, and usage statistics.
- `GET /api/v1/admin/subscriptions/tenant/{tenantId}`: Retrieves active subscription for a specific tenant.
- `GET /api/v1/admin/subscriptions/{id}/history`: Paginated audit history log of all transitions for a subscription.
- `GET /api/v1/admin/subscriptions/summary`: Aggregated metric cards and estimated MRR.

### 2.4 Tenant Subscription Self-Inspection API (`GET /api/v1/me/subscription`)

- Accessible to authenticated shop staff (`OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER`).
- Returns plan code, plan name, effective status, current period start and end, days remaining, auto-renew status, cancel-at-period-end status.
- Returns real-time quota usage: current product count vs product limit, and current user count vs user seat limit.
- Returns list of entitled feature strings.

---

## 3. Architectural & Domain Highlights

### 3.1 Real-Time Dynamic Expiry Cutoff vs Background Batch Processing

Relying solely on periodic batch jobs (cron schedulers) to enforce subscription expiration creates an authorization vulnerability: a shop whose subscription expired at 02:01 UTC could continue transacting until the next hourly sweep at 03:00 UTC.

**Kuter Dual-Layer Solution:**

```text
Request Arrives (e.g. POST /api/v1/sales)
   │
   ▼
EntitlementService.getEffectiveStatus(tenantId)
   │
   ├── Fetch TenantSubscription (status in DB)
   │
   ├── Instant now = clock.instant();  <-- Injected UTC Clock Bean
   │
   ├── IF (status == ACTIVE || status == TRIALING) AND (now.isAfter(currentPeriodEnd))
   │      │
   │      └── RETURN SubscriptionStatus.EXPIRED  (Real-Time Cutoff!)
   │
   └── ELSE
          └── RETURN subscription.getStatus()
```

- When `getEffectiveStatus(tenantId)` resolves to `EXPIRED`, `EntitlementService.assertCanPerformAction(tenantId, ...)` immediately throws `SubscriptionExpiredException` (`HTTP 403 Forbidden`).
- The hourly `SubscriptionExpiryScheduler` runs asynchronously to keep PostgreSQL database rows aligned for reporting and indexing.
- Injecting `java.time.Clock` ensures 100% deterministic, clock-manipulable unit and integration testing without brittle `Thread.sleep()` calls.

### 3.2 Plan-Based Entitlement and Quota Enforcement

Server-side quota checks are embedded directly in domain service operations:

1. **Product Catalog Quota (`ProductServiceImpl.createProduct`):**
   ```java
   entitlementService.assertCanPerformAction(tenantId, "create product");
   entitlementService.assertCanAddProduct(tenantId);
   ```
   Counts currently active products via `productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE)`. If $\text{count} \ge \text{plan.productLimit}$, throws `SubscriptionLimitExceededException` (`HTTP 409 Conflict`).

2. **Staff User Seat Quota (`TenantMembershipServiceImpl.addMember`):**
   ```java
   entitlementService.assertCanPerformAction(tenantId, "add member");
   entitlementService.assertCanAddUser(tenantId);
   ```
   Counts active memberships via `membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE)`. If $\text{count} \ge \text{plan.userLimit}$, throws `SubscriptionLimitExceededException` (`HTTP 409 Conflict`).

3. **Active Subscription Gating on Counter Sales (`SaleServiceImpl.createSale`):**
   ```java
   entitlementService.assertCanPerformAction(tenantId, "record sales");
   ```
   Ensures that expired, suspended, or cancelled shops cannot deduct inventory or process customer checkouts.

### 3.3 Downgrade Data Preservation Policy

When an administrator changes a tenant's plan from an Enterprise tier (e.g. 5,000 product limit) to a Starter tier (e.g. 50 product limit) while the tenant currently holds 120 products:
- **Zero Data Loss:** Existing catalog products and active user accounts are never automatically deleted or deactivated.
- **Enforcement on New Additions:** Attempting to create product #121 is rejected with `HTTP 409 Conflict` because $120 \ge 50$.
- The tenant retains full operational read access to their existing inventory while being gently guided to either upgrade or archive excess items.

### 3.4 Concurrency Control & Optimistic Locking

`TenantSubscription` entities incorporate `@Version private Long version;` mapped to `version BIGINT NOT NULL DEFAULT 0` in PostgreSQL. Concurrent plan updates or simultaneous administrative actions trigger JPA optimistic locking verification, preventing lost updates.

### 3.5 Immutable Subscription History Ledger

Every lifecycle modification automatically records a row in `subscription_history`:
- `subscription_id`: Foreign key to `tenant_subscriptions(id)`.
- `action`: `ASSIGNED`, `ACTIVATED`, `TRIAL_STARTED`, `PLAN_CHANGED`, `EXTENDED`, `SUSPENDED`, `REACTIVATED`, `CANCELLED`, `EXPIRED`.
- `previous_status` & `new_status`.
- `previous_plan_id` & `new_plan_id`.
- `effective_date`: Instant of modification.
- `reason`: Mandatory text justification for manual actions.
- `actor_id`: UUID of administrator or system user who executed the transition.

---

## 4. Database Migrations & Indexing

### Flyway Migration: `V6__subscription_enhancements.sql`

```sql
-- 1. Subscription Plans enhancements
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS trial_days INT NOT NULL DEFAULT 14;
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS branch_limit INT NOT NULL DEFAULT 1;

-- 2. Tenant Subscriptions lifecycle enhancements
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS billing_interval VARCHAR(20) NOT NULL DEFAULT 'MONTHLY';
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS trial_start TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS trial_end TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS is_current BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Subscription History audit ledger
CREATE TABLE IF NOT EXISTS subscription_history (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL REFERENCES tenant_subscriptions(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    previous_plan_id UUID REFERENCES subscription_plans(id),
    new_plan_id UUID REFERENCES subscription_plans(id),
    effective_date TIMESTAMP WITH TIME ZONE NOT NULL,
    reason TEXT,
    actor_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 4. Composite performance indexes
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_status_period ON tenant_subscriptions(status, current_period_end);
CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant_current ON tenant_subscriptions(tenant_id, is_current);
CREATE INDEX IF NOT EXISTS idx_subscription_history_sub_created ON subscription_history(subscription_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_subscription_history_tenant ON subscription_history(tenant_id, created_at DESC);
```

- Schema migration runs cleanly and idempotently across both PostgreSQL 18.6 and H2 test database.
- Composite index `(status, current_period_end)` provides high-performance lookups for the background expiry scheduler.
- `(tenant_id, is_current)` accelerates real-time subscription resolution for incoming tenant requests.

---

## 5. Security & Multi-Tenant Isolation

### 5.1 Authorization Matrix

| Endpoint | HTTP Method | Allowed Roles / Scope | Security Behavior |
| :--- | :---: | :--- | :--- |
| `/api/v1/public/subscription-plans` | `GET` | Public (`permitAll()`) | Returns active, public plans; excludes internal admin fields |
| `/api/v1/subscription-plans/**` | Any | Platform Admin (`PLATFORM_ADMIN`) | Full plan catalog management; non-admins receive `403 Forbidden` |
| `/api/v1/admin/subscriptions/**` | Any | Platform Admin (`PLATFORM_ADMIN`) | Full lifecycle administration; non-admins receive `403 Forbidden` |
| `/api/v1/me/subscription` | `GET` | Authenticated Tenant Staff | Inspects own tenant's subscription and real-time usage |
| `/api/v1/tenants/me/subscription` | `GET` | Authenticated Tenant Staff | Alias for `/api/v1/me/subscription` |

### 5.2 Tenant Isolation Defense

- Tenant users cannot inspect or manipulate other tenants' subscriptions or audit history.
- Context tampering by passing an arbitrary `X-Tenant-ID` header is rejected with `HTTP 403 Forbidden` by `TenantContextFilter`.
- Platform admin subscription operations strictly reference explicit target `tenantId` paths without requiring `TenantContext` switching.

---

## 6. Comprehensive Automated Test Verification

### 6.1 Phase Six Subscription Test Suites (35 Tests)

1. **`SubscriptionPlanTests` (9 Tests):**
   - `adminCanCreatePlan`: Verifies administrative plan creation with pricing, limits, and features.
   - `cannotCreatePlanWithDuplicateCode`: Verifies HTTP 409 Conflict when creating duplicate plan code.
   - `cannotCreatePlanWithInvalidData`: Verifies validation rules (blank name, negative limits).
   - `publicEndpointReturnsOnlyActiveAndPublicPlans`: Verifies public endpoint filtering and security permitAll.
   - `adminCanUpdatePlan`: Verifies full plan modifications.
   - `adminCanTogglePlanStatus`: Verifies patch status updates.
   - `cannotDeletePlanReferencedByTenant`: Verifies foreign key deletion protection (HTTP 409 Conflict).
   - `adminCanDeleteUnreferencedPlan`: Verifies clean deletion of unused plans.
   - `nonAdminCannotManagePlans`: Verifies HTTP 403 Forbidden for non-admin accounts.

2. **`SubscriptionLifecycleTests` (11 Tests):**
   - `adminCanAssignSubscriptionToTenant`: Verifies initial plan assignment and history entry.
   - `adminCanStartFreeTrial`: Verifies trial initiation with dates and `TRIALING` status.
   - `adminCanActivateSubscription`: Verifies explicit activation of pending subscription.
   - `adminCanChangePlan`: Verifies tier switching and updating entitlements.
   - `adminCanExtendSubscription`: Verifies period extension, mandatory reason, and history logging.
   - `adminCanSuspendSubscription`: Verifies suspension with mandatory reason.
   - `adminCanReactivateSubscription`: Verifies reactivation back to `ACTIVE`.
   - `adminCanCancelSubscriptionImmediately`: Verifies immediate cancellation and timestamping.
   - `adminCanScheduleCancellationAtPeriodEnd`: Verifies `cancel_at_period_end = true` flag.
   - `extendingRequiresNonBlankReason`: Verifies HTTP 400 when extension reason is missing.
   - `suspendingRequiresNonBlankReason`: Verifies HTTP 400 when suspension reason is missing.

3. **`SubscriptionExpiryAndEntitlementTests` (9 Tests):**
   - `activeSubscriptionAllowsProductCreation`: Verifies normal catalog operations for active tenant.
   - `expiredSubscriptionBlocksProductCreation`: Verifies dynamic expiry evaluation rejects product creation (HTTP 403 Forbidden).
   - `expiredSubscriptionBlocksSales`: Verifies dynamic expiry evaluation rejects counter sales (HTTP 403 Forbidden).
   - `expiredSubscriptionBlocksAddingMembers`: Verifies dynamic expiry evaluation rejects adding staff members (HTTP 403 Forbidden).
   - `extensionRestoresAccessImmediately`: Verifies extending an expired subscription immediately restores product creation without waiting for background jobs.
   - `productLimitEnforcedWhenExceeded`: Verifies HTTP 409 Conflict when catalog reaches plan product limit.
   - `userLimitEnforcedWhenExceeded`: Verifies HTTP 409 Conflict when staff members reach plan user seat limit.
   - `downgradePreservesExistingData`: Verifies existing products remain active on lower plan, while new additions are blocked.
   - `scheduledExpiryJobTransitionsElapsedSubscriptions`: Verifies background scheduler sweeps elapsed subscriptions to DB status `EXPIRED` and logs history.

4. **`SubscriptionIsolationAndSecurityTests` (6 Tests):**
   - `shopOwnerCanInspectOwnSubscriptionAndUsage`: Verifies `/api/v1/me/subscription` returns accurate limits, counts, and days remaining.
   - `nonAdminCannotAccessAdminSubscriptionEndpoints`: Verifies HTTP 403 Forbidden for tenant owners attempting admin calls.
   - `tenantCannotAccessOtherTenantSubscription`: Verifies cross-tenant data isolation.
   - `adminCanQuerySubscriptionSummary`: Verifies platform metrics summary, counts, and estimated MRR calculations.
   - `subscriptionHistoryTracksAllTransitions`: Verifies immutable audit records across multiple lifecycle transitions.
   - `tamperingTenantHeaderForbidden`: Verifies header spoofing rejection by `TenantContextFilter`.

### 6.2 Full Test Suite Summary

```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.kuter.catalog.CategoryAndProductApiTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.health.HealthApiTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.identity.AuthenticationApiTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.identity.AuthenticationAndSecurityTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.inventory.CatalogAndConversionTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.inventory.InventoryLedgerTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.inventory.InventoryReportingAndIsolationTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.inventory.StockAdjustmentAndLedgerTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.inventory.StockReceivingAndCostingTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.onboarding.OnboardingWorkflowTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.PostgreSqlIntegrationTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.sales.SaleConcurrencyTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.sales.SaleCreationAndDeductionTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.sales.SalesReportAndIsolationTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.sales.SaleVoidingAndReversalTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.subscriptions.SubscriptionExpiryAndEntitlementTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.subscriptions.SubscriptionIsolationAndSecurityTests
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.subscriptions.SubscriptionLifecycleTests
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.subscriptions.SubscriptionPlanTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.tenancy.TenantAndMembershipManagementTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.tenancy.TenantIsolationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 141, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 7. Out-of-Scope Confirmation & Boundary Adherence

In strict adherence to the project instructions:
1. **Payments & Gateways:** No payment collection, card tokens, webhook handlers, Stripe/PayPal SDKs, or invoicing ledgers were introduced. Plan prices represent descriptive commercial metadata.
2. **General Ledger Accounting:** No accounting journals, debit/credit entries, or tax authority invoice generation was added.
3. **Cash Drawer Reconciliation:** No register balance counting or cash drawer workflows were built.
4. **React Portals:** No frontend UI screens or client subscription pages were constructed. Existing Vite production builds for `admin/` and `frontend/` were verified to guarantee zero frontend build regressions.

---

## 8. Phase Six Sign-Off & Readiness for Phase Seven

Phase Six is complete, fully verified, and documented. The subscription management and platform-administration backend is resilient, secure, mathematically sound across limits and pricing, dynamically responsive to subscription expiry without batch latency, and backed by 141 passing automated tests.

The platform is ready to proceed to **Phase Seven: Platform Administration & Shop Tenant Portals (React)**.
