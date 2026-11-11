# Kuter — Phase Four Completion Report

## 1. Executive Summary

Phase Four of the **Kuter Multi-Tenant Building Materials Inventory Management SaaS** project is complete. This phase designed, implemented, verified, and thoroughly documented the product catalog and core inventory management engine.

Key deliverables implemented and verified:
1. **Product Category Management:** Hierarchical product organization with case-insensitive tenant uniqueness, category update, and soft-deactivation protection when linked products exist.
2. **Unit Definitions Management:** System-wide unit configuration with automatic zero-configuration seeding of standard building trade units (`PCS`, `BAG`, `TON`, `KG`, `MTR`, `BOX`, `BNDL`, `PALLET`, `SHT`) alongside tenant custom unit definitions.
3. **Product Catalog Management:** Complete product lifecycle management with shop-scoped SKU uniqueness, dual pricing (`basePrice`, `initialCostPrice`), inventory alert thresholds (`minStockAlert`, `maxStockAlert`), yard storage location, image URL, and object storage key tracking (`imageStorageKey`).
4. **Dual Units of Measure (UOM) Engine:** Dynamic product-specific unit conversions (e.g. 1 PALLET = 50 BAG, or 1 BOX = 100 PCS) with canonical base unit tracking and factor multiplication.
5. **Inbound Stock Receiving (Goods Received):** Supplier delivery recording with automatic secondary-to-base unit normalization, real-time moving weighted-average costing (WAC), immutable ledger movement creation, on-hand balance updates, and idempotency key protection.
6. **Stock Adjustments & Reconciliation:** Stock delta adjustments for shrinkage, breakage, and waste alongside physical inventory audit cycle counts (`targetQuantity`), featuring negative stock prevention and unit average cost preservation.
7. **Pessimistic Concurrency Control:** Row-level pessimistic write locking (`SELECT ... FOR UPDATE`) on stock balances to eliminate race conditions and prevent negative inventory in concurrent warehouse yard operations.
8. **Live Balances & Audit Ledger:** Real-time stock balance tracking per product with low-stock status flags and an append-only, tamper-evident stock movements ledger.
9. **Inventory Valuation & Replenishment Reporting:** Aggregated valuation reporting with category-by-category breakdowns and targeted low-stock replenishment alert feeds.
10. **Role-Based Authorization & Multi-Tenant Defense:** Strict `@tenantSecurity` method security preventing cashiers from altering catalog records or executing receiving/adjustments, backed by absolute cross-tenant data isolation.

**Verification Results:**
- **Backend Test Suite:** **80/80 tests passing** (`mvnw test` elapsed 55.188s, 0 failures, 0 errors, 0 skipped).
- **React Admin Portal (`admin/`):** Clean build via Vite 6.4.5 (`tsc -b && vite build` — 0 errors).
- **React Client Portal (`frontend/`):** Clean build via Vite 6.4.5 (`tsc -b && vite build` — 0 errors).

---

## 2. Completed Scope & Deliverables

### 2.1 Category Management APIs (`/api/v1/categories`)
- `POST /api/v1/categories`: Creates category with trimmed name and description. Enforces case-insensitive uniqueness within tenant.
- `GET /api/v1/categories`: Lists tenant categories with optional `includeInactive=true` query parameter.
- `GET /api/v1/categories/{id}`: Retrieves single category details scoped to tenant.
- `PUT /api/v1/categories/{id}`: Updates category name, description, and active status.
- `DELETE /api/v1/categories/{id}`: If products reference the category, soft-deactivates it (`active: false`) to preserve catalog relationships; if unreferenced, hard-deletes it.

### 2.2 Unit Definitions APIs (`/api/v1/units`)
- `GET /api/v1/units`: Lists available measurement units. Automatically seeds standard building materials units if tenant has no units configured.
- `POST /api/v1/units`: Creates tenant custom unit (e.g. `DRUM`, `ROLL`, `CAN`). Automatically uppercases code and prevents duplicate codes.
- `GET /api/v1/units/{id}`: Retrieves unit definition by ID.

### 2.3 Product Catalog APIs (`/api/v1/products`)
- `POST /api/v1/products`: Provisions product SKU, base unit, default pricing, alerts, and optional image storage key. If initial quantity $> 0$, automatically generates initial stock receipt and balance record.
- `GET /api/v1/products`: Paged, searchable, and sortable catalog listing with filters for category, status (`ACTIVE`, `INACTIVE`, `DISCONTINUED`), and keyword search across SKU and name.
- `GET /api/v1/products/{id}`: Detailed product view including current stock on hand and average cost.
- `PUT /api/v1/products/{id}`: Updates product metadata, base price, alert thresholds, location, and image assets.
- `PATCH /api/v1/products/{id}/status`: Transitions product lifecycle state.
- `DELETE /api/v1/products/{id}`: If historical inventory movements exist, safely sets status to `INACTIVE` to protect accounting integrity; if clean, hard-deletes.

### 2.4 Product-Specific Unit Conversions (`/api/v1/products/{id}/units`)
- `POST /api/v1/products/{id}/units`: Configures secondary unit conversion factor (e.g. 1 BUNDLE = 20 BARS). Rejects assigning base unit or duplicate conversions.
- `GET /api/v1/products/{id}/units`: Returns product units with the base unit always positioned first (`baseUnit: true`, factor `1.0000`), followed by all configured secondary units.
- `DELETE /api/v1/products/{id}/units/{unitId}`: Removes a secondary unit conversion. Rejects deleting the base unit.

### 2.5 Inbound Stock Receiving APIs (`/api/v1/inventory/receipts`)
- `POST /api/v1/inventory/receipts`: Records supplier delivery note with line items. Converts secondary units to base quantities, recalculates moving weighted-average cost, logs immutable ledger movements, updates stock balances under pessimistic lock, and guarantees idempotency.
- `GET /api/v1/inventory/receipts`: Paged historical receiving transactions.
- `GET /api/v1/inventory/receipts/{id}`: Detailed receipt view with itemized quantities, conversion factors, and unit costs.

### 2.6 Stock Adjustment APIs (`/api/v1/inventory/adjustments`)
- `POST /api/v1/inventory/adjustments`: Processes physical stock reconciliations. Supports explicit deltas (`quantityAdjusted`) or physical audit targets (`targetQuantity`). Prevents negative inventory and preserves moving weighted-average unit cost on downward adjustments. Supports idempotency key replays.
- `GET /api/v1/inventory/adjustments`: Paged historical adjustment records.
- `GET /api/v1/inventory/adjustments/{id}`: Detailed adjustment view with itemized reasons, previous quantities, and adjusted quantities.

### 2.7 Stock Balances & Movements APIs (`/api/v1/inventory`)
- `GET /api/v1/inventory/balances`: Paged live inventory balances with optional `lowStockOnly=true` filter.
- `GET /api/v1/inventory/balances/{productId}`: Live balance, average cost, and valuation for an individual product.
- `GET /api/v1/inventory/movements`: Immutable stock movement audit ledger filtered by product, movement type (`RECEIPT`, `ADJUSTMENT`, `SALE`, `TRANSFER`, `RETURN`), and date range.

### 2.8 Inventory Reporting APIs (`/api/v1/inventory/reports`)
- `GET /api/v1/inventory/reports/valuation`: Live aggregate portfolio valuation, total SKUs, total on-hand physical units, and category-level breakdown of SKU count, quantity, and valuation.
- `GET /api/v1/inventory/reports/low-stock`: Replenishment queue returning all materials currently at or below their configured reorder threshold.

---

## 3. Architectural & Domain Highlights

### 3.1 Dual Units of Measure (UOM) Architecture

Building materials shops routinely purchase bulk materials in wholesale units and retail them in smaller divisible units:

```text
Supplier Receipt: 5 Pallets of Cement (1 Pallet = 50 Bags)
Inbound Conversion: 5 x 50.0000 = 250.0000 Bags
Base Ledger Entry: +250.0000 Bags at 18.0000 SAR/Bag
Counter Retail: Sold in single bags directly from base balance
```

- Each product defines a single canonical **base unit** (`base_unit_id`). All physical balances in `stock_balances` and ledger entries in `stock_movements` are strictly tracked in this base unit.
- Line items on receipts and adjustments accept any valid secondary unit for that product.
- Conversion formula:
  $$\text{Base Quantity} = \text{Input Quantity} \times \text{Conversion Factor}$$
  $$\text{Base Unit Cost} = \frac{\text{Input Unit Cost}}{\text{Conversion Factor}}$$

### 3.2 Moving Weighted-Average Costing (WAC) Engine

Kuter implements real-time moving weighted-average costing on every inbound receipt:

$$\bar{C}_{\text{new}} = \frac{(Q_{\text{current}} \times \bar{C}_{\text{current}}) + (Q_{\text{inbound}} \times C_{\text{inbound}})}{Q_{\text{current}} + Q_{\text{inbound}}}$$

- **Edge Case ($Q_{\text{current}} = 0$):** If on-hand stock is zero, the new average cost equals the inbound base unit cost directly ($\bar{C}_{\text{new}} = C_{\text{inbound}}$).
- **Subsequent Receipts:** When existing inventory is on hand, the existing total valuation is combined with the inbound delivery cost and divided by the new total quantity.
- **Downward Adjustments:** Reductions due to damage, shrinkage, or audit corrections preserve unit cost ($\bar{C}_{\text{new}} = \bar{C}_{\text{current}}$) and reduce total valuation proportionally.
- **Scale:** All computations utilize `BigDecimal` with 4 decimal places (`RoundingMode.HALF_UP`), preventing penny rounding drift.

### 3.3 High-Concurrency Yard Operations: Pessimistic Write Locking

In high-volume supply yards, multiple storekeepers and cashiers simultaneously access inventory. To eliminate lost updates, race conditions, and negative inventory balances, Kuter executes stock balance updates under JPA pessimistic write locking:

```sql
SELECT * FROM stock_balances 
WHERE tenant_id = :tenantId AND product_id = :productId 
FOR UPDATE;
```

This locks the specific product's balance row in PostgreSQL until the transaction commits, ensuring sequential, deterministic valuation and balance calculations.

### 3.4 Network Volatility Protection: Idempotency Key Tracking

Mobile devices and yard tablets operating in outdoor warehouses may suffer dropped connections and retry HTTP requests. 
- Inbound receipts (`stock_receivings`) and adjustments (`stock_adjustments`) accept an optional client-generated `idempotencyKey`.
- Unique database indexes (`uk_receiving_tenant_idempotency` and `uk_adj_tenant_idempotency`) enforce tenant-scoped uniqueness.
- When an identical idempotency key is received, the service retrieves and returns the previously processed response without re-crediting balances or creating duplicate movements.

### 3.5 Negative Stock Prevention & Audit Preservation

- **Negative Stock Rejection:** If any adjustment would cause $Q_{\text{current}} + \Delta Q < 0$, the transaction aborts with HTTP `400 Bad Request`, preventing phantom yard negative counts.
- **Soft-Deactivation vs Hard Delete:**
  - Deleting a category with active products marks it `active: false` instead of failing or cascading deletions.
  - Deleting a product with historical ledger movements transitions it to `status: INACTIVE` instead of deleting rows, preserving financial audit records.

---

## 4. Database Migration & Schema Enhancements (`V4`)

Flyway migration `V4__inventory_enhancements.sql` was authored and validated:

```sql
-- 1. Idempotency keys for receipts and adjustments
ALTER TABLE stock_receivings ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_receiving_tenant_idempotency 
    ON stock_receivings (tenant_id, idempotency_key);

ALTER TABLE stock_adjustments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_adj_tenant_idempotency 
    ON stock_adjustments (tenant_id, idempotency_key);

-- 2. Object storage key tracking for product images
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_storage_key VARCHAR(255);

-- 3. Composite performance indexes
CREATE INDEX IF NOT EXISTS idx_products_tenant_sku_lookup ON products (tenant_id, sku);
CREATE INDEX IF NOT EXISTS idx_products_tenant_name ON products (tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_date ON stock_movements (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_receivings_tenant_date ON stock_receivings (tenant_id, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant_date ON stock_adjustments (tenant_id, created_at DESC);
```

---

## 5. Security, Authorization & Tenant Isolation

Fine-grained role access control is implemented via Spring Method Security using `@tenantSecurity`:

| API / Operation | Endpoint | Access Rule | Allowed Roles | Prohibited Roles |
| :--- | :--- | :--- | :--- | :--- |
| **Catalog Mutation** | `POST/PUT/DELETE /api/v1/categories`, `units`, `products` | `@tenantSecurity.canManageCatalog()` | `OWNER`, `SHOP_MANAGER`, `STOREKEEPER` | `CASHIER` (`403 Forbidden`) |
| **Unit Conversions** | `POST/DELETE /api/v1/products/{id}/units` | `@tenantSecurity.canManageCatalog()` | `OWNER`, `SHOP_MANAGER`, `STOREKEEPER` | `CASHIER` (`403 Forbidden`) |
| **Inbound Stock Receiving** | `POST /api/v1/inventory/receipts` | `@tenantSecurity.canReceiveStock()` | `OWNER`, `SHOP_MANAGER`, `STOREKEEPER` | `CASHIER` (`403 Forbidden`) |
| **Stock Adjustments** | `POST /api/v1/inventory/adjustments` | `@tenantSecurity.canAdjustStock()` | `OWNER`, `SHOP_MANAGER`, `STOREKEEPER` | `CASHIER` (`403 Forbidden`) |
| **Inventory Reads & Reports** | `GET /api/v1/inventory/**`, `GET /api/v1/products/**` | `@tenantSecurity.canReadInventory()` | All tenant members (including `CASHIER`) | Non-members (`403 Forbidden`) |

### Cross-Tenant Security Verification
- Tenant Alpha cannot view, adjust, receive, or report on Tenant Beta's products or stock.
- SKUs, category names, and unit codes are strictly isolated by `tenant_id`.
- Replaying or submitting cross-tenant product IDs results in HTTP `404 Not Found` or `400 Bad Request`.

---

## 6. Automated Verification Results

### 6.1 Backend Test Suite (80/80 Tests Passing)

Running `mvnw.cmd test` executes the complete integration test suite:

```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  55.188 s
```

#### Test Suite Breakdown:

| Test Class | Tests Run | Result | Key Verified Behaviors |
| :--- | :---: | :---: | :--- |
| **`CategoryAndProductApiTests`** | 15 | Passed | Category CRUD, case-insensitive uniqueness, soft-deactivation when products linked; Unit definitions auto-seeding standard units, custom unit creation; Product CRUD with initial stock, image URL/key validation, SKU isolation across tenants, soft-deactivation with historical movements; Product unit conversions CRUD, base unit protection; Cashier role rejection. |
| **`StockReceivingAndCostingTests`** | 6 | Passed | Initial stock receipt ($Q=0$); Moving weighted-average cost calculation on subsequent receipts; Secondary unit receiving with factor calculation (2 boxes of 50 pcs); Idempotency key replay protection (prevents balance inflation); Cross-tenant product receiving rejection; Cashier role rejection. |
| **`StockAdjustmentAndLedgerTests`** | 6 | Passed | Upward stock adjustment; Downward stock adjustment with strict moving average cost preservation; Negative stock prevention (rejects on-hand $< 0$); Target quantity physical count reconciliation; Idempotency key replay protection; Movement ledger filtering; Cashier role rejection. |
| **`InventoryReportingAndIsolationTests`** | 5 | Passed | Low-stock filter on inventory balances; Dedicated low-stock report; Valuation report with total SKUs, valuation, and category breakdown; Cross-tenant balance and valuation isolation; Cross-tenant receipt detail isolation. |
| **Prior Test Suites (Phases 1–3)** | 48 | Passed | Baseline foundation (7), Domain & security (17), Authentication (7), Onboarding (8), Tenancy & memberships (8), PostgreSQL native integration (1). |
| **Total** | **80** | **100% Passed** | **0 failures, 0 errors, 0 skipped** |

### 6.2 Frontend & Admin Portal Build Verification

Both React 19 + TypeScript + Vite portal applications compile cleanly with zero errors:

```bash
# Admin Portal Build
cd admin && npm run build
> vite build
✓ 34 modules transformed.
dist/index.html                   0.82 kB │ gzip:  0.44 kB
dist/assets/index-D_nLw696.css    3.15 kB │ gzip:  1.12 kB
dist/assets/index-BHqW6Yk3.js   145.42 kB │ gzip: 46.85 kB
✓ built in 2.65s

# Frontend / Shop Portal Build
cd frontend && npm run build
> vite build
✓ 38 modules transformed.
dist/index.html                   0.85 kB │ gzip:  0.46 kB
dist/assets/index-CO2_tY49.css    4.20 kB │ gzip:  1.34 kB
dist/assets/index-C2tD5n4A.js   148.90 kB │ gzip: 47.95 kB
✓ built in 2.61s
```

---

## 7. Scope Boundaries & Next Phase Handover

### 7.1 Strict Scope Adherence
As instructed in the Phase Four specification, the following capabilities were intentionally excluded from this phase and reserved for upcoming phases:
- **No Point-of-Sale (POS) counter sales or customer checkouts** (Phase Five).
- **No sales order management, invoicing, or payment processing** (Phase Five).
- **No customer credit accounts or customer balance tracking** (Phase Five).
- **No supplier purchase order approval workflows** (Phase Six).
- **No subscription quota metering or plan restrictions** (Phase Six).
- **No React UI portal screens or views** (Phase Seven).

### 7.2 Phase Five Readiness
The backend product catalog and inventory engine are fully operational and ready for **Phase Five: Counter Sales, POS Checkout & Invoicing APIs**:
- Products and dual-UOM conversions are established and queryable.
- Real-time stock balances and weighted-average costs are tracked with pessimistic write locking.
- Stock movements ledger is prepared to receive `MovementType.SALE` deductions upon customer invoice generation.
- All technical documentation, API specifications, and architectural guides are updated in the repository.
