# Kuter API Specification — Phase Five: Sales Processing, Stock Deduction, and Reporting

## Overview

The Sales Processing API enables building materials merchants to execute single-operation counter sales, support multi-product carts with dual-UOM conversions, automatically deduct on-hand inventory under pessimistic locks, snapshot moving weighted-average costs, perform controlled sale voiding with inventory restoration, print operational receipts, and generate financial reports.

**Base URL**: `/api/v1/sales`  
**Authentication**: Bearer JWT (`Authorization: Bearer <token>`)  
**Multi-Tenant Header**: `X-Tenant-ID: <tenant-uuid>`

---

## 1. Create and Finalize Counter Sale

### `POST /api/v1/sales`

Executes an atomic checkout: validates catalog items, deducts inventory on hand in canonical lock order, snapshots WAC costs, creates an immutable sales invoice, logs stock movement records, and emits audit entries.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`
- **Idempotency**: Supported via optional `idempotencyKey`.

#### Request Headers
| Header | Type | Required | Description |
|---|---|---|---|
| `Authorization` | String | Yes | `Bearer <JWT>` |
| `X-Tenant-ID` | UUID | Yes | Active Tenant Shop ID |
| `Content-Type` | String | Yes | `application/json` |

#### Request Body Schema
```json
{
  "customerName": "Ahmed Contractor",
  "customerPhone": "+254712345678",
  "notes": "Delivered to site gate 3",
  "discountAmount": 10.0000,
  "idempotencyKey": "pos-terminal-01-tx-10948",
  "items": [
    {
      "productId": "79b32c12-32b4-42b1-b3b3-82b132837261",
      "unitDefinitionId": "e3b52e8b-73c5-40a5-8b5a-42ed3b83340b",
      "quantity": 20.0000,
      "unitPrice": 12.0000,
      "discountAmount": 0.0000
    }
  ]
}
```

| Field | Type | Validation | Description |
|---|---|---|---|
| `customerName` | String | Optional, max 255 | Customer identifier or name |
| `customerPhone` | String | Optional, max 50 | Customer contact number |
| `notes` | String | Optional | General sale notes or delivery instructions |
| `discountAmount` | Decimal | $\ge 0.0000$ | Overall header discount |
| `idempotencyKey` | String | Optional, max 100 | Client-generated unique transaction key |
| `items` | Array | Min 1 item | Line items in checkout cart |
| `items[].productId` | UUID | `@NotNull` | Target product |
| `items[].unitDefinitionId` | UUID | `@NotNull` | Selected unit of measure |
| `items[].quantity` | Decimal | $> 0.0000$ | Quantity in selected unit |
| `items[].unitPrice` | Decimal | Optional, $\ge 0.0000$ | Custom unit price. If omitted, uses catalog selling price $\times$ conversion factor |
| `items[].discountAmount` | Decimal | Optional, $\ge 0.0000$ | Line-level discount |

#### Response (`201 Created` - Owner / Manager View)
```json
{
  "success": true,
  "message": "Sale completed and stock deducted successfully",
  "data": {
    "id": "f78c67c6-d169-4cd9-aa8a-e076af1dbd8f",
    "tenantId": "9aee5274-ee83-45fd-82e6-3e16ef51d5f8",
    "invoiceNumber": "INV-2026-50194",
    "customerName": "Ahmed Contractor",
    "customerPhone": "+254712345678",
    "status": "COMPLETED",
    "subtotal": 240.0000,
    "discountAmount": 10.0000,
    "taxAmount": 0.0000,
    "totalAmount": 230.0000,
    "totalCost": 160.0000,
    "grossProfit": 70.0000,
    "soldAt": "2026-10-29T08:15:30Z",
    "cashierId": "70639ec7-3b3c-4068-9327-5c673af5acad",
    "cashierName": "Arthur Alpha",
    "notes": "Delivered to site gate 3",
    "idempotencyKey": "pos-terminal-01-tx-10948",
    "items": [
      {
        "id": "7db04f57-8f15-4fdb-b4f2-f5edbc4d6563",
        "productId": "79b32c12-32b4-42b1-b3b3-82b132837261",
        "productName": "Portland Cement 50kg",
        "productSku": "CEM-50KG",
        "soldUnitId": "e3b52e8b-73c5-40a5-8b5a-42ed3b83340b",
        "soldUnitCode": "BAG",
        "soldQuantity": 20.0000,
        "conversionFactor": 1.0000,
        "baseQuantity": 20.0000,
        "unitPrice": 12.0000,
        "discountAmount": 0.0000,
        "totalPrice": 240.0000,
        "unitCost": 8.0000,
        "totalCost": 160.0000,
        "grossProfit": 80.0000
      }
    ],
    "createdAt": "2026-10-29T08:15:30Z"
  },
  "timestamp": "2026-10-29T08:15:30.125Z",
  "correlationId": "62e9d504-eba5-40ad-bcdf-1bf390e036ff"
}
```
*Note*: For `CASHIER` users, proprietary fields (`totalCost`, `grossProfit`, `items[].unitCost`, `items[].totalCost`, `items[].grossProfit`) are omitted.

---

## 2. Retrieve Sale Details

### `GET /api/v1/sales/{saleId}`

Retrieves complete information for a single sale.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`
- **Status Codes**:
  - `200 OK`: Sale found.
  - `404 Not Found`: Sale does not exist or belongs to another tenant.

---

## 3. List Sales with Search and Pagination

### `GET /api/v1/sales`

Lists sales for the authenticated tenant with date range filtering, customer search, cashier filter, and status filters.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`
- **Query Parameters**:
  - `startDate` (String, optional): Beginning of range (format: `YYYY-MM-DD` or ISO-8601).
  - `endDate` (String, optional): End of range (format: `YYYY-MM-DD` or ISO-8601).
  - `status` (SaleStatus, optional): `COMPLETED`, `VOIDED`, etc.
  - `search` (String, optional): Substring match on `invoiceNumber` or `customerName`.
  - `cashierId` (UUID, optional): Filter by cashier.
  - `page` (int, default `0`), `size` (int, default `20`), `sort` (default `soldAt,desc`).

---

## 4. Controlled Sale Voiding

### `POST /api/v1/sales/{saleId}/void`

Voids a completed sale. Atomically restores on-hand inventory under pessimistic locks, recomputes stock average cost valuation, records compensating `RETURN` stock movements, marks sale status as `VOIDED`, and emits audit log.

- **Required Roles**: `OWNER`, `MANAGER` (`CASHIER` and `STOREKEEPER` receive `403 Forbidden`).

#### Request Body Schema
```json
{
  "reason": "Customer returned unopened items with manager approval"
}
```

| Field | Type | Validation | Description |
|---|---|---|---|
| `reason` | String | `@NotBlank`, max 500 | Required operational audit reason |

#### Response (`200 OK`)
```json
{
  "success": true,
  "message": "Sale voided and stock restored successfully",
  "data": {
    "id": "f78c67c6-d169-4cd9-aa8a-e076af1dbd8f",
    "invoiceNumber": "INV-2026-50194",
    "status": "VOIDED",
    "voidReason": "Customer returned unopened items with manager approval",
    "voidedAt": "2026-10-29T08:25:00Z",
    "voidedBy": "70639ec7-3b3c-4068-9327-5c673af5acad",
    "voidedByName": "David Owner"
  }
}
```

#### Error Scenarios
- `400 Bad Request`: Sale is already `VOIDED` or reason is blank.
- `403 Forbidden`: User role is `CASHIER` or `STOREKEEPER`.
- `404 Not Found`: Sale does not exist or belongs to another tenant.

---

## 5. Operational Printable Receipt

### `GET /api/v1/sales/{saleId}/receipt`

Generates an operational printable receipt record for counter staff and customers. Contains store name, receipt/invoice number, line items, prices, discounts, totals, and operational disclaimer. Contains NO sensitive wholesale costs or profit margins.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`

#### Response (`200 OK`)
```json
{
  "success": true,
  "message": "Sale receipt retrieved successfully",
  "data": {
    "shopName": "Alpha Building Materials",
    "shopPhone": null,
    "shopEmail": null,
    "shopAddress": null,
    "shopCity": null,
    "shopCountry": null,
    "invoiceNumber": "INV-2026-50194",
    "soldAt": "2026-10-29T08:15:30Z",
    "cashierName": "Arthur Alpha",
    "customerName": "Ahmed Contractor",
    "customerPhone": "+254712345678",
    "items": [
      {
        "productName": "Portland Cement 50kg",
        "productSku": "CEM-50KG",
        "unitCode": "BAG",
        "quantity": 20.0000,
        "unitPrice": 12.0000,
        "discountAmount": 0.0000,
        "totalPrice": 240.0000
      }
    ],
    "subtotal": 240.0000,
    "discountAmount": 10.0000,
    "taxAmount": 0.0000,
    "totalAmount": 230.0000,
    "notes": "Delivered to site gate 3",
    "status": "COMPLETED",
    "voidReason": null,
    "voidedAt": null,
    "disclaimer": "Operational Sales Record - Not a Proof of Payment"
  }
}
```

---

## 6. Operational Sales Summary Report

### `GET /api/v1/sales/reports/summary`

Aggregates store sales, voided orders, revenue, discounts, and daily breakdown for an operational date window.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`
- **Query Parameters**:
  - `startDate` (String, optional): Defaults to 30 days ago. Format: `YYYY-MM-DD` or ISO-8601 UTC timestamp.
  - `endDate` (String, optional): Defaults to current time. Format: `YYYY-MM-DD` or ISO-8601 UTC timestamp.

#### Response (`200 OK` - Owner / Manager View)
```json
{
  "success": true,
  "message": "Sales summary report generated successfully",
  "data": {
    "tenantId": "9aee5274-ee83-45fd-82e6-3e16ef51d5f8",
    "startDate": "2026-10-01T00:00:00Z",
    "endDate": "2026-10-29T23:59:59Z",
    "generatedAt": "2026-10-29T08:30:00Z",
    "totalSalesCount": 42,
    "totalSalesAmount": 12450.0000,
    "totalCostOfGoodsSold": 8200.0000,
    "grossProfit": 4250.0000,
    "grossMarginPercentage": 34.14,
    "voidedSalesCount": 2,
    "voidedSalesAmount": 380.0000,
    "dailyBreakdowns": [
      {
        "date": "2026-10-29",
        "salesCount": 12,
        "salesAmount": 3450.0000,
        "costOfGoodsSold": 2200.0000,
        "grossProfit": 1250.0000
      }
    ]
  }
}
```
*Note*: When queried by a `CASHIER`, `totalCostOfGoodsSold`, `grossProfit`, `grossMarginPercentage`, and daily `costOfGoodsSold`/`grossProfit` are omitted.

---

## 7. Product Sales Ranking Report

### `GET /api/v1/sales/reports/products`

Ranks shop inventory items by sales performance, volume sold (converted to base units), and total revenue.

- **Required Roles**: `OWNER`, `MANAGER`, `CASHIER`
- **Query Parameters**:
  - `startDate` (String, optional)
  - `endDate` (String, optional)
  - `limit` (int, default `20`)

#### Response (`200 OK` - Owner / Manager View)
```json
{
  "success": true,
  "message": "Product sales report generated successfully",
  "data": {
    "tenantId": "9aee5274-ee83-45fd-82e6-3e16ef51d5f8",
    "startDate": "2026-10-01T00:00:00Z",
    "endDate": "2026-10-29T23:59:59Z",
    "generatedAt": "2026-10-29T08:30:00Z",
    "products": [
      {
        "productId": "79b32c12-32b4-42b1-b3b3-82b132837261",
        "productSku": "CEM-50KG",
        "productName": "Portland Cement 50kg",
        "baseUnitCode": "BAG",
        "totalQuantitySold": 120.0000,
        "totalRevenue": 1440.0000,
        "transactionCount": 14,
        "totalCost": 960.0000,
        "grossProfit": 480.0000
      }
    ]
  }
}
```
*Note*: When queried by a `CASHIER`, `totalCost` and `grossProfit` are omitted.
