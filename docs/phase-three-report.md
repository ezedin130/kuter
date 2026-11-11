# Kuter — Phase Three Completion Report

## 1. Executive Summary

Phase Three of the **Kuter Multi-Tenant Building Materials Inventory Management SaaS** project is complete. This phase designed, implemented, and verified the complete backend workflow for:
1. **Authentication & Session/Token Management:** Stateless JSON Web Token (JWT) infrastructure with HMAC-SHA256 signing, claims extraction, password verification, and account lifecycle protection.
2. **Current-User Identity & Permissions:** Profile retrieval (`/api/v1/users/me` and `/api/v1/auth/me`), active tenant memberships, and effective role-based permissions.
3. **Public Shop Registration & Onboarding Pipeline:** Public submission without upfront password collection (strictly matching the Figma prototype design), reference code issuance, and public status tracking.
4. **Platform Administration Review & Approval:** Review queue with filtering and text search, transactional tenant provisioning, owner account creation/linking, default subscription provisioning, and rejection with feedback.
5. **Tenant & Membership Management:** Direct tenant provisioning, tenant lifecycle status suspension/activation, tenant settings updates, and staff membership management (`OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER`).
6. **Security Auditing & Isolation Safeguards:** Append-only audit logging for all sensitive administrative and authentication operations, and cross-tenant spoofing defenses.

All 48 backend automated tests pass cleanly (48/48, 0 failures, 0 errors, 0 skipped), and both the React Admin and Frontend portals compile with zero errors.

---

## 2. Architecture & Modular Domain Packages

The Phase Three implementation extends the backend's modular monolith structure under `com.kuter`:

```text
com.kuter/
├── common/
│   ├── ApiResponse.java                 # Standard envelope {success, data, message, timestamp}
│   ├── ErrorResponse.java               # Standard envelope {status, error, message, path, correlationId, timestamp}
│   ├── PageResponse.java                # Pagination envelope {content, page, size, totalElements, ...}
│   ├── Constants.java                   # Route constants, header names, MDC keys
│   ├── CorrelationIdFilter.java         # End-to-end request tracing via SLF4J MDC
│   └── GlobalExceptionHandler.java      # Uniform JSON error mapping (400, 401, 403, 404, 409, 500)
├── config/
│   ├── SecurityConfig.java              # Stateless filter chain, route permits, FilterRegistrationBean decoupling
│   ├── JwtService.java                  # JJWT 0.12.6 HMAC-SHA256 token issuance & validation
│   ├── JwtAuthenticationFilter.java     # Parses Bearer JWT, populates SecurityContext with KuterUserPrincipal
│   ├── TenantContextFilter.java         # Enforces X-Tenant-ID against memberships & tenant lifecycle status
│   └── CorsConfig.java                  # Whitelists Admin (:5174) and Frontend (:5173) origins
├── identity/
│   ├── User.java, UserRepository.java, UserStatus.java
│   ├── TenantMembership.java, TenantMembershipRepository.java, TenantRole.java, MembershipStatus.java
│   ├── KuterUserPrincipal.java          # UserDetails with multi-tenant roles & platform admin flag
│   ├── CustomUserDetailsService.java    # Database-backed principal resolution
│   ├── AuthService.java, AuthServiceImpl.java # Login, logout, JWT issuance, profile retrieval
│   ├── TenantMembershipService.java, TenantMembershipServiceImpl.java # Member addition, role update, deletion
│   ├── AuthController.java              # /api/v1/auth/login, /api/v1/auth/logout, /api/v1/auth/me
│   ├── UserController.java              # /api/v1/users/me
│   ├── TenantMembershipController.java  # /api/v1/tenants/{tenantId}/members/**
│   └── dto/                             # LoginRequest, AuthResponse, UserProfileResponse, AddMemberDto, etc.
├── onboarding/
│   ├── TenantOnboardingRequest.java, TenantOnboardingRequestRepository.java, OnboardingStatus.java
│   ├── OnboardingService.java, OnboardingServiceImpl.java # Submission, review, approval transaction, rejection
│   ├── PublicOnboardingController.java  # POST /api/v1/onboarding-requests, GET .../status/{ref}
│   ├── AdminOnboardingController.java   # /api/v1/admin/onboarding-requests/**
│   └── dto/                             # CreateOnboardingRequestDto, OnboardingPublicResponse, ApproveOnboardingDto, etc.
├── tenancy/
│   ├── Tenant.java, TenantRepository.java, TenantStatus.java
│   ├── TenantContext.java               # ThreadLocal tenant storage with leak-proof cleanup
│   ├── TenantManagementService.java, TenantManagementServiceImpl.java # Direct provisioning, status, settings
│   ├── AdminTenantController.java       # /api/v1/admin/tenants/**
│   ├── TenantController.java            # /api/v1/tenants/me
│   ├── TenantSecurityService.java       # Method security evaluation @tenantSecurity.*
│   └── dto/                             # CreateTenantDto, UpdateTenantDto, TenantDetailResponse, etc.
└── audit/
    ├── AuditLog.java, AuditLogRepository.java
    └── AuditService.java, AuditServiceImpl.java # Append-only tamper-evident audit logger
```

---

## 3. Authentication & Stateless Token Architecture

### 3.1 Token Strategy & Rationale
We selected a **stateless JSON Web Token (JWT)** architecture implemented via the maintained **JJWT 0.12.6** library:
- **Scalability:** Enables complete statelessness (`SessionCreationPolicy.STATELESS`) without server-side memory sessions or centralized Redis cluster dependencies in V1.
- **HMAC-SHA256 Signing:** Tokens are signed using a 256-bit secret key (`app.jwt.secret`) configured via environment variables.
- **Expiration Policy:** Default lifespan of 24 hours (`app.jwt.expiration-ms=86400000`).
- **Decoupled Tenant Context:** Active tenant context is deliberately **not** baked into the JWT token payload. Since building materials operators may own or work in multiple yards with different permissions, the active tenant context is resolved dynamically on each HTTP request via the `X-Tenant-ID` header and checked against the user's active memberships.

### 3.2 Filter Ordering & Spring Filter Registration
Spring Boot automatically registers beans implementing `Filter` in the root servlet container unless explicitly told otherwise. To prevent `TenantContextFilter` or `JwtAuthenticationFilter` from executing before Spring Security has set up the context, we implemented explicit `FilterRegistrationBean` definitions with `.setEnabled(false)`:
1. `CorrelationIdFilter`: Injects `X-Correlation-ID` into SLF4J MDC.
2. `JwtAuthenticationFilter`: Parses `Authorization: Bearer <jwt>`, validates signature and expiration, loads `KuterUserPrincipal`, and populates `SecurityContext`.
3. `TenantContextFilter`: Reads `X-Tenant-ID`, validates membership, verifies tenant status is `ACTIVE` (rejects `SUSPENDED` with 403), sets `TenantContext`, and always clears `ThreadLocal` in a `finally` block.
4. `UsernamePasswordAuthenticationFilter`: Standard Spring Security endpoint authorization.

### 3.3 Security Hardening & Enumeration Defense
- **Generic 401 Responses:** Authentication failures return generic messages (`"Invalid username or password"`), preventing attackers from enumerating valid merchant emails.
- **Credential Protection:** The password hash is annotated with `@JsonIgnore` and is never returned in any API payload or logged in SLF4J.
- **Account Status Enforced:** Users marked as `SUSPENDED` or `INVITED` cannot log in (`isAccountNonLocked()` and `isEnabled()` return `false`).

---

## 4. Public Onboarding & Admin Review Pipeline

### 4.1 Alignment with Figma Prototype
In the Figma design reference (lines 1335-1348), the public onboarding form collects business metadata without requesting a password:
> *"No password yet. If approved, the owner will receive a secure account setup invitation."*

Our implementation strictly follows this specification:
1. **Public Submission:** The applicant submits business details (`shopName`, `ownerFullName`, `ownerEmail`, `ownerPhone`, `preferredShopCode`, `businessAddress`, `currency`, `requestedPlanCode`, `applicantNotes`).
2. **Duplicate Prevention:** Pending requests for an existing email are rejected with `409 Conflict`.
3. **Reference Code:** A human-friendly tracking code (`REQ-2026-XXXXX`) is generated and returned to the applicant.
4. **Public Status Tracking:** Applicants can check `GET /api/v1/onboarding-requests/status/{ref}` to view their status (`PENDING`, `APPROVED`, `REJECTED`) without disclosing private platform data.

### 4.2 Approval & Provisioning Transaction
When a platform administrator approves a request via `POST /api/v1/admin/onboarding-requests/{id}/approve`:
1. Validates that the request is currently `PENDING`.
2. Resolves or generates a unique URL-friendly tenant slug (`code`).
3. Saves the new `Tenant` entity with `status = ACTIVE`.
4. Checks if a `User` with the applicant's email already exists:
   - If found, links the existing user as `OWNER`.
   - If new, creates a user record with an encrypted temporary secret and `status = ACTIVE`.
5. Creates a `TenantMembership` binding the user to the tenant with `role = OWNER` and `status = ACTIVE`.
6. Resolves the requested `SubscriptionPlan` (falling back to `starter` if not found) and creates a `TenantSubscription` active for 30 days.
7. Updates the onboarding request with `status = APPROVED`, `reviewerId`, `reviewerNotes`, `reviewedAt`, and `createdTenantId`.
8. Records an immutable audit log entry in `audit_logs`.

---

## 5. Tenant & Membership Management APIs

### 5.1 Platform Administration Tenant Management
- `POST /api/v1/admin/tenants`: Allows administrators to provision a tenant directly.
- `GET /api/v1/admin/tenants`: Lists all tenants with pagination, status filter, and text search.
- `GET /api/v1/admin/tenants/{id}`: Returns complete tenant details including member count and active subscription.
- `PATCH /api/v1/admin/tenants/{id}/status`: Enables activating or suspending a tenant.

### 5.2 Tenant Lifecycle Suspension
Suspending a tenant immediately locks all access to that shop:
- When a tenant is `SUSPENDED`, `TenantContextFilter` intercepts any incoming request containing that tenant's ID and immediately returns `403 Forbidden` (`"Tenant shop is suspended. Please contact platform administration."`).
- This applies even if the user possesses a completely valid JWT token.

### 5.3 Staff Membership Management
Tenant Owners and Managers can manage staff members via `/api/v1/tenants/{tenantId}/members/**`:
- **Role Assignment:** Supports `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER`.
- **Method Security:** Method annotations (`@tenantSecurity.canManageMembersOf(#tenantId)`) verify the caller has authority over the target shop.
- **Sole Owner Protection:** A tenant shop must always have at least one active `OWNER`. Attempts to remove or demote the last active owner throw `BadRequestException`.

---

## 6. Database Migrations

Database schema evolution is managed strictly via Flyway:

### Migration `V3__onboarding_enhancements.sql`
- Added columns to `tenant_onboarding_requests`:
  - `preferred_shop_code VARCHAR(50)`
  - `business_address TEXT`
  - `currency VARCHAR(3) DEFAULT 'USD'`
  - `applicant_notes TEXT`
  - `created_tenant_id UUID REFERENCES tenants(id)`
- Added indexes on `status`, `owner_email`, `reference_code`, and `created_tenant_id`.
- Seeded baseline subscription tiers:
  - `starter`: 1 shop, 3 users, 500 products, 1,000 monthly transactions.
  - `growth`: 3 shops, 10 users, 5,000 products, 10,000 monthly transactions.
  - `enterprise`: 10 shops, 50 users, 50,000 products, unlimited transactions.
- Fully tested and verified on both H2 and PostgreSQL 18.6 (`kuter_dev` and `kuter_test`).

---

## 7. Automated Test Suite & Verification Evidence

The automated test suite was expanded to **48 comprehensive tests** across 10 test classes, covering domain constraints, security boundaries, REST endpoints, and database migrations.

### Test Execution Summary (`mvnw test`)
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.kuter.catalog.CatalogAndConversionTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in CatalogAndConversionTests
[INFO] Running com.kuter.HealthControllerTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in HealthControllerTests
[INFO] Running com.kuter.identity.AuthenticationAndSecurityTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in AuthenticationAndSecurityTests
[INFO] Running com.kuter.identity.AuthenticationApiTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in AuthenticationApiTests
[INFO] Running com.kuter.inventory.InventoryLedgerTests
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in InventoryLedgerTests
[INFO] Running com.kuter.KuterApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in KuterApplicationTests
[INFO] Running com.kuter.onboarding.OnboardingWorkflowTests
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in OnboardingWorkflowTests
[INFO] Running com.kuter.PostgreSqlIntegrationTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in PostgreSqlIntegrationTests
[INFO] Running com.kuter.SecurityAndErrorHandlingTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- in SecurityAndErrorHandlingTests
[INFO] Running com.kuter.tenancy.TenantAndMembershipManagementTests
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 -- in TenantAndMembershipManagementTests
[INFO] Running com.kuter.tenancy.TenantIsolationTests
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0 -- in TenantIsolationTests
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Detailed Test Coverage Matrix

| Test Suite Class | Tests Run | Result | Key Scenarios Verified |
| :--- | :---: | :---: | :--- |
| [`AuthenticationApiTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/identity/AuthenticationApiTests.java) | 7 | Passed | Successful login, JWT token issuance, bad credentials 401 generic error, suspended user 403, `/api/v1/auth/me` with active tenant, logout audit. |
| [`OnboardingWorkflowTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/onboarding/OnboardingWorkflowTests.java) | 9 | Passed | Public submission, reference code format, duplicate pending rejection, public status tracking, non-existent status 404, admin review pagination, approve provisioning (Tenant, Owner, Subscription), reject with feedback, unauthorized access rejection. |
| [`TenantAndMembershipManagementTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantAndMembershipManagementTests.java) | 8 | Passed | Direct tenant creation, tenant suspension immediate 403 enforcement, owner updating tenant settings, listing members, adding cashier member, updating role to manager, removing cashier, last owner deletion prevention. |
| [`TenantIsolationTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantIsolationTests.java) | 5 | Passed | Cross-tenant spoofing rejected with 403, platform admin without membership blocked from tenant context, legitimate member allowed, tenant-scoped unique SKUs, query isolation. |
| [`AuthenticationAndSecurityTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/identity/AuthenticationAndSecurityTests.java) | 5 | Passed | BCrypt password hashing, principal multi-tenant role loading, account lockout, audit trail. |
| [`PostgreSqlIntegrationTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/PostgreSqlIntegrationTests.java) | 2 | Passed | Real PostgreSQL 18.6 connection, Flyway migrations V1, V2, V3 applied, schema validation. |
| [`CatalogAndConversionTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/catalog/CatalogAndConversionTests.java) | 3 | Passed | Dual-UOM units, conversion factor precision, product SKU scoping. |
| [`InventoryLedgerTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/inventory/InventoryLedgerTests.java) | 3 | Passed | Stock balance updates, append-only movements, running balance auditing. |
| [`HealthControllerTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/HealthControllerTests.java) | 3 | Passed | Foundation health check, database status check, info endpoint. |
| [`SecurityAndErrorHandlingTests`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/SecurityAndErrorHandlingTests.java) | 2 | Passed | Correlation ID injection, CORS allowed and blocked origin handling. |

---

## 8. Frontend Portals Verification

Both React applications were compiled to ensure zero build regressions:

1. **Admin Portal (`admin/`):**
   ```text
   > kuter-admin@0.1.0 build
   > tsc -b && vite build
   ✓ 1972 modules transformed.
   dist/index.html                   0.80 kB │ gzip:  0.45 kB
   dist/assets/index-DrBZjHcr.css    4.04 kB │ gzip:  1.39 kB
   dist/assets/index-jxLR4M0s.js   244.65 kB │ gzip: 75.47 kB
   ✓ built in 2.69s
   ```

2. **Frontend Portal (`frontend/`):**
   ```text
   > kuter-frontend@0.1.0 build
   > tsc -b && vite build
   ✓ 1976 modules transformed.
   dist/index.html                   0.82 kB │ gzip:  0.46 kB
   dist/assets/index-kiJn9ndS.css    5.73 kB │ gzip:  1.90 kB
   dist/assets/index-jLUNytci.js   268.24 kB │ gzip: 80.33 kB
   ✓ built in 2.63s
   ```

---

## 9. Deliverables Inventory

| Path | Description |
| :--- | :--- |
| [`backend/src/main/resources/db/migration/V3__onboarding_enhancements.sql`](file:///c:/D%20One/Projects/Kuter/backend/src/main/resources/db/migration/V3__onboarding_enhancements.sql) | Flyway V3 migration: onboarding request enhancements & plan seeds. |
| [`backend/src/main/java/com/kuter/config/JwtService.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/JwtService.java) | JJWT 0.12.6 HMAC-SHA256 token service. |
| [`backend/src/main/java/com/kuter/config/JwtAuthenticationFilter.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/JwtAuthenticationFilter.java) | Bearer token authentication filter. |
| [`backend/src/main/java/com/kuter/config/TenantContextFilter.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/config/TenantContextFilter.java) | Tenant validation & lifecycle enforcement filter. |
| [`backend/src/main/java/com/kuter/identity/AuthService.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/AuthService.java) | Authentication service interface. |
| [`backend/src/main/java/com/kuter/identity/AuthServiceImpl.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/AuthServiceImpl.java) | Authentication service implementation. |
| [`backend/src/main/java/com/kuter/identity/TenantMembershipService.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/TenantMembershipService.java) | Membership management service interface. |
| [`backend/src/main/java/com/kuter/identity/TenantMembershipServiceImpl.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/TenantMembershipServiceImpl.java) | Membership management service implementation. |
| [`backend/src/main/java/com/kuter/identity/AuthController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/AuthController.java) | REST endpoints for login, logout, and `/auth/me`. |
| [`backend/src/main/java/com/kuter/identity/UserController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/UserController.java) | REST endpoint for `/users/me`. |
| [`backend/src/main/java/com/kuter/identity/TenantMembershipController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/identity/TenantMembershipController.java) | REST endpoints for staff membership CRUD. |
| [`backend/src/main/java/com/kuter/onboarding/OnboardingService.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/onboarding/OnboardingService.java) | Onboarding service interface. |
| [`backend/src/main/java/com/kuter/onboarding/OnboardingServiceImpl.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/onboarding/OnboardingServiceImpl.java) | Onboarding submission, review, approval & rejection. |
| [`backend/src/main/java/com/kuter/onboarding/PublicOnboardingController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/onboarding/PublicOnboardingController.java) | Public registration submission & status lookup. |
| [`backend/src/main/java/com/kuter/onboarding/AdminOnboardingController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/onboarding/AdminOnboardingController.java) | Platform admin review, approval & rejection endpoints. |
| [`backend/src/main/java/com/kuter/tenancy/TenantManagementService.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/TenantManagementService.java) | Tenant management service interface. |
| [`backend/src/main/java/com/kuter/tenancy/TenantManagementServiceImpl.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/TenantManagementServiceImpl.java) | Tenant management implementation. |
| [`backend/src/main/java/com/kuter/tenancy/AdminTenantController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/AdminTenantController.java) | Platform admin tenant provisioning & suspension. |
| [`backend/src/main/java/com/kuter/tenancy/TenantController.java`](file:///c:/D%20One/Projects/Kuter/backend/src/main/java/com/kuter/tenancy/TenantController.java) | Tenant owner settings endpoints. |
| [`backend/src/test/java/com/kuter/identity/AuthenticationApiTests.java`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/identity/AuthenticationApiTests.java) | 7 automated integration tests for authentication APIs. |
| [`backend/src/test/java/com/kuter/onboarding/OnboardingWorkflowTests.java`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/onboarding/OnboardingWorkflowTests.java) | 9 automated integration tests for onboarding workflows. |
| [`backend/src/test/java/com/kuter/tenancy/TenantAndMembershipManagementTests.java`](file:///c:/D%20One/Projects/Kuter/backend/src/test/java/com/kuter/tenancy/TenantAndMembershipManagementTests.java) | 8 automated integration tests for tenant & membership APIs. |
| [`docs/api/phase-three.md`](file:///c:/D%20One/Projects/Kuter/docs/api/phase-three.md) | Comprehensive REST API contract for all Phase Three endpoints. |
| [`docs/security-design.md`](file:///c:/D%20One/Projects/Kuter/docs/security-design.md) | Security architecture, stateless JWT, and role hierarchy. |
| [`docs/tenant-isolation.md`](file:///c:/D%20One/Projects/Kuter/docs/tenant-isolation.md) | Multi-tenant isolation and lifecycle enforcement. |
| [`docs/architecture.md`](file:///c:/D%20One/Projects/Kuter/docs/architecture.md) | Modular monolith structure and package mapping. |
| [`docs/development-progress.md`](file:///c:/D%20One/Projects/Kuter/docs/development-progress.md) | Progress checklist and verification matrices. |
| [`README.md`](file:///c:/D%20One/Projects/Kuter/README.md) | Updated top-level documentation and verification instructions. |

---

## 10. Readiness for Phase Four

Phase Three has established a secure, multi-tenant authentication, onboarding, and tenant-management platform.

The system is fully prepared to proceed with **Phase Four: Building Materials Catalog & Dual-UOM Engine**:
- Product Categories management.
- Base Units of Measure (`TON`, `KG`, `BAG`, `PCS`, `MTR`, `BNDL`, `SHT`).
- Product Catalog CRUD with tenant-scoped SKUs and barcode tracking.
- Product-specific Dual-UOM conversion factor management (`to_base_factor`, `from_base_factor`).
- Reorder threshold levels and cost/selling price tracking.
