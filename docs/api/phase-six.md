# Kuter API Reference — Phase Six: Subscriptions & Platform Administration

## Overview

Phase Six introduces subscription plan management, tenant subscription lifecycle workflows, real-time feature and usage entitlement enforcement, platform metrics, and tenant subscription self-inspection.

- **Base Path:** `/api/v1`
- **Authentication:** Bearer JWT required for all protected endpoints.
- **Precision:** All monetary amounts (`price`, `estimatedMonthlyRevenue`) and usage metrics use high-precision decimals (`NUMERIC(15, 4)`).
- **Security:** Platform-admin endpoints strictly require `ROLE_PLATFORM_ADMIN`. Shop endpoints enforce multi-tenant isolation via `X-Tenant-ID` and token memberships.

---

## 1. Public Plan Directory

### 1.1 List Public Plans
`GET /api/v1/public/subscription-plans`

Publicly accessible endpoint for marketing websites, landing pages, and prospective shop owners. Returns all active and public subscription plans without internal administrative metadata.

- **Authorization:** Public (`permitAll()`)
- **Headers:** None required

#### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Public subscription plans retrieved successfully",
  "data": [
    {
      "id": "11111111-1111-1111-1111-111111111101",
      "code": "starter",
      "name": "Starter",
      "description": "Essential dual-unit inventory control for single yard building materials shops.",
      "price": 49.0000,
      "currency": "USD",
      "billingInterval": "MONTHLY",
      "productLimit": 500,
      "userLimit": 3,
      "branchLimit": 1,
      "trialDays": 14,
      "featuresSummary": "Up to 500 products, 3 staff seats, counter checkout, basic UOM conversion"
    },
    {
      "id": "11111111-1111-1111-1111-111111111102",
      "code": "growth",
      "name": "Growth",
      "description": "High-volume materials distribution, multiple warehouse locations, and advanced conversion rules.",
      "price": 119.0000,
      "currency": "USD",
      "billingInterval": "MONTHLY",
      "productLimit": 2500,
      "userLimit": 10,
      "branchLimit": 3,
      "trialDays": 14,
      "featuresSummary": "Up to 2,500 products, 10 staff seats, delivery note receiving, multi-location stock ledger"
    }
  ],
  "timestamp": "2026-10-29T10:00:00Z",
  "correlationId": "pub-plans-1234"
}
```

---

## 2. Platform Admin: Plan Management

All endpoints under this section require `ROLE_PLATFORM_ADMIN`.

### 2.1 Create Subscription Plan
`POST /api/v1/subscription-plans`

- **Authorization:** `ROLE_PLATFORM_ADMIN`
- **Headers:** `Authorization: Bearer <admin-token>`

#### Request Payload
```json
{
  "code": "pro-yard",
  "name": "Professional Yard",
  "description": "Tailored for mid-market building supplies distributors.",
  "price": 89.0000,
  "currency": "USD",
  "billingInterval": "MONTHLY",
  "productLimit": 1500,
  "userLimit": 7,
  "branchLimit": 2,
  "trialDays": 14,
  "featuresSummary": "inventory,catalog,sales,reports,custom_units",
  "isPublic": true
}
```

#### Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Subscription plan created successfully",
  "data": {
    "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
    "code": "pro-yard",
    "name": "Professional Yard",
    "price": 89.0000,
    "currency": "USD",
    "billingInterval": "MONTHLY",
    "productLimit": 1500,
    "userLimit": 7,
    "branchLimit": 2,
    "trialDays": 14,
    "public": true,
    "active": true
  }
}
```

#### Error Responses
- `400 Bad Request`: Validation failure (blank code/name, negative price).
- `409 Conflict`: Plan code already exists.

---

### 2.2 List All Plans (Admin)
`GET /api/v1/subscription-plans`

Lists all plans with optional filtering.

- **Query Parameters:**
  - `active` (`Boolean`, optional): Filter by active status.
  - `isPublic` (`Boolean`, optional): Filter by public visibility.
  - `page` (`Integer`, default `0`), `size` (`Integer`, default `20`)

---

### 2.3 Update Plan
`PUT /api/v1/subscription-plans/{id}`

Updates plan pricing, limits, descriptions, or feature summaries.

---

### 2.4 Update Plan Active Status
`PATCH /api/v1/subscription-plans/{id}/status`

- **Request Body:**
```json
{
  "active": false
}
```

---

### 2.5 Delete Plan
`DELETE /api/v1/subscription-plans/{id}`

- **Protection Rule:** If the plan is currently assigned to one or more tenant subscriptions, deletion is rejected with `409 Conflict`. Deactivate the plan instead.

---

## 3. Platform Admin: Tenant Subscription Management

### 3.1 Assign Subscription to Tenant
`POST /api/v1/admin/subscriptions/assign`

Assigns a subscription plan to a tenant shop. Marks the subscription as the current active/pending record for the tenant.

- **Request Payload:**
```json
{
  "tenantId": "550e8400-e29b-41d4-a716-446655440000",
  "planId": "11111111-1111-1111-1111-111111111102",
  "status": "ACTIVE",
  "periodDays": 30,
  "notes": "Direct annual onboarding assignment"
}
```

---

### 3.2 Activate Subscription
`POST /api/v1/admin/subscriptions/{id}/activate`

Activates a pending, trialing, or expired subscription.

- **Request Payload:**
```json
{
  "periodDays": 30,
  "notes": "Activated upon manual invoice confirmation"
}
```

---

### 3.3 Start Free Trial
`POST /api/v1/admin/subscriptions/{id}/start-trial`

Initiates a trial period based on plan `trialDays` or custom `trialDaysOverride`.

---

### 3.4 Change Plan (Upgrade / Downgrade)
`POST /api/v1/admin/subscriptions/{id}/change-plan`

Switches the subscription to a new plan tier. Reapplies resource quotas immediately. Preserves existing catalog and staff data if downgrading.

- **Request Payload:**
```json
{
  "newPlanId": "11111111-1111-1111-1111-111111111103",
  "immediate": true,
  "notes": "Upgraded to Enterprise tier"
}
```

---

### 3.5 Extend Subscription
`POST /api/v1/admin/subscriptions/{id}/extend`

Extends the current billing period end timestamp. **Requires a mandatory, non-blank business reason.** If extending an expired subscription past now, automatically restores status to `ACTIVE`.

- **Request Payload:**
```json
{
  "additionalDays": 30,
  "reason": "Payment verified via bank transfer order #TRX-99482"
}
```

---

### 3.6 Suspend Subscription
`POST /api/v1/admin/subscriptions/{id}/suspend`

Freezes shop access. Requires a non-blank reason.

- **Request Payload:**
```json
{
  "reason": "Non-payment after 15-day grace notice"
}
```

---

### 3.7 Reactivate Suspended Subscription
`POST /api/v1/admin/subscriptions/{id}/reactivate`

Restores an administratively suspended subscription.

- **Request Payload:**
```json
{
  "reason": "Account balance cleared in full"
}
```

---

### 3.8 Cancel Subscription
`POST /api/v1/admin/subscriptions/{id}/cancel`

- **Request Payload:**
```json
{
  "immediate": true,
  "reason": "Shop business closed down"
}
```

---

### 3.9 Platform Subscription Summary & MRR
`GET /api/v1/admin/subscriptions/summary`

Returns global operational and financial health metrics across all tenants.

#### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Subscription summary retrieved successfully",
  "data": {
    "totalSubscriptions": 42,
    "activeCount": 35,
    "trialingCount": 4,
    "expiredCount": 2,
    "suspendedCount": 1,
    "cancelledCount": 0,
    "estimatedMonthlyRevenue": 4820.0000
  }
}
```

---

### 3.10 Subscription History Ledger
`GET /api/v1/admin/subscriptions/{id}/history`
`GET /api/v1/admin/subscriptions/tenant/{tenantId}/history`

Returns chronological, immutable audit entries for all lifecycle events on a subscription or tenant.

---

## 4. Tenant-Scoped: Subscription Self-Inspection

Shop staff inspect their own subscription status and plan limits via `/api/v1/me/subscription` or `/api/v1/tenants/me/subscription`.

### 4.1 Get My Shop Subscription & Usage
`GET /api/v1/me/subscription` (or `GET /api/v1/tenants/me/subscription`)

- **Authorization:** Any authenticated member of the tenant (`OWNER`, `MANAGER`, `CASHIER`, `STOREKEEPER`)
- **Headers:**
  - `Authorization: Bearer <jwt-token>`
  - `X-Tenant-ID: <tenant-uuid>`

#### Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Subscription details retrieved successfully",
  "data": {
    "subscriptionId": "4f1fc9be-719b-48d1-9a98-bd6dd3522552",
    "planId": "11111111-1111-1111-1111-111111111101",
    "planName": "Starter",
    "planCode": "starter",
    "planDescription": "Essential dual-unit inventory control for single yard building materials shops.",
    "price": 49.0000,
    "currency": "USD",
    "billingInterval": "MONTHLY",
    "status": "ACTIVE",
    "effectiveStatus": "ACTIVE",
    "currentPeriodStart": "2026-10-15T00:00:00Z",
    "currentPeriodEnd": "2026-11-15T00:00:00Z",
    "trialStart": null,
    "trialEnd": null,
    "daysRemaining": 17,
    "cancelAtPeriodEnd": false,
    "productLimit": 500,
    "activeProductsCount": 142,
    "productLimitReached": false,
    "userLimit": 3,
    "activeUsersCount": 2,
    "userLimitReached": false,
    "branchLimit": 1,
    "features": [
      "Up to 500 products",
      "3 staff seats",
      "counter checkout",
      "basic UOM conversion"
    ]
  }
}
```

---

### 4.2 Get My Shop Subscription History
`GET /api/v1/me/subscription/history` (or `GET /api/v1/tenants/me/subscription/history`)

Returns immutable history events for the caller's tenant only. Strictly isolated across tenants.
