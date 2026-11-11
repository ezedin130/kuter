# Kuter — Phase Five Completion Report

## 1. Executive Summary

Phase Five of the **Kuter Multi-Tenant Building Materials Inventory Management SaaS** project is complete. This phase designed, implemented, thoroughly tested, and documented the counter sales processing engine, inventory balance deductions, moving weighted-average cost snapshotting, transaction voiding, operational printable receipts, and multi-tenant sales reporting.

### Core Capabilities Delivered:
1. **Atomic Counter Sales Processing:** Fast counter checkout (`POST /api/v1/sales`) supporting multi-product carts, customer metadata, and customizable line and transaction-level discounts.
2. **Dual-UOM Sales Deduction:** Selling in configured product-specific secondary units (e.g. bundles of rebar, pallets of cement, boxes of screws) while automatically converting and deducting canonical base units from physical yard stock balances without rounding discrepancies.
3. **Deadlock-Free Canonical Lock Ordering:** Row-level pessimistic write locking (`SELECT ... FOR UPDATE`) with deterministic UUID sorting across cart items, eliminating cyclic deadlocks under heavy concurrent checkout traffic.
4. **Moving Weighted-Average Cost (WAC) Snapshotting:** Point-of-sale capture of current moving average cost into immutable line-item records (`unitCost`, `totalCost`), permanently preserving historical gross profit margins regardless of future inventory price fluctuations.
5. **Controlled Sale Voiding & Valuation Restoration:** Audited sale voiding (`POST /api/v1/sales/{id}/void`) with mandatory business reasoning, stock balance restoration under lock, recalculation of weighted-average inventory valuation, and compensating ledger movement logging (`MovementType.RETURN`, reference `SALE_VOID`).
6. **Operational Printable Receipt:** Printable counter receipt endpoint (`GET /api/v1/sales/{id}/receipt`) with merchant branding, cashier identification, line items, and an explicit compliance disclaimer: *"Operational Sales Record - Not a Proof of Payment"*.
7. **Cashier Financial Privacy & Metric Redaction:** Field-level data redaction stripping proprietary wholesale cost of goods sold, gross profit, profit margin percentages, and line-item unit costs for cashier accounts (`CASHIER` role).
8. **Multi-Tenant Sales Reporting:** Aggregated sales performance summary (`GET /api/v1/sales/reports/summary`) and paginated product sales ranking reports (`GET /api/v1/sales/reports/products`) with date range filtering.
9. **Idempotency Replay Protection:** Enforced uniqueness on `(tenant_id, idempotency_key)` preventing duplicate charges or duplicate stock deductions from POS network retries.

### Verification Results:
- **Backend Test Suite:** **106/106 tests passing** (`mvnw test` elapsed 57.371s, 0 failures, 0 errors, 0 skipped).
  - 26 comprehensive Phase Five sales tests across 4 dedicated test suites.
  - 80 regression tests across Foundation, Security, Onboarding, Tenancy, Catalog, and Inventory modules.
- **React Admin Portal (`admin/`):** Clean production build via Vite 6.4.5 (`tsc -b && vite build` in 5.06s, 0 errors).
- **React Client Portal (`frontend/`):** Clean production build via Vite 6.4.5 (`tsc -b && vite build` in 5.85s, 0 errors).

---

## 2. Completed Scope & Deliverables

### 2.1 Counter Sale Processing API (`POST /api/v1/sales`)
- Validates tenant context, active product status, customer details, and unit definitions.
- Generates human-readable sequential invoice numbers formatted as `INV-YYYYMMDD-XXXX`.
- Converts secondary sales units to base units using product conversion multipliers.
- Verifies stock availability and applies row-level pessimistic write locks in canonical sort order.
- Captures point-of-sale moving weighted-average cost snapshot.
- Decrements on-hand balance and logs immutable `MovementType.SALE` entry in `stock_movements`.
- Supports idempotency replay: identical requests return the original sale record without re-deducting inventory.

### 2.2 Sale Inquiry & Listing APIs (`GET /api/v1/sales`, `GET /api/v1/sales/{id}`)
- `GET /api/v1/sales`: Paginated, multi-filter sale history supporting date range (`startDate`, `endDate`), status (`COMPLETED`, `VOIDED`), and cashier filters.
- `GET /api/v1/sales/{id}`: Detailed transaction breakdown with line items, quantities, units, and discounts. Cashier roles receive redacted financial figures.

### 2.3 Sale Voiding API (`POST /api/v1/sales/{id}/void`)
- Restricted to `OWNER` and `MANAGER` roles (cashiers rejected with HTTP 403).
- Requires non-blank business justification (`voidReason`).
- Restores physical inventory on-hand balances under pessimistic write lock.
- Recalculates moving weighted-average cost to incorporate returned inventory valuation.
- Logs compensating `MovementType.RETURN` entries referencing `SALE_VOID`.
- Prevents double-voiding (rejected with HTTP 400).

### 2.4 Operational Printable Receipt API (`GET /api/v1/sales/{id}/receipt`)
- Generates clean, printer-friendly operational receipt payload with shop identity, cashier name, line items, itemized discounts, tax, and net totals.
- Embeds standardized operational disclaimer: *"Operational Sales Record - Not a Proof of Payment"*.

### 2.5 Sales Summary Report API (`GET /api/v1/sales/reports/summary`)
- Aggregates completed sales across date ranges: total transactions, gross sales, discounts, net sales, tax collected.
- For authorized roles (`OWNER`, `MANAGER`), computes total Cost of Goods Sold (COGS), gross profit, and profit margin percentage.
- For `CASHIER` role, sensitive cost and margin metrics are omitted from JSON output.

### 2.6 Product Performance Ranking Report API (`GET /api/v1/sales/reports/products`)
- Aggregates sales velocity by product: total base units sold, total revenue generated.
- Supports pagination and date range filtering.

---

## 3. Architectural & Domain Highlights

### 3.1 Dual-UOM Sales Conversion Mechanics

Building materials shops regularly retail items in secondary packaging while holding stock in bulk base units:

```text
Sale Item: 2 Bundles of 12mm Rebar (Conversion factor: 20 Bars / Bundle)
Input Quantity: 2.0000 BUNDLE
Unit Price: 350.0000 SAR / BUNDLE
Base Quantity Deducted: 2.0000 x 20.0000 = 40.0000 BARS
Stock Movement Ledger: -40.0000 BARS
```

- Each product defines a single canonical **base unit** (`base_unit_id`).
- All physical balances in `stock_balances` and movements in `stock_movements` are strictly deducted in base units.
- Conversion math:
  $$\text{Base Quantity} = \text{Input Quantity} \times \text{Conversion Factor}$$
- All quantities and currency amounts use `NUMERIC(15, 4)` precision.

### 3.2 Canonical Lock Ordering & Deadlock Prevention

High-volume building supply yards frequently have multiple sales counters checking out overlapping materials simultaneously. When two cashiers ring up carts containing the same products in different orders, cyclic wait conditions cause database deadlocks:

```text
Counter 1: Locks Rebar -> Tries to lock Cement (Blocked by Counter 2)
Counter 2: Locks Cement -> Tries to lock Rebar (Blocked by Counter 1)
==> Cyclic Deadlock (Transaction Rolled Back)
```

**Kuter Solution — Deterministic Canonical Ordering:**
1. Collect all distinct product IDs across cart items.
2. Sort them in deterministic natural UUID order:
   ```java
   List<UUID> sortedProductIds = request.getItems().stream()
       .map(CreateSaleItemRequestDto::getProductId)
       .distinct()
       .sorted()
       .toList();
   ```
3. Acquire pessimistic write locks (`SELECT ... FOR UPDATE`) strictly in this sorted order.
4. Because all concurrent transactions acquire row locks in the exact same sequence, cyclic wait conditions are mathematically impossible.

### 3.3 Moving Weighted-Average Cost Snapshot & Immutability

- At the instant of sale, `StockBalance.averageCost` represents the true moving weighted-average valuation of that inventory.
- Each `SaleItem` captures:
  $$\text{unitCost} = \text{StockBalance.averageCost}$$
  $$\text{totalCost} = \text{baseQuantity} \times \text{unitCost}$$
- Sale header records `totalCostOfGoodsSold` = $\sum \text{totalCost}$.
- These cost values are permanently immutable. Subsequent supplier receipts that shift inventory unit cost never distort historical sale profitability.

### 3.4 Sale Voiding & Valuation Recalculation

When a sale is voided, inventory returns to stock and moving weighted-average unit cost is recalculated:

$$\bar{C}_{\text{new}} = \frac{(Q_{\text{current}} \times \bar{C}_{\text{current}}) + (Q_{\text{returned}} \times C_{\text{item}})}{Q_{\text{current}} + Q_{\text{returned}}}$$

- If current stock on hand is 0, $\bar{C}_{\text{new}} = C_{\text{item}}$.
- Compensating stock movements are written to `stock_movements`:
  - `movement_type` = `RETURN`
  - `quantity_change` = $+Q_{\text{returned}}$
  - `reference_type` = `SALE_VOID`
  - `reference_id` = `sale_id`

### 3.5 Cashier Financial Privacy & Metric Redaction

Wholesale procurement costs and merchant markups represent sensitive business intelligence.
- `SaleResponseDto`, `SaleItemResponseDto`, `SalesSummaryReportDto`, and `ProductSalesItemDto` use Jackson's `@JsonInclude(JsonInclude.Include.NON_NULL)`.
- When the authenticated user holds only the `CASHIER` role:
  - `totalCostOfGoodsSold`, `grossProfit`, and `profitMarginPercentage` are set to `null` on sales and summaries.
  - `unitCost` and `totalCost` are set to `null` on sale line items.
- Redacted fields are completely omitted from the serialized JSON payload, ensuring zero confidential margin leakage to checkout counters.

### 3.6 Operational Printable Receipt Design

Receipts are formatted specifically for counter operations:
- Merchant header: Shop name, code, contact information.
- Sale metadata: Invoice number, cashier name, ISO-8601 timestamp.
- Line items: Product name, SKU, quantity, unit symbol, unit price, line total.
- Financial totals: Subtotal, total discount, tax amount, net total.
- Disclaimer: *"Operational Sales Record - Not a Proof of Payment"*.
- In full compliance with project boundaries, payment processing, payment method selection, cash drawer reconciliation, and tax authority fiscal invoicing are strictly out of scope.

### 3.7 Idempotency Protection

- Clients supply an optional `Idempotency-Key` header or `idempotencyKey` field in `CreateSaleRequestDto`.
- Backed by PostgreSQL unique constraint `uk_sales_tenant_idempotency` on `(tenant_id, idempotency_key)`.
- If a cashier terminal retransmits a request due to a network glitch or double-click, the backend detects the duplicate and returns the original sale response with HTTP 201 without double-deducting inventory balances.

---

## 4. Database Migrations & Indexing

### Flyway Migration: `V5__sales_enhancements.sql`

```sql
-- 1. Sales table enhancements
ALTER TABLE sales
    ADD COLUMN cashier_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN idempotency_key VARCHAR(100),
    ADD COLUMN notes TEXT,
    ADD COLUMN void_reason TEXT,
    ADD COLUMN voided_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN voided_by UUID REFERENCES users(id) ON DELETE SET NULL;

-- 2. Sale items snapshot enhancements
ALTER TABLE sale_items
    ADD COLUMN product_name VARCHAR(255),
    ADD COLUMN product_sku VARCHAR(100),
    ADD COLUMN discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000;

-- 3. Uniqueness and performance indexes
ALTER TABLE sales
    ADD CONSTRAINT uk_sales_tenant_idempotency UNIQUE (tenant_id, idempotency_key);

CREATE INDEX idx_sales_tenant_status ON sales(tenant_id, status);
CREATE INDEX idx_sales_tenant_sold_at ON sales(tenant_id, sold_at DESC);
CREATE INDEX idx_sales_tenant_cashier ON sales(tenant_id, cashier_id);
CREATE INDEX idx_sale_items_tenant_product ON sale_items(tenant_id, product_id);
```

- Verified clean execution on PostgreSQL 18.6 and H2 in-memory test database.
- Idempotency index enforces per-tenant uniqueness while ignoring nulls.
- Composite indexes optimize common point-of-sale queries, cashier audit reports, and date-range aggregations.

---

## 5. Security & Multi-Tenant Isolation

### 5.1 Authorization Matrix

| Endpoint | HTTP Method | Allowed Roles | Cashier Cost Redacted |
| :--- | :---: | :--- | :---: |
| `/api/v1/sales` | `POST` | `OWNER`, `MANAGER`, `CASHIER` | Yes (on response) |
| `/api/v1/sales` | `GET` | `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER` | Yes |
| `/api/v1/sales/{id}` | `GET` | `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER` | Yes |
| `/api/v1/sales/{id}/void` | `POST` | `OWNER`, `MANAGER` | N/A (Forbidden: 403) |
| `/api/v1/sales/{id}/receipt` | `GET` | `OWNER`, `MANAGER`, `STOREKEEPER`, `CASHIER`, `VIEWER` | N/A (No costs shown) |
| `/api/v1/sales/reports/summary` | `GET` | `OWNER`, `MANAGER`, `CASHIER` | Yes (COGS & Profit) |
| `/api/v1/sales/reports/products` | `GET` | `OWNER`, `MANAGER`, `CASHIER` | N/A |

### 5.2 Tenant Isolation Defense

- Every sale, sale item, and compensating stock movement carries non-nullable `tenant_id`.
- Tenant B supplying an ID for a sale belonging to Tenant A receives HTTP 404 Not Found.
- Aggregation queries in reports strictly include `AND s.tenantId = :tenantId`.
- No cross-tenant data leakage is possible.

---

## 6. Comprehensive Automated Test Verification

### 6.1 Phase Five Sales Test Suites (26 Tests)

1. **`SaleCreationAndDeductionTests` (11 Tests):**
   - `testCreateSale_WithBaseUnit_DeductsStockCorrectly`: Verifies single-item base unit deduction and movement ledger recording.
   - `testCreateSale_WithSecondaryUnit_ConvertsAndDeductsBaseQuantity`: Verifies multi-unit conversion (2 bundles = 40 bars deducted).
   - `testCreateSale_MultipleItems_DeductsAllCorrectly`: Verifies multi-item cart deductions across distinct products.
   - `testCreateSale_WithLineAndHeaderDiscounts_CalculatesTotalsCorrectly`: Verifies discount math and net totals.
   - `testCreateSale_WithCustomUnitPrice_UsesCustomPrice`: Verifies price overriding at checkout.
   - `testCreateSale_InsufficientStock_ThrowsExceptionAndRollsBack`: Verifies atomic rollback when stock $< 0$.
   - `testCreateSale_InactiveProduct_ThrowsException`: Verifies rejection of inactive products.
   - `testCreateSale_IdempotencyKey_ReplaysExistingSale`: Verifies that replaying the same idempotency key returns the original sale without double stock deduction.
   - `testCreateSale_SnapshotsMovingAverageCost`: Verifies cost snapshot immutability against future inventory receipts.
   - `testCreateSale_AsCashier_RedactsCostAndProfit`: Verifies field omission for cashier role.
   - `testCreateSale_CashierIdRecorded`: Verifies cashier tracking on the sale entity.

2. **`SaleVoidingAndReversalTests` (7 Tests):**
   - `testVoidSale_AsOwner_RestoresStockAndRecalculatesWAC`: Verifies status transition to `VOIDED`, stock restoration under lock, and WAC recalculation.
   - `testVoidSale_CreatesCompensatingStockMovement`: Verifies `MovementType.RETURN` with reference `SALE_VOID`.
   - `testVoidSale_AsCashier_Forbidden`: Verifies HTTP 403 Forbidden when cashier attempts void.
   - `testVoidSale_AlreadyVoided_ThrowsException`: Verifies HTTP 400 Bad Request when double-voiding.
   - `testVoidSale_BlankReason_ValidationFailure`: Verifies HTTP 400 when reason is missing.
   - `testVoidSale_NotFound_ThrowsException`: Verifies HTTP 404 on invalid sale ID.
   - `testVoidSale_MultiItemSale_RestoresAllBalances`: Verifies stock restoration across all cart items.

3. **`SaleConcurrencyTests` (3 Tests):**
   - `testConcurrentSales_LimitedStock_PreventsNegativeBalance`: 5 concurrent threads competing for limited stock; exactly 3 succeed, 2 fail, leaving zero negative stock.
   - `testConcurrentSales_OpposingOrder_NoDeadlock`: Multi-product checkouts in opposing cart item orders execute deadlock-free due to canonical natural UUID sorting.
   - `testConcurrentSales_SameIdempotencyKey_OnlyOneSucceeds`: Concurrent duplicate idempotency submissions cleanly resolve to one unique sale.

4. **`SalesReportAndIsolationTests` (5 Tests):**
   - `testCrossTenantIsolation_CannotAccessOtherTenantSale`: Verifies Tenant B receives HTTP 404 when querying Tenant A sale.
   - `testGenerateReceipt_ValidSale_ReturnsReceiptWithDisclaimer`: Verifies receipt format and disclaimer presence.
   - `testSalesSummaryReport_AsManager_IncludesCostAndProfit`: Verifies full financial calculations for managers.
   - `testSalesSummaryReport_AsCashier_RedactsCostAndProfit`: Verifies Jackson omission of cost and profit fields.
   - `testProductSalesReport_ReturnsRankedProducts`: Verifies product ranking aggregation and pagination.

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
[INFO] Running com.kuter.tenancy.TenantAndMembershipManagementTests
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.kuter.tenancy.TenantIsolationTests
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 7. Out-of-Scope Confirmation & Boundary Adherence

In strict adherence to the project instructions:
1. **Payments & Gateways:** No payment processing, card integrations, or gateway callbacks were implemented.
2. **General Ledger Accounting:** No accounting journals, debit/credit ledgers, or external ERP synchronization was introduced.
3. **Cash Drawer Reconciliation:** No register balance counting or cash float workflows were built.
4. **Subscription Quotas:** Merchant subscription plan limits were not enforced on sales volume.
5. **React Portals:** No frontend UI screens were implemented in this phase. Existing Vite builds for `admin/` and `frontend/` were verified to ensure zero regression.

---

## 8. Phase Five Sign-Off & Readiness for Phase Six

Phase Five is complete and fully verified. The sales processing engine is performant, resilient to concurrent checkout race conditions and deadlocks, mathematically precise across dual units of measure, protective of wholesale margin privacy, and verified by 106 passing backend tests.

The platform is ready to proceed to **Phase Six: Subscriptions, Billing Quotas, and Merchant Limits**.
