# Kuter API Specification — Phase Three: Authentication, Tenant Onboarding, and Management

This document defines the complete REST API contract for Phase Three of **Kuter**, a multi-tenant Building Materials Inventory Management SaaS.

All endpoints adhere to the following global conventions:
- **Base URI:** `/api/v1`
- **Content Type:** `application/json;charset=UTF-8`
- **Standard Envelope:** Responses use standard envelopes:
  - Success: `{"success": true, "data": ..., "message": "...", "timestamp": "..."}`
  - Error: `{"status": 4xx/5xx, "error": "...", "message": "...", "path": "...", "correlationId": "...", "timestamp": "...", "details": [...]}`
- **Correlation ID:** Clients may send `X-Correlation-ID` header. If absent, backend generates a UUID and echoes it in `X-Correlation-ID` and `X-Request-ID` response headers.
- **Tenant Context:** Multi-tenant endpoints accept the `X-Tenant-ID` header containing the tenant's UUID. This is validated cryptographically against the authenticated user's active memberships.

---

## 1. Authentication & Identity APIs

### 1.1 User Login
Authenticates user credentials and issues a signed stateless JWT.

- **Method & Path:** `POST /api/v1/auth/login`
- **Access Control:** Public (`permitAll`)
- **Headers:** None required.
- **Request Body:**
```json
{
  "email": "owner@alphahardware.example",
  "password": "CorrectPassword123!"
}
```
- **Validation Rules:**
  - `email`: Non-blank, valid email syntax.
  - `password`: Non-blank, minimum 6 characters.
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": "e8f30d08-f80d-4116-b043-224450b6c51d",
      "email": "owner@alphahardware.example",
      "fullName": "Ahmad Al-Mansoor",
      "phoneNumber": "+966501234567",
      "isPlatformAdmin": false,
      "status": "ACTIVE",
      "createdAt": "2026-10-28T10:00:00Z"
    },
    "memberships": [
      {
        "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
        "tenantName": "Alpha Hardware Supplies",
        "tenantCode": "alpha-hardware",
        "role": "OWNER",
        "status": "ACTIVE"
      }
    ],
    "activeTenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06"
  },
  "message": "Authentication successful"
}
```
- **Error Responses:**
  - `401 Unauthorized`: Generic error message `"Invalid username or password"` (prevents account enumeration).
  - `403 Forbidden`: Returned when account is suspended or locked.

---

### 1.2 User Logout
Terminates user session client-side and registers audit log.

- **Method & Path:** `POST /api/v1/auth/logout`
- **Access Control:** Authenticated user (`ROLE_TENANT_MEMBER` or `ROLE_PLATFORM_ADMIN`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Request Body:** Empty
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": null,
  "message": "Logged out successfully"
}
```

---

### 1.3 Current User Profile
Retrieves authenticated user details, authorized memberships, and active tenant context.

- **Method & Path:** `GET /api/v1/users/me` (alias: `GET /api/v1/auth/me`)
- **Access Control:** Authenticated user
- **Headers:** 
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <uuid>` (optional; activates specific tenant context)
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "e8f30d08-f80d-4116-b043-224450b6c51d",
      "email": "owner@alphahardware.example",
      "fullName": "Ahmad Al-Mansoor",
      "phoneNumber": "+966501234567",
      "isPlatformAdmin": false,
      "status": "ACTIVE",
      "createdAt": "2026-10-28T10:00:00Z"
    },
    "activeTenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "memberships": [
      {
        "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
        "tenantName": "Alpha Hardware Supplies",
        "tenantCode": "alpha-hardware",
        "role": "OWNER",
        "status": "ACTIVE"
      }
    ],
    "effectivePermissions": [
      "CATALOG_READ", "CATALOG_WRITE", "INVENTORY_READ", "INVENTORY_RECEIVE", "INVENTORY_ADJUST",
      "SALES_READ", "SALES_CREATE", "SALES_REFUND", "CUSTOMER_READ", "CUSTOMER_WRITE",
      "SUPPLIER_READ", "SUPPLIER_WRITE", "REPORT_READ", "TENANT_SETTINGS_WRITE", "USER_MANAGE"
    ]
  },
  "message": "User profile retrieved"
}
```

---

## 2. Public Shop Registration & Onboarding APIs

### 2.1 Submit Onboarding Request
Public endpoint for prospective shop owners to register their business. Aligned with Figma prototype specifications: no initial password is collected at submission.

- **Method & Path:** `POST /api/v1/onboarding-requests`
- **Access Control:** Public (`permitAll`)
- **Request Body:**
```json
{
  "shopName": "Summit Timber & Hardware Yard",
  "preferredShopCode": "summit-timber",
  "ownerFullName": "Maya Lin",
  "ownerEmail": "maya@summittimber.example",
  "ownerPhone": "+14155552671",
  "businessAddress": "100 Industrial Pkwy, Sector 4",
  "currency": "USD",
  "requestedPlanCode": "growth",
  "applicantNotes": "Operating 2 physical yards; requesting annual growth tier."
}
```
- **Validation Rules:**
  - `shopName`: Required, 2-100 characters.
  - `ownerFullName`: Required, 2-100 characters.
  - `ownerEmail`: Required, valid email format.
  - `ownerPhone`: Optional, max 20 characters.
  - `preferredShopCode`: Optional, 2-50 characters alphanumeric/hyphen.
  - `requestedPlanCode`: Required (`starter`, `growth`, or `enterprise`).
  - `currency`: Optional, default `USD`.
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "referenceCode": "REQ-2026-94812",
    "shopName": "Summit Timber & Hardware Yard",
    "ownerEmail": "maya@summittimber.example",
    "requestedPlanCode": "growth",
    "status": "PENDING",
    "createdAt": "2026-10-28T14:20:00Z"
  },
  "message": "Onboarding request submitted successfully. Our team will review your application."
}
```
- **Error Responses:**
  - `409 Conflict`: Returned if a pending onboarding request already exists for the email.

---

### 2.2 Check Public Onboarding Status
Enables applicants to track their review status via reference code without revealing sensitive tenant data.

- **Method & Path:** `GET /api/v1/onboarding-requests/status/{referenceCode}`
- **Access Control:** Public (`permitAll`)
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "referenceCode": "REQ-2026-94812",
    "shopName": "Summit Timber & Hardware Yard",
    "ownerEmail": "maya@summittimber.example",
    "requestedPlanCode": "growth",
    "status": "PENDING",
    "createdAt": "2026-10-28T14:20:00Z"
  },
  "message": "Onboarding status retrieved"
}
```
- **Error Responses:**
  - `404 Not Found`: Reference code does not exist.

---

## 3. Platform Administration Onboarding Review APIs

### 3.1 List Onboarding Requests
Returns paginated list of registration requests with optional status and text search filters.

- **Method & Path:** `GET /api/v1/admin/onboarding-requests`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Query Parameters:**
  - `status`: Optional filter (`PENDING`, `APPROVED`, `REJECTED`).
  - `search`: Optional case-insensitive search by shop name, owner name, email, or reference code.
  - `page`: Page index (default: `0`).
  - `size`: Page size (default: `20`, max: `100`).
  - `sort`: Sort property and direction (default: `createdAt,desc`).
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "1cc2aa10-3d9a-4efa-b6be-9dca13d41726",
        "referenceCode": "REQ-2026-94812",
        "shopName": "Summit Timber & Hardware Yard",
        "preferredShopCode": "summit-timber",
        "ownerFullName": "Maya Lin",
        "ownerEmail": "maya@summittimber.example",
        "ownerPhone": "+14155552671",
        "businessAddress": "100 Industrial Pkwy, Sector 4",
        "currency": "USD",
        "requestedPlanCode": "growth",
        "applicantNotes": "Operating 2 physical yards; requesting annual growth tier.",
        "status": "PENDING",
        "reviewerNotes": null,
        "reviewedAt": null,
        "createdTenantId": null,
        "createdAt": "2026-10-28T14:20:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "message": "Onboarding requests retrieved"
}
```

---

### 3.2 Get Onboarding Request Detail
- **Method & Path:** `GET /api/v1/admin/onboarding-requests/{id}`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Success Response (`200 OK`):** Detailed response matching single element of list above.

---

### 3.3 Approve Onboarding Request
Transactionally approves application, provisions the `Tenant`, resolves/creates the `User`, creates `OWNER` `TenantMembership`, provisions `TenantSubscription`, and writes immutable audit log.

- **Method & Path:** `POST /api/v1/admin/onboarding-requests/{id}/approve`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Request Body:**
```json
{
  "customShopCode": "summit-timber-yard",
  "reviewerNotes": "Business registration verified via registry call."
}
```
- **Validation Rules:**
  - `customShopCode`: Optional; overrides requested shop code.
  - `reviewerNotes`: Optional reviewer remarks.
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "requestId": "1cc2aa10-3d9a-4efa-b6be-9dca13d41726",
    "referenceCode": "REQ-2026-94812",
    "tenantId": "e9b17943-176a-4cfc-9328-5a6e4b36952a",
    "tenantCode": "summit-timber-yard",
    "tenantName": "Summit Timber & Hardware Yard",
    "ownerEmail": "maya@summittimber.example",
    "status": "APPROVED",
    "subscriptionPlanCode": "growth"
  },
  "message": "Onboarding request approved and tenant provisioned successfully"
}
```
- **Error Responses:**
  - `400 Bad Request`: If request status is not `PENDING`.
  - `404 Not Found`: If request ID does not exist.

---

### 3.4 Reject Onboarding Request
Rejects application with mandatory justification recorded for applicant feedback and audit trail.

- **Method & Path:** `POST /api/v1/admin/onboarding-requests/{id}/reject`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Request Body:**
```json
{
  "reason": "Business registration certificate could not be validated with state registry."
}
```
- **Validation Rules:**
  - `reason`: Required, 5-1000 characters.
- **Success Response (`200 OK`):** Returns updated `OnboardingDetailResponse` with `status: "REJECTED"`.

---

## 4. Platform Administration Direct Tenant Management APIs

### 4.1 Create Tenant Directly
Allows platform administrators to directly provision new tenant shops without prior public onboarding.

- **Method & Path:** `POST /api/v1/admin/tenants`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Request Body:**
```json
{
  "name": "Delta Ready-Mix Concrete",
  "code": "delta-concrete",
  "businessRegistrationNumber": "CR-99210-A",
  "currency": "USD",
  "timezone": "America/New_York",
  "ownerFullName": "Dan Reynolds",
  "ownerEmail": "dan@deltaconcrete.example",
  "ownerPhone": "+12125559812",
  "subscriptionPlanCode": "starter"
}
```
- **Success Response (`201 Created`):** Returns `TenantDetailResponse`.
- **Error Responses:**
  - `409 Conflict`: Tenant code already in use.

---

### 4.2 List All Tenants
- **Method & Path:** `GET /api/v1/admin/tenants`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Query Parameters:** `status`, `search`, `page`, `size`, `sort`.
- **Success Response (`200 OK`):** Paginated `PageResponse<TenantSummaryResponse>`.

---

### 4.3 Get Tenant Detail
- **Method & Path:** `GET /api/v1/admin/tenants/{id}`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Success Response (`200 OK`):** Returns `TenantDetailResponse` with member count and subscription details.

---

### 4.4 Update Tenant Lifecycle Status
Enables platform administrators to activate or suspend tenant shops. Suspending a tenant immediately blocks access for all its users.

- **Method & Path:** `PATCH /api/v1/admin/tenants/{id}/status`
- **Access Control:** Platform Admin only (`hasRole('PLATFORM_ADMIN')`)
- **Headers:** `Authorization: Bearer <jwt>`
- **Request Body:**
```json
{
  "status": "SUSPENDED",
  "reason": "Overdue subscription invoice past 30 days."
}
```
- **Success Response (`200 OK`):** Returns updated `TenantDetailResponse`.

---

## 5. Tenant Shop Management APIs (Tenant Context)

### 5.1 Get Current Tenant Details
- **Method & Path:** `GET /api/v1/tenants/me`
- **Access Control:** Authenticated tenant member with active context
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: <uuid>`
- **Success Response (`200 OK`):** Returns current tenant details and active member count.

---

### 5.2 Update Current Tenant Settings
- **Method & Path:** `PUT /api/v1/tenants/me`
- **Access Control:** Tenant Owner or Manager (`@tenantSecurity.isOwnerOrManager()`)
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: <uuid>`
- **Request Body:**
```json
{
  "name": "Summit Timber & Building Supplies Yard",
  "businessRegistrationNumber": "BN-2026-9901",
  "currency": "USD",
  "timezone": "America/New_York"
}
```
- **Success Response (`200 OK`):** Returns updated `TenantDetailResponse`.

---

## 6. Tenant User Membership APIs

### 6.1 List Tenant Members
- **Method & Path:** `GET /api/v1/tenants/{tenantId}/members`
- **Access Control:** Tenant member belonging to `{tenantId}` (`@tenantSecurity.isMemberOf(#tenantId)`)
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: {tenantId}`
- **Success Response (`200 OK`):** List of member summaries (`TenantMemberDetailResponse`).

---

### 6.2 Add Tenant Member
Invites or provisions a new user with a specific tenant role (`MANAGER`, `CASHIER`, `STOCK_KEEPER`).

- **Method & Path:** `POST /api/v1/tenants/{tenantId}/members`
- **Access Control:** Tenant Owner/Manager or Platform Admin (`@tenantSecurity.canManageMembersOf(#tenantId)`)
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: {tenantId}`
- **Request Body:**
```json
{
  "email": "cashier.jane@summittimber.example",
  "fullName": "Jane Doe",
  "phoneNumber": "+14155558192",
  "role": "CASHIER"
}
```
- **Success Response (`201 Created`):** Returns newly created `TenantMemberDetailResponse`.
- **Error Responses:**
  - `409 Conflict`: User already holds active membership in this tenant shop.

---

### 6.3 Update Member Role or Status
- **Method & Path:** `PUT /api/v1/tenants/{tenantId}/members/{memberId}`
- **Access Control:** Tenant Owner/Manager or Platform Admin (`@tenantSecurity.canManageMembersOf(#tenantId)`)
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: {tenantId}`
- **Request Body:**
```json
{
  "role": "MANAGER",
  "status": "ACTIVE"
}
```
- **Success Response (`200 OK`):** Returns updated `TenantMemberDetailResponse`.

---

### 6.4 Remove / Deactivate Member
- **Method & Path:** `DELETE /api/v1/tenants/{tenantId}/members/{memberId}`
- **Access Control:** Tenant Owner/Manager or Platform Admin (`@tenantSecurity.canManageMembersOf(#tenantId)`)
- **Headers:** `Authorization: Bearer <jwt>`, `X-Tenant-ID: {tenantId}`
- **Success Response (`204 No Content`):** Soft deletes or suspends membership.
- **Safety Enforcement:** The last active `OWNER` of a tenant shop cannot be removed.
