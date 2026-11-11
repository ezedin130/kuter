# Kuter Shop Portal — Phase Ten Implementation

## 1. Overview
The **Kuter Shop Portal** is the dedicated web application for retail building-materials shop owners, yard managers, storekeepers, and cashiers. Built with **React 19**, **TypeScript**, and **Vite**, it integrates directly with the multi-tenant Spring Boot backend (`http://localhost:8080/api/v1`).

---

## 2. Port & Workspace Mapping
- **Port:** `5173` (`http://localhost:5173`)
- **Backend Service:** Spring Boot API on `http://localhost:8080` (reverse-proxied via `/api` in `vite.config.ts`)
- **Other Applications:**
  - `admin/` (Platform Administration Portal) — Port `5174`
  - `public/` (Public Marketing Website & Onboarding) — Port `5175`
  - `backend/` (Spring Boot API & PostgreSQL Database) — Port `8080`

---

## 3. Core Features Implemented

### 3.1 Authentication & Tenant Context Scoping
- **Login Flow:** Authenticates via `POST /api/v1/auth/login`.
- **Identity & Membership Resolution:** Profile retrieved from `GET /api/v1/auth/me`. The active tenant is extracted directly from the user's validated memberships—**never trusted from client URLs or form inputs**.
- **Context Injection:** Injects `Authorization: Bearer <jwt>` and `X-Tenant-ID: <activeTenantUuid>` on every request.
- **Role-Based Access Control:**
  - `OWNER` / `MANAGER`: Full access to catalog, stock, sales, voiding, financial margins, settings, and subscription quotas.
  - `STOREKEEPER`: Inbound stock receipts, stock adjustments, live stock balances, and ledger inspection.
  - `CASHIER`: Fast counter checkout, sales history, and customer operational receipt printing; cost of goods sold (COGS), gross margins, and inventory valuations are automatically redacted.

### 3.2 Counter Point of Sale (POS) & Checkout
- **Instant Product Search:** Real-time filtering by SKU, name, or barcode.
- **Dual-UOM Cart:** Cashiers can sell products in their base unit (e.g. `PCS`, `KG`) or any configured conversion unit (e.g. `BUNDLE`, `BOX`, `TON`) with automatic price derivation.
- **Line-Level & Order Discounts:** Instant recalculation of totals.
- **Idempotency Protection:** Generates unique `crypto.randomUUID()` keys per transaction to prevent double-charging or duplicate stock deduction on network glitches.
- **Printable Operational Receipt:** Instantly renders an itemized receipt with the non-payment disclaimer (*"Operational Sales Record - Not a Proof of Payment"*) and browser print integration (`window.print()`).

### 3.3 Inventory Operations & Ledger
- **Delivery Note Receiving (`POST /api/v1/inventory/receipts`):** Logs inbound vendor deliveries in bulk units (e.g. 20 Bundles) and derives base inventory (1,000 PCS) while updating the moving weighted-average cost.
- **Stock Adjustments (`POST /api/v1/inventory/adjustments`):** Pessimistic-lock adjustments for shrinkage, damaged materials, weather degradation, or physical stocktake discrepancies.
- **Immutable Ledger (`GET /api/v1/inventory/movements`):** Chronological, tamper-proof movement logs filtered by date, actor, and transaction type.

### 3.4 Product Catalog & Dual-UOM Conversions
- **Catalog Management:** Create and edit products with SKU, base measurement unit, selling price, and reorder threshold alerts.
- **Conversion Factor Engine (`/products/{id}/conversions`):** Add and manage multi-unit packaging definitions with optional custom selling prices.

### 3.5 Operational Reports & Quotas
- **Sales Analytics:** Aggregate revenue, transaction counts, average basket value, and daily breakdown intervals.
- **Warehouse Valuation:** Itemized stock valuation based on moving weighted-average unit costs.
- **Subscription Self-Inspection:** Live tracking of product limits and staff user quotas.
- **Shop Settings:** Profile management and staff team membership visibility.

---

## 4. Development & Testing Commands

```bash
# Install dependencies
npm install

# Start Vite development server on port 5173
npm run dev

# Run automated Vitest test suite
npm test

# Build production bundle with TypeScript type-checking
npm run build
```
