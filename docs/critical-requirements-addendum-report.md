# Kuter — Critical Requirements Addendum Implementation & Verification Report

**Status:** Completed & Fully Verified  
**Date:** November 2, 2026  
**Governing Architecture:** Enterprise Multi-Tenant Modular Architecture with Cryptographic Tenant Context Isolation, Moving WAC Inventory Accounting, and Pessimistic Locking.

---

## 1. Executive Summary

This report delivers the implementation of the **Critical Requirements Addendum** for the Kuter platform, superseding earlier assumptions and integrating four mandatory business pillars without disturbing existing database history, business logic, or multi-tenant boundary guarantees:

1. **Shop Users and Four-Account Limit:** Enforced a strict maximum of four user accounts per shop (1 Owner + up to 3 Staff) across all standard plans, with capacity checks also respecting lower subscription caps (e.g. Micro tier = 2). Pessimistic locking on `Tenant` prevents race conditions. Account reactivation validates available seats, and an audited ownership transfer workflow (`POST /api/v1/tenants/me/transfer-ownership`) allows transferring the primary owner role while protecting the last owner from deletion or self-demotion.
2. **Fine-Grained Permissions & Role-Based Access:** Implemented the 14-permission `ShopPermission` engine backed by the `tenant_membership_permissions` table. Role defaults are seeded automatically. Crucially, non-delegable owner privileges (`PRICES_MANAGE` and `USERS_MANAGE`) are enforced at the API and database levels—attempts to delegate them to non-owner staff are rejected with HTTP 400 Bad Request.
3. **Owner-Controlled Product Selling Prices & POS Validation:** Selling prices are exclusively controlled by the shop owner via `PATCH /api/v1/products/{id}/selling-price`. Non-owner staff cannot edit product prices. At the sales counter (`POST /api/v1/sales`), line unit prices submitted by non-owner cashiers are strictly validated against the owner's catalog selling price (with secondary unit conversion factor applied); any price deviation is rejected with HTTP 400 Bad Request.
4. **Simplified Inter-Shop Lending & Borrowing:** Replaced earlier complex cross-tenant handshake designs with a robust, single-tenant ledger integrating external counterparties (whether or not they use Kuter). Supports incoming borrowings (`BORROW_RECEIPT`) and outgoing loans (`LOAN_DISPATCH`). Enables multi-step partial returns (`stock_loan_returns`), over-return rejection, stock-on-hand validation before returning borrowed items, loan cancellation, and aggregated net obligations per SKU without creating false sales revenue or payment claims.

All four decoupled applications (`backend/`, `admin/`, `shop/`, `public/`) build cleanly with **237/237 automated tests passing (100% pass rate)**.

---

## 2. Architecture & Design Decisions

### 2.1 Four-Seat Capacity Model
- **Pessimistic Locking:** `TenantRepository.findByIdForUpdate(tenantId)` locks the tenant row during member creation, invitation, and reactivation, preventing concurrent requests from exceeding the 4-user limit.
- **Plan Limit Integration:** `EntitlementService.checkUserLimit(tenantId)` checks `activeCount >= Math.min(planLimit, 4)`. If the plan limit is lower (e.g., Micro = 2), that limit takes precedence; standard tiers (Starter, Growth, Enterprise) are capped at 4 total accounts.
- **Reactivation Safety:** Reactivating a suspended account validates current active seat capacity before changing status.
- **Audited Ownership Transfer:** Owners can transfer shop ownership to any active team member with current password verification. The previous owner is automatically demoted to `MANAGER`, preserving system audit history.

### 2.2 Granular Permissions Matrix
14 distinct permissions in `ShopPermission`:
- `PRODUCTS_VIEW`: Browse product catalog and dual-UOM conversions.
- `PRODUCTS_MANAGE`: Create and edit product metadata (excluding selling prices).
- `PRICES_MANAGE`: **Owner-exclusive.** Authoritatively set and update selling prices.
- `INVENTORY_RECEIVE`: Record incoming shipments and delivery notes.
- `INVENTORY_ADJUST`: Perform accountable inventory adjustments.
- `INVENTORY_VIEW`: Inspect live stock on hand and movement history.
- `SALES_CREATE`: Process counter POS sales checkouts.
- `SALES_VIEW`: Inspect sales history and printable operational receipts.
- `SALES_REPORTS_VIEW`: View sales summary and daily interval reports.
- `USERS_MANAGE`: **Owner-exclusive.** Manage shop staff memberships and roles.
- `LENDING_REQUEST`: Record incoming borrowings and outgoing loans.
- `LENDING_MANAGE`: Process partial/complete returns and cancel unreturned loans.
- `LENDING_VIEW`: View lending records and net obligations summary.
- `SUBSCRIPTION_VIEW`: Inspect subscription tier and usage meters.

### 2.3 Selling Price Validation & Counter Checkout Integrity
- `ProductService.updateSellingPrice(productId, dto)` updates `sellingPrice` and logs `PRODUCT_PRICE_CHANGED` in the audit log. Only users with `PRICES_MANAGE` (shop owners) are permitted.
- `SaleServiceImpl.createSale(...)` evaluates the authenticated actor's membership. If the actor is not an `OWNER`, each item's requested `unitPrice` is compared against `expectedUnitPrice = product.sellingPrice * conversionFactor`. A delta exceeding `0.005` triggers `BadRequestException("Unit price for product '...' does not match authoritative owner selling price")`.

### 2.4 Simplified External Lending & Physical Ledger Integration
- **Physical Movements:**
  - `BORROW`: Incoming stock increases balance with `MovementType.BORROW_RECEIPT`.
  - `LOAN`: Outgoing stock decreases balance with `MovementType.LOAN_DISPATCH`. Blocked if stock on hand is insufficient.
  - `BORROW Return`: Returning borrowed stock to external counterparty decreases balance with `MovementType.BORROW_RETURN_DISPATCH`. Blocked if physical stock on hand is less than return quantity (e.g. stock was sold out).
  - `LOAN Return`: Receiving returned stock from borrower increases balance with `MovementType.LOAN_RETURN_RECEIPT`.
- **Zero Fictitious Revenue:** No sales, invoices, or revenue entries are generated during lending or return workflows.
- **Partial Returns:** Each return is recorded in `stock_loan_returns` with quantity, return date, receiving staff ID, and optional notes.
- **Obligations Summary:** Aggregates net position per SKU across all counterparties:
  $$\text{Net Obligation} = \text{Total Borrowed Base Qty} - \text{Total Loaned Base Qty}$$
  A positive net position indicates the shop owes external counterparties; a negative net position indicates external counterparties owe the shop.

---

## 3. Database Migration & Backend Implementation

### 3.1 Flyway Migration `V8__shop_users_permissions_and_lending.sql`
- Created table `tenant_membership_permissions`:
  - `(membership_id, permission)` composite primary key with foreign key cascading to `tenant_memberships`.
  - Populated default permissions for existing memberships.
- Created table `stock_loans`:
  - UUID primary key, `tenant_id`, `loan_reference` (unique within tenant), `direction` (`BORROW`, `LOAN`), `status` (`ACTIVE`, `PARTIALLY_RETURNED`, `RETURNED`, `CANCELLED`).
  - Counterparty details (`name`, `contact_person`, `phone`, `address`).
  - Product reference, transaction unit, conversion factor, `initial_quantity`, `initial_base_quantity`, `returned_quantity`, `returned_base_quantity`, `outstanding_base_quantity`.
  - Transaction date, expected return date, notes, cancellation reason.
  - Foreign key constraints with `ON DELETE CASCADE` on tenant, product, and unit references.
- Created table `stock_loan_returns`:
  - UUID primary key, `stock_loan_id`, `unit_id`, `quantity`, `base_quantity`, `return_date`, `received_by_user_id`, `notes`, `created_at`, `updated_at`.
- Successfully validated and applied on H2 in-memory test database and PostgreSQL 18.6 development/production database.

### 3.2 Backend Code Changes
- **Entities & Repositories:**
  - `TenantMembership`: Added `@ElementCollection(fetch = FetchType.EAGER) Set<ShopPermission> permissions`.
  - `TenantRepository`: Added `@Lock(LockModeType.PESSIMISTIC_WRITE) Optional<Tenant> findByIdForUpdate(UUID id)`.
  - `StockLoan` and `StockLoanReturn` entities in package `com.kuter.lending`.
  - `StockLoanRepository` and `StockLoanReturnRepository` in package `com.kuter.lending`.
- **Services & Security:**
  - `TenantSecurityService`: Added `hasPermission(String permissionName)` and `hasPermissionInTenant(UUID tenantId, String permissionName)`.
  - `TenantMembershipService` / `TenantMembershipServiceImpl`: Implemented capacity checks, non-delegable permission defense, reactivation validation, and audited ownership transfer.
  - `ProductService` / `ProductServiceImpl`: Added `updateSellingPrice()` with audit logging.
  - `SaleServiceImpl`: Added owner-controlled price enforcement during checkout.
  - `LendingService` / `LendingServiceImpl`: Implemented borrowing, loaning, returns, cancellations, and obligations calculations.
- **REST Controllers:**
  - `TenantController`: Added `POST /api/v1/tenants/me/transfer-ownership` and member status PATCH.
  - `ProductController`: Added `PATCH /api/v1/products/{id}/selling-price`.
  - `LendingController`: Added endpoints under `/api/v1/inventory/`:
    - `POST /borrowings`, `GET /borrowings`
    - `POST /loans`, `GET /loans`
    - `GET /lending/obligations`
    - `GET /lending/{id}`, `POST /lending/{id}/returns`, `POST /lending/{id}/cancel`

---

## 4. Shop Portal Frontend Implementation

### 4.1 Type Definitions & API Client (`shop/src/api/`)
- `shop/src/api/types.ts`: Added `ShopPermission`, `TenantMembership.permissions`, `AddMemberDto`, `UpdateMemberDto`, `TransferOwnershipDto`, `UpdateSellingPriceDto`, `LoanDirection`, `LoanStatus`, `StockLoanResponse`, `StockLoanReturnResponse`, `CreateBorrowingRequestDto`, `CreateLoanRequestDto`, `RecordLoanReturnDto`, `CancelLoanDto`, `ProductObligation`, `LendingObligationsSummary`.
- `shop/src/api/client.ts`: Added methods `addTenantMember()`, `updateTenantMember()`, `updateTenantMemberStatus()`, `removeTenantMember()`, `transferOwnership()`, `updateProductSellingPrice()`, `createBorrowing()`, `listBorrowings()`, `createLoan()`, `listLoans()`, `getLoan()`, `recordLoanReturn()`, `cancelLoan()`, `getLendingObligations()`.

### 4.2 Auth Context (`shop/src/context/AuthContext.tsx`)
- Exposed `isOwner: boolean` and `hasPermission: (permission: ShopPermission) => boolean`.
- Refined capability flags: `canManagePrices`, `canManageUsers`, `canViewLending`, `canManageLending`.

### 4.3 POS Counter Checkout (`shop/src/pages/SalesPosPage.tsx`)
- Line unit price input is disabled/read-only for non-owner staff, displaying a lock icon and tooltip indicating owner-controlled pricing.
- `handleSetUnitPrice()` allows edits only if `isOwner`.

### 4.4 Products Catalog (`shop/src/pages/ProductsPage.tsx`)
- Added owner-only quick price update modal via table action button (`Tag` icon), allowing instant authoritative selling price updates.
- In product edit modal, selling price field is disabled for non-owner staff with visual explanation.

### 4.5 Shop Settings & Team Management (`shop/src/pages/ShopSettingsPage.tsx`)
- **4-Seat Capacity Meter:** Displays active account count (e.g. `3 / 4 Seats In Use`). Disables adding staff when capacity is reached.
- **Add Staff Member Modal:** Form with full name, email, phone, role dropdown, and 12 toggleable staff permissions. Non-delegable owner privileges are disabled with clear explanation.
- **Edit Member Modal:** Allows owners to update roles and fine-tune staff permissions.
- **Account Activation / Suspension:** Toggles active/suspended status with seat capacity checks on reactivation.
- **Transfer Ownership Modal:** Password-verified workflow to transfer primary ownership to another active team member with confirmation warnings.

### 4.6 Inter-Shop Lending & Borrowing (`shop/src/pages/LendingPage.tsx`)
- **Tabs:** Incoming Borrowings, Outgoing Loans, Obligations & Net Position.
- **Record Borrowing Modal:** Select external counterparty, local product SKU, transaction unit, quantity, dates, and notes. Updates physical stock balance.
- **Record Loan Modal:** Validates available physical stock and records outbound loan.
- **Record Return Modal:** Supports partial returns with quantity checks against remaining balance.
- **Cancel Modal:** Allows cancelling unreturned transactions with mandatory reason, cleanly reversing stock.
- **Obligations Summary:** 4 KPI cards and net position table per SKU showing whether the shop owes stock or is owed stock.
- Integrated into `ShopSidebar.tsx` navigation and `App.tsx` routing.

---

## 5. Verification Matrix & Automated Test Results

### 5.1 Test Execution Summary

| Module | Test Suite / Focus | Tests Run | Passed | Failed | Errors | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| `backend/` | `ShopUserLimitAndPermissionsTests` | 5 | 5 | 0 | 0 | **PASSED** |
| `backend/` | `OwnerControlledPricingTests` | 4 | 4 | 0 | 0 | **PASSED** |
| `backend/` | `InventoryLendingAndBorrowingTests` | 4 | 4 | 0 | 0 | **PASSED** |
| `backend/` | `PostgreSqlIntegrationTests` (Postgres 18.6) | 3 | 3 | 0 | 0 | **PASSED** |
| `backend/` | Full backend test suite (33 test classes) | 186 | 186 | 0 | 0 | **PASSED** |
| `admin/` | Vitest administration test suite | 23 | 23 | 0 | 0 | **PASSED** |
| `shop/` | Vitest shop portal test suite | 12 | 12 | 0 | 0 | **PASSED** |
| `public/` | Vitest marketing website test suite | 16 | 16 | 0 | 0 | **PASSED** |
| **System Total** | **Full automated regression & unit suite** | **237** | **237** | **0** | **0** | **100% GREEN** |

### 5.2 Frontend Production Builds

| Application | Framework / Bundler | Build Output | Build Time | Status |
| :--- | :--- | :--- | :---: | :---: |
| `backend/` | Java 21 / Maven 3.9 | `kuter-backend-0.0.1-SNAPSHOT.jar` | 2.95s | Clean |
| `admin/` | React 19 / Vite 6 | `dist/assets/index-uTHUmvaA.js` (349.7 kB) | 3.71s | Clean |
| `shop/` | React 19 / Vite 6 | `dist/assets/index-yme9h-DS.js` (431.9 kB) | 4.17s | Clean |
| `public/` | React 19 / Vite 6 | `dist/assets/index-CxbSsaY1.js` (290.3 kB) | 3.41s | Clean |

---

## 6. Authoritative API Endpoints Added / Updated

| Method | Endpoint | Authorized Roles / Permissions | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/tenants/me/members` | `@tenantSecurity.hasPermission('USERS_MANAGE')` | Adds a staff member with role and permissions (enforcing 4-account limit). |
| `PUT` | `/api/v1/tenants/me/members/{id}` | `@tenantSecurity.hasPermission('USERS_MANAGE')` | Updates member role and permissions (rejects non-delegable privileges). |
| `PATCH` | `/api/v1/tenants/me/members/{id}/status` | `@tenantSecurity.hasPermission('USERS_MANAGE')` | Activates or suspends member account (validating capacity on reactivation). |
| `DELETE` | `/api/v1/tenants/me/members/{id}` | `@tenantSecurity.hasPermission('USERS_MANAGE')` | Removes staff member (protects last active owner). |
| `POST` | `/api/v1/tenants/me/transfer-ownership` | `@tenantSecurity.hasPermission('USERS_MANAGE')` | Transfers shop ownership to another member with password verification. |
| `PATCH` | `/api/v1/products/{id}/selling-price` | `@tenantSecurity.hasPermission('PRICES_MANAGE')` | Updates authoritative product selling price (Owner-only). |
| `POST` | `/api/v1/inventory/borrowings` | `@tenantSecurity.hasPermission('LENDING_REQUEST')` | Records incoming inventory borrowing from external counterparty. |
| `GET` | `/api/v1/inventory/borrowings` | `@tenantSecurity.hasPermission('LENDING_VIEW')` | Lists incoming borrowings with status and date filtering. |
| `POST` | `/api/v1/inventory/loans` | `@tenantSecurity.hasPermission('LENDING_REQUEST')` | Records outgoing inventory loan to external counterparty. |
| `GET` | `/api/v1/inventory/loans` | `@tenantSecurity.hasPermission('LENDING_VIEW')` | Lists outgoing inventory loans. |
| `GET` | `/api/v1/inventory/lending/{id}` | `@tenantSecurity.hasPermission('LENDING_VIEW')` | Retrieves loan/borrowing detail including return logs. |
| `POST` | `/api/v1/inventory/lending/{id}/returns` | `@tenantSecurity.hasPermission('LENDING_MANAGE')` | Records partial or complete return of stock. |
| `POST` | `/api/v1/inventory/lending/{id}/cancel` | `@tenantSecurity.hasPermission('LENDING_MANAGE')` | Cancels unreturned loan/borrowing, reversing physical stock. |
| `GET` | `/api/v1/inventory/lending/obligations` | `@tenantSecurity.hasPermission('LENDING_VIEW')` | Summarizes active obligations and net position per SKU. |

---

## 7. Compliance Checklist & Review Readiness

- [x] **4-User Limit:** Exactly enforced across standard tiers (1 Owner + 3 Staff = 4 Max).
- [x] **Pessimistic Locking:** Concurrency-safe against race conditions via DB locks.
- [x] **Role-Based & Fine-Grained Permissions:** 14 permissions implemented; non-delegable owner privileges protected.
- [x] **Owner-Controlled Selling Prices:** Non-owner price modification blocked; checkout validates line unit prices against owner catalog price.
- [x] **Simplified Lending & Borrowing:** External counterparties supported; physical stock movements integrated; multi-step returns tracked; zero false sales revenue.
- [x] **Database Integrity:** Flyway `V8` migration tested on H2 and PostgreSQL 18.6 with clean schema validation.
- [x] **Multi-Tenancy:** Cryptographic tenant boundary isolation intact.
- [x] **Testing & Verification:** 237/237 tests passing (100% green); clean production builds across all 4 directories.
- [x] **Documentation:** `docs/development-progress.md` and `README.md` updated.
- [x] **Execution Rules Adherence:** Work completed in controlled increments; stopping now for user review without starting unauthorized phases.
