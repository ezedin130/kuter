# Kuter — Inventory Architecture & Costing Design Specification

## 1. Executive Summary & Domain Scope

In the building materials supply industry (lumber yards, masonry depots, hardware distributors, tile merchants), inventory is characterized by heavy bulky goods, complex packaging hierarchies, high transaction volumes, and frequent price fluctuations. **Kuter** implements an enterprise-grade, multi-tenant inventory management and product catalog engine engineered for maximum financial accuracy, strict auditability, and zero data leakage across tenant shops.

This document formalizes the domain model, unit-of-measure conversions, moving weighted-average costing (WAC) algorithm, pessimistic concurrency controls, and immutable ledger architecture established in **Phase Four**.

---

## 2. Dual Unit of Measure (UOM) Architecture

### 2.1 The Challenge in Building Materials
Building materials are frequently purchased in bulk packaging (e.g. pallets of blocks, crates of tiles, bundles of rebar, metric tonnes of sand) but sold to contractors and retail customers in smaller units (e.g. individual blocks, square meters of tiles, single pieces of rebar, 50kg bags). 

### 2.2 Canonical Base Unit vs Secondary Transaction Units
To eliminate rounding discrepancies and prevent inventory drift:
1. **Canonical Base Inventory Unit:**
   - Every product has exactly one base unit (`baseUnitId`), such as `PCS` (Pieces), `KG` (Kilograms), or `MTR` (Meters).
   - All physical inventory balances (`stock_balances.quantity_on_hand`) and movement ledger quantities (`stock_movements.quantity_change`, `balance_after`) are stored strictly in this base unit.
   - The base unit conversion factor is immutably defined as `1.0000`.

2. **Secondary Transaction / Conversion Units:**
   - Configured via `product_unit_conversions`.
   - Each conversion defines a positive multiplier relative to the base unit:
     $$\text{Base Quantity} = \text{Transaction Quantity} \times \text{Conversion Factor}$$
   - *Example:* For product "Wire Nails 2 Inch" (Base Unit: `PCS`):
     - Secondary Unit `BOX`: conversion factor = `50.0000` (1 Box = 50 Pieces).
     - Secondary Unit `CARTON`: conversion factor = `500.0000` (1 Carton = 10 Boxes = 500 Pieces).
   - If a shipment arrives containing 4 cartons at \$120 per carton:
     - Received Quantity = `4 CARTONS`
     - Base Quantity Credited = $4 \times 500.0000 = 2,000 \text{ PCS}$
     - Effective Base Unit Cost = $\frac{\$480.00}{2000} = \$0.2400 / \text{PC}$

### 2.3 Conversion Validation Rules
- **Base Unit Self-Referencing Prohibition:** Attempting to define a conversion rule where `unitId == product.baseUnitId` is rejected with `400 Bad Request`.
- **Positive Non-Zero Multipliers:** Conversion factors must satisfy `conversionFactor > 0.0000`.
- **Tenant Scope:** Conversion units must belong to the same tenant as the product.
- **Unit Deletion Safety:** A unit definition linked to active products or conversions cannot be hard-deleted.

---

## 3. Weighted-Average Costing (WAC) Algorithm

### 3.1 Mathematical Formulation
Kuter uses a **continuous moving weighted-average cost (WAC)** model. Rather than periodic batch recalculation, every inbound stock receipt atomically updates the product's moving average cost.

When an inbound receipt arrives with base quantity $Q_{\text{receipt}}$ and total purchase cost $C_{\text{receipt}}$:

$$\bar{C}_{\text{new}} = \begin{cases} 
\frac{C_{\text{receipt}}}{Q_{\text{receipt}}}, & \text{if } Q_{\text{existing}} \le 0 \\ 
\frac{(Q_{\text{existing}} \times \bar{C}_{\text{existing}}) + C_{\text{receipt}}}{Q_{\text{existing}} + Q_{\text{receipt}}}, & \text{if } Q_{\text{existing}} > 0 
\end{cases}$$

Where:
- $Q_{\text{existing}}$: Current on-hand quantity in base units before the receipt.
- $\bar{C}_{\text{existing}}$: Current weighted average unit cost.
- $C_{\text{receipt}}$: Total cost of the received line item ($Q_{\text{received}} \times \text{unitCost}$).
- $Q_{\text{receipt}}$: Base quantity received ($Q_{\text{received}} \times \text{factor}$).

### 3.2 Numeric Precision & Rounding Rules
All monetary calculations and stock balances use `java.math.BigDecimal` with scale 4 and `RoundingMode.HALF_UP`:
- Database storage: PostgreSQL `NUMERIC(15, 4)`.
- No floating-point arithmetic (`double` / `float`) is permitted anywhere in the domain or persistence layers.

### 3.3 Downward Adjustment Cost Preservation Rule
A critical principle in GAAP/IFRS inventory accounting:
> **Downward adjustments (damage, breakage, spillage, loss, physical count shortages) DO NOT change the average unit cost of the remaining inventory.**

When stock is reduced:
- Quantity on hand decreases: $Q_{\text{new}} = Q_{\text{existing}} - |\Delta Q|$
- Moving average unit cost is **strictly preserved**: $\bar{C}_{\text{new}} = \bar{C}_{\text{existing}}$
- The shop's total inventory valuation drops proportionally:
  $$\Delta \text{Valuation} = |\Delta Q| \times \bar{C}_{\text{existing}}$$

---

## 4. Inbound Stock Receiving Engine

### 4.1 Workflow & State Transitions
1. Client submits receipt payload containing supplier information, delivery note reference, optional idempotency key, and one or more items (`POST /api/v1/inventory/receipts`).
2. Server validates tenant context and user role (`@tenantSecurity.canReceiveStock()`).
3. If an `idempotencyKey` is provided and already exists in the tenant's receiving history, the server re-returns the existing completed receipt without modifying balances or writing ledger duplicates.
4. For each item:
   - Product is resolved and verified against current tenant.
   - Conversion factor is applied to determine `baseQuantity`.
   - Pessimistic write lock (`PESSIMISTIC_WRITE`) is acquired on the `StockBalance` row.
   - Moving weighted-average cost is recomputed.
   - Balance row is updated: `quantity_on_hand += baseQuantity`, `average_cost = newAvgCost`.
   - Immutable `StockMovement` ledger entry is appended (`type = RECEIPT`).
5. Master `StockReceiving` and detail `StockReceivingItem` rows are persisted within the single atomic transaction.

---

## 5. Stock Adjustments & Physical Audit Reconciliation

### 5.1 Dual Adjustment Modes
Physical stock counts in building yards require flexible recording:
1. **Explicit Delta Mode (`quantityChange`):**
   - Direct positive delta (e.g. `+15.0000` bags found in warehouse corner).
   - Direct negative delta (e.g. `-10.0000` sheets ruined by rain).
2. **Target Audit Count Mode (`targetQuantity`):**
   - Stock auditor counts 68 items physically on the shelf.
   - System calculates delta automatically:
     $$\Delta Q = Q_{\text{target}} - Q_{\text{onHand}}$$

### 5.2 Negative Stock Prevention
In building materials retailing, negative stock on hand leads to severe financial distortion (negative inventory valuations, corrupted COGS calculations, inverted profit margins).
- Any downward adjustment that would result in $Q_{\text{onHand}} + \Delta Q < 0$ is rejected immediately with `400 Bad Request`.
- Clear descriptive error messages are returned indicating current on-hand vs requested deduction.

### 5.3 Reason Codes
Standard industry adjustment reason codes are enforced:
- `STOCK_COUNT`: Routine cycle counts and end-of-month reconciliations.
- `DAMAGED`: Handling damage, forklift accidents, moisture damage, broken bags.
- `LOST`: Shrinkage, unrecorded removal, or yard discrepancy.
- `SPILLAGE`: Bag tears, aggregate spillage, loose materials loss.
- `FOUND`: Discovered untracked material, return of mislaid items.
- `CORRECTION`: Clerical or data entry corrections.

---

## 6. Concurrency, Locking, and Data Integrity

### 6.1 Pessimistic Write Locking
In high-concurrency yard environments (multiple staff receiving shipments or adjusting stock simultaneously), race conditions on stock balance rows can lead to lost updates and incorrect average costs.

Kuter enforces pessimistic write locking via JPA:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT b FROM StockBalance b WHERE b.tenantId = :tenantId AND b.productId = :productId")
Optional<StockBalance> findByTenantIdAndProductIdForUpdate(
    @Param("tenantId") UUID tenantId, 
    @Param("productId") UUID productId
);
```
In PostgreSQL, this emits:
```sql
SELECT * FROM stock_balances 
WHERE tenant_id = ? AND product_id = ? 
FOR UPDATE;
```
Any competing transaction attempting to modify the same product's balance blocks until the first transaction completes, guaranteeing atomic serialization.

---

## 7. Immutable Stock Movement Ledger

The `stock_movements` table is an **append-only, tamper-evident audit ledger**.
- **No updates or deletions** are ever permitted on `stock_movements`.
- Every movement captures:
  - `tenantId`: Tenant yard ownership.
  - `productId`: Product referenced.
  - `movementType`: `RECEIPT`, `ADJUSTMENT`, `SALE`, `TRANSFER`, `RETURN`.
  - `referenceId` & `referenceType`: Foreign key to origin transaction (`STOCK_RECEIVING`, `STOCK_ADJUSTMENT`, `SALE`).
  - `quantityChange`: Signed delta ($+Q$ or $-Q$).
  - `balanceAfter`: Snapshot of total on-hand quantity after this movement took effect.
  - `unitCost`: Weighted average or purchase unit cost at the moment of movement.
  - `actorId`: User ID of the staff member responsible.
  - `notes`: Human-readable explanation.

---

## 8. Catalog Lifecycle & Safe Product Deactivation

To preserve data integrity and prevent foreign key violations:
1. **Unused Products:** Products created in error with zero stock movements may be hard-deleted (`DELETE /api/v1/products/{id}`).
2. **Products with History:** Any product that has historical entries in `stock_movements` cannot be deleted. Attempting deletion automatically transitions the product's status to `INACTIVE`.
   - Inactive products are hidden from POS search and default catalog views.
   - Past ledger history, historical receipts, and financial reports remain intact.

---

## 9. Security & Role Permissions Matrix

| Operation | URI Path | Required Roles | PreAuthorize SpEL |
|:---|:---|:---|:---|
| **Manage Categories** | `/api/v1/categories/**` | Platform Admin, Owner, Manager | `@tenantSecurity.canManageCatalog()` |
| **Manage Units** | `/api/v1/units/**` | Platform Admin, Owner, Manager | `@tenantSecurity.canManageCatalog()` |
| **Create/Update Product** | `/api/v1/products/**` | Platform Admin, Owner, Manager | `@tenantSecurity.canManageCatalog()` |
| **Manage Unit Conversions** | `/api/v1/products/{id}/units/**` | Platform Admin, Owner, Manager | `@tenantSecurity.canManageCatalog()` |
| **Receive Stock** | `/api/v1/inventory/receipts` | Platform Admin, Owner, Manager, Storekeeper | `@tenantSecurity.canReceiveStock()` |
| **Adjust Stock** | `/api/v1/inventory/adjustments` | Platform Admin, Owner, Manager, Storekeeper | `@tenantSecurity.canAdjustStock()` |
| **Read Balances & Reports** | `/api/v1/inventory/balances`, `/reports/**` | Platform Admin, Owner, Manager, Storekeeper, Cashier | `@tenantSecurity.canReadInventory()` |
