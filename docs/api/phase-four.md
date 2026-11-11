# Kuter API Specification — Phase Four: Product Catalog and Inventory Management

This document defines the complete REST API contract for Phase Four of **Kuter**, a multi-tenant Building Materials Inventory Management SaaS.

All endpoints adhere to the following global conventions:
- **Base URI:** `/api/v1`
- **Content Type:** `application/json;charset=UTF-8`
- **Standard Envelope:** Responses use standard envelopes:
  - Success: `{"success": true, "data": ..., "message": "...", "timestamp": "..."}`
  - Error: `{"status": 4xx/5xx, "error": "...", "message": "...", "path": "...", "correlationId": "...", "timestamp": "...", "details": [...]}`
- **Correlation ID:** Clients may send `X-Correlation-ID` header. If absent, backend generates a UUID and echoes it in `X-Correlation-ID` and `X-Request-ID` response headers.
- **Tenant Context:** Multi-tenant endpoints accept the `X-Tenant-ID` header containing the tenant's UUID. This is validated cryptographically against the authenticated user's active memberships.
- **Authorization:** Enforced via Spring Security Method Security using `@tenantSecurity` expressions. Required tenant roles are specified per endpoint.

---

## 1. Product Category APIs

### 1.1 Create Category
Creates a new product categorization group within the tenant shop.

- **Method & Path:** `POST /api/v1/categories`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN` (`@tenantSecurity.canManageCatalog()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "name": "Cement & Aggregates",
  "description": "Standard grey cement, white cement, sand, gravel, and bulk aggregates"
}
```
- **Validation Rules:**
  - `name`: Non-blank, max 100 characters. Unique per tenant (case-insensitive).
  - `description`: Optional, max 500 characters.
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "7b8e1f02-a89c-43df-9750-f823f6630f14",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "name": "Cement & Aggregates",
    "description": "Standard grey cement, white cement, sand, gravel, and bulk aggregates",
    "active": true,
    "createdAt": "2026-10-28T14:00:00Z",
    "updatedAt": "2026-10-28T14:00:00Z"
  },
  "message": "Category created successfully",
  "timestamp": "2026-10-28T14:00:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request`: Validation failure on missing name.
  - `409 Conflict`: Category with the same name already exists in this tenant.

---

### 1.2 List Categories
Lists all categories for the authenticated tenant.

- **Method & Path:** `GET /api/v1/categories`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Query Parameters:**
  - `includeInactive`: Boolean (optional, default `false`). If true, includes deactivated categories.
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": [
    {
      "id": "7b8e1f02-a89c-43df-9750-f823f6630f14",
      "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
      "name": "Cement & Aggregates",
      "description": "Standard grey cement, white cement, sand, gravel, and bulk aggregates",
      "active": true,
      "createdAt": "2026-10-28T14:00:00Z",
      "updatedAt": "2026-10-28T14:00:00Z"
    }
  ],
  "message": "Categories retrieved successfully",
  "timestamp": "2026-10-28T14:00:05Z"
}
```

---

### 1.3 Get Category by ID
Retrieves details of a single category.

- **Method & Path:** `GET /api/v1/categories/{id}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "7b8e1f02-a89c-43df-9750-f823f6630f14",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "name": "Cement & Aggregates",
    "description": "Standard grey cement, white cement, sand, gravel, and bulk aggregates",
    "active": true,
    "createdAt": "2026-10-28T14:00:00Z",
    "updatedAt": "2026-10-28T14:00:00Z"
  },
  "message": "Category retrieved successfully",
  "timestamp": "2026-10-28T14:00:10Z"
}
```
- **Error Responses:**
  - `404 Not Found`: Category does not exist or belongs to another tenant.

---

### 1.4 Update Category
Updates category name, description, or active status.

- **Method & Path:** `PUT /api/v1/categories/{id}`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "name": "Cement, Mortar & Aggregates",
  "description": "Updated category description",
  "active": true
}
```
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "7b8e1f02-a89c-43df-9750-f823f6630f14",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "name": "Cement, Mortar & Aggregates",
    "description": "Updated category description",
    "active": true,
    "createdAt": "2026-10-28T14:00:00Z",
    "updatedAt": "2026-10-28T14:05:00Z"
  },
  "message": "Category updated successfully",
  "timestamp": "2026-10-28T14:05:00Z"
}
```
- **Error Responses:**
  - `404 Not Found`: Category not found in active tenant.
  - `409 Conflict`: New name conflicts with another existing category in tenant.

---

### 1.5 Delete or Deactivate Category
Deletes category if no products are assigned. If products are linked, safely soft-deactivates the category instead of breaking historical product records.

- **Method & Path:** `DELETE /api/v1/categories/{id}`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": null,
  "message": "Category deleted successfully",
  "timestamp": "2026-10-28T14:06:00Z"
}
```
*(Note: If products are linked, `message` returns `"Category deactivated because it has linked products"` with status `200 OK`)*.

---

## 2. Unit Definition APIs

### 2.1 List Unit Definitions
Lists all measurement unit definitions. If tenant has no custom units configured, the platform automatically auto-seeds standard building materials units (`PCS`, `BAG`, `TON`, `KG`, `MTR`, `BOX`, `BNDL`, `PALLET`, `SHT`).

- **Method & Path:** `GET /api/v1/units`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": [
    {
      "id": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
      "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
      "code": "PCS",
      "name": "Pieces",
      "symbol": "pcs",
      "createdAt": "2026-10-28T14:00:00Z"
    },
    {
      "id": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
      "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
      "code": "BAG",
      "name": "Bags",
      "symbol": "bag",
      "createdAt": "2026-10-28T14:00:00Z"
    }
  ],
  "message": "Unit definitions retrieved successfully",
  "timestamp": "2026-10-28T14:01:00Z"
}
```

---

### 2.2 Create Unit Definition
Creates a custom unit definition for the tenant shop.

- **Method & Path:** `POST /api/v1/units`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "code": "DRUM",
  "name": "Steel Drum 200L",
  "symbol": "drm"
}
```
- **Validation Rules:**
  - `code`: Non-blank, max 20 chars, auto-trimmed to uppercase. Unique per tenant.
  - `name`: Non-blank, max 50 chars.
  - `symbol`: Optional, max 10 chars.
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "5a4b3c2d-1e0f-9a8b-7c6d-5e4f3a2b1c0d",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "code": "DRUM",
    "name": "Steel Drum 200L",
    "symbol": "drm",
    "createdAt": "2026-10-28T14:10:00Z"
  },
  "message": "Unit definition created successfully",
  "timestamp": "2026-10-28T14:10:00Z"
}
```
- **Error Responses:**
  - `409 Conflict`: Unit definition code already exists for this tenant.

---

### 2.3 Get Unit Definition by ID
Retrieves details of a single measurement unit.

- **Method & Path:** `GET /api/v1/units/{id}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "id": "5a4b3c2d-1e0f-9a8b-7c6d-5e4f3a2b1c0d",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "code": "DRUM",
    "name": "Steel Drum 200L",
    "symbol": "drm",
    "createdAt": "2026-10-28T14:10:00Z"
  },
  "message": "Unit definition retrieved successfully",
  "timestamp": "2026-10-28T14:10:05Z"
}
```

---

## 3. Product Catalog APIs

### 3.1 Create Product
Creates a new building material product with SKU, category, base unit, optional image metadata, and optional initial stock.

- **Method & Path:** `POST /api/v1/products`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "sku": "CEM-OPC-50KG",
  "name": "Portland Grey Cement 50KG",
  "description": "High performance Ordinary Portland Cement Type I for structural concrete",
  "categoryId": "7b8e1f02-a89c-43df-9750-f823f6630f14",
  "baseUnitId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
  "basePrice": 24.5000,
  "initialCostPrice": 18.0000,
  "initialQuantity": 100.0000,
  "minStockAlert": 20.0000,
  "maxStockAlert": 500.0000,
  "location": "Warehouse Bay A-12",
  "imageUrl": "https://cdn.kuter.cloud/tenants/alpha/products/cem-opc.png",
  "imageStorageKey": "tenants/alpha/products/cem-opc.png"
}
```
- **Validation Rules:**
  - `sku`: Non-blank, max 50 chars, trimmed to uppercase. Unique per tenant.
  - `name`: Non-blank, max 200 chars.
  - `baseUnitId`: Must reference an active `unit_definitions` record in this tenant.
  - `basePrice`: Greater than 0.
  - `initialQuantity`: Non-negative. If $> 0$, an initial `RECEIPT` movement and stock balance are automatically initialized.
  - `initialCostPrice`: Required if `initialQuantity > 0`.
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "sku": "CEM-OPC-50KG",
    "name": "Portland Grey Cement 50KG",
    "description": "High performance Ordinary Portland Cement Type I for structural concrete",
    "categoryId": "7b8e1f02-a89c-43df-9750-f823f6630f14",
    "categoryName": "Cement & Aggregates",
    "baseUnitId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
    "baseUnitCode": "BAG",
    "baseUnitName": "Bags",
    "basePrice": 24.5000,
    "status": "ACTIVE",
    "minStockAlert": 20.0000,
    "maxStockAlert": 500.0000,
    "location": "Warehouse Bay A-12",
    "imageUrl": "https://cdn.kuter.cloud/tenants/alpha/products/cem-opc.png",
    "imageStorageKey": "tenants/alpha/products/cem-opc.png",
    "currentStockOnHand": 100.0000,
    "currentAverageCost": 18.0000,
    "createdAt": "2026-10-28T14:15:00Z",
    "updatedAt": "2026-10-28T14:15:00Z"
  },
  "message": "Product created successfully",
  "timestamp": "2026-10-28T14:15:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request`: Invalid unit or category references.
  - `409 Conflict`: SKU already exists in this tenant.

---

### 3.2 List Products (Paged & Searchable)
Lists products in tenant inventory with pagination, category filter, status filter, and keyword search.

- **Method & Path:** `GET /api/v1/products`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Query Parameters:**
  - `search`: Keyword string (optional; matches SKU or name case-insensitively).
  - `categoryId`: UUID (optional).
  - `status`: ProductStatus enum (optional; `ACTIVE`, `INACTIVE`, `DISCONTINUED`).
  - `page`: Page index (optional, default `0`).
  - `size`: Page size (optional, default `20`).
  - `sort`: Sort specification (optional, default `name,asc`).
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
        "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
        "sku": "CEM-OPC-50KG",
        "name": "Portland Grey Cement 50KG",
        "categoryId": "7b8e1f02-a89c-43df-9750-f823f6630f14",
        "categoryName": "Cement & Aggregates",
        "baseUnitCode": "BAG",
        "basePrice": 24.5000,
        "status": "ACTIVE",
        "quantityOnHand": 100.0000,
        "averageCost": 18.0000,
        "lowStock": false,
        "imageUrl": "https://cdn.kuter.cloud/tenants/alpha/products/cem-opc.png"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "message": "Products retrieved successfully",
  "timestamp": "2026-10-28T14:16:00Z"
}
```

---

### 3.3 Get Product Detail
Retrieves complete product record including alerts, storage keys, and live stock on hand.

- **Method & Path:** `GET /api/v1/products/{id}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):** `ProductDetailResponseDto` (matches format in 3.1).
- **Error Responses:**
  - `404 Not Found`: Product not found in tenant.

---

### 3.4 Update Product
Updates product descriptive metadata, pricing, alerts, location, and image references. Base unit and SKU remain immutable to preserve inventory integrity.

- **Method & Path:** `PUT /api/v1/products/{id}`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "name": "Premium Portland Grey Cement 50KG",
  "description": "Updated structural cement formula",
  "categoryId": "7b8e1f02-a89c-43df-9750-f823f6630f14",
  "basePrice": 25.0000,
  "minStockAlert": 30.0000,
  "maxStockAlert": 600.0000,
  "location": "Warehouse Bay A-14",
  "imageUrl": "https://cdn.kuter.cloud/tenants/alpha/products/cem-opc-v2.png",
  "imageStorageKey": "tenants/alpha/products/cem-opc-v2.png"
}
```
- **Success Response (`200 OK`):** Updated `ProductDetailResponseDto`.

---

### 3.5 Patch Product Status
Direct status transition (`ACTIVE`, `INACTIVE`, `DISCONTINUED`).

- **Method & Path:** `PATCH /api/v1/products/{id}/status`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Request Body:**
```json
{
  "status": "DISCONTINUED"
}
```
- **Success Response (`200 OK`):** Updated `ProductDetailResponseDto`.

---

### 3.6 Delete or Deactivate Product
Deletes product if no stock movements or receiving records exist. If historical ledger movements exist, safely deactivates the product (`status: INACTIVE`) to safeguard accounting audit trails.

- **Method & Path:** `DELETE /api/v1/products/{id}`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": null,
  "message": "Product deleted successfully",
  "timestamp": "2026-10-28T14:18:00Z"
}
```
*(Note: If movements exist, `message` returns `"Product has historical inventory movements; marked as INACTIVE instead of deleting"` with status `200 OK`)*.

---

## 4. Product-Specific Unit Conversion APIs

### 4.1 Configure Product Unit Conversion
Establishes a secondary unit conversion for a product (e.g. 1 PALLET = 50 BAG, or 1 BOX = 100 PCS).

- **Method & Path:** `POST /api/v1/products/{id}/units`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "unitDefinitionId": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
  "conversionFactor": 50.0000
}
```
- **Validation Rules:**
  - `unitDefinitionId`: Required; cannot match the product's own base unit.
  - `conversionFactor`: Must be strictly positive ($> 0$).
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "8f7e6d5c-4b3a-2109-8765-43210fedcba9",
    "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
    "unitDefinitionId": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
    "unitCode": "PALLET",
    "unitName": "Pallets",
    "unitSymbol": "plt",
    "conversionFactor": 50.0000,
    "baseUnit": false
  },
  "message": "Product unit conversion configured successfully",
  "timestamp": "2026-10-28T14:20:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request`: Cannot configure conversion for product's base unit.
  - `409 Conflict`: Conversion for this unit definition already exists for the product.

---

### 4.2 List Product Units
Lists all available measurement units for the product. Always returns the **base unit** first (`baseUnit: true`, `conversionFactor: 1.0000`), followed by all configured secondary units (`baseUnit: false`).

- **Method & Path:** `GET /api/v1/products/{id}/units`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": [
    {
      "id": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "unitDefinitionId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
      "unitCode": "BAG",
      "unitName": "Bags",
      "unitSymbol": "bag",
      "conversionFactor": 1.0000,
      "baseUnit": true
    },
    {
      "id": "8f7e6d5c-4b3a-2109-8765-43210fedcba9",
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "unitDefinitionId": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
      "unitCode": "PALLET",
      "unitName": "Pallets",
      "unitSymbol": "plt",
      "conversionFactor": 50.0000,
      "baseUnit": false
    }
  ],
  "message": "Product units retrieved successfully",
  "timestamp": "2026-10-28T14:20:05Z"
}
```

---

### 4.3 Delete Product Unit Conversion
Removes a secondary unit conversion.

- **Method & Path:** `DELETE /api/v1/products/{id}/units/{unitId}`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": null,
  "message": "Product unit conversion removed successfully",
  "timestamp": "2026-10-28T14:21:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request`: Cannot delete base unit.

---

## 5. Inbound Stock Receiving APIs

### 5.1 Post Stock Receiving
Records inbound shipment from supplier. Automatically converts secondary units to base quantities, recalculates moving weighted-average cost (WAC), records immutable ledger movements, updates on-hand stock balances, and guarantees idempotency.

- **Method & Path:** `POST /api/v1/inventory/receipts`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN` (`@tenantSecurity.canReceiveStock()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body:**
```json
{
  "supplierName": "Saudi Ready Mix & Cement Co.",
  "invoiceNumber": "INV-2026-8812",
  "notes": "Delivered to Main Yard",
  "idempotencyKey": "recv-tx-20261028-001",
  "items": [
    {
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "unitDefinitionId": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
      "quantityReceived": 2.0000,
      "unitCost": 900.0000
    }
  ]
}
```
- **Unit Conversion & Costing Logic:**
  - If received in `PALLET` (factor 50) with `quantityReceived = 2.0000` at `unitCost = 900.0000`:
    - Base quantity received: $2.0000 \times 50.0000 = 100.0000\text{ BAG}$.
    - Base unit cost: $900.0000 / 50.0000 = 18.0000\text{ SAR/BAG}$.
    - Total cost: $1,800.0000\text{ SAR}$.
  - Moving Weighted-Average Cost formula:
    $$\text{New WAC} = \frac{(Q_{\text{on-hand}} \times C_{\text{current}}) + (Q_{\text{received}} \times C_{\text{inbound}})}{Q_{\text{on-hand}} + Q_{\text{received}}}$$
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "11223344-5566-7788-99aa-bbccddeeff00",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "supplierName": "Saudi Ready Mix & Cement Co.",
    "invoiceNumber": "INV-2026-8812",
    "notes": "Delivered to Main Yard",
    "receivedBy": "e8f30d08-f80d-4116-b043-224450b6c51d",
    "totalCost": 1800.0000,
    "idempotencyKey": "recv-tx-20261028-001",
    "createdAt": "2026-10-28T14:25:00Z",
    "items": [
      {
        "id": "aa11bb22-cc33-dd44-ee55-ff6677889900",
        "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
        "productName": "Portland Grey Cement 50KG",
        "productSku": "CEM-OPC-50KG",
        "unitDefinitionId": "2d1a3e4b-9c8a-4d7e-b6f1-3a0c5e7b9d1f",
        "unitCode": "PALLET",
        "quantityReceived": 2.0000,
        "conversionFactor": 50.0000,
        "baseQuantity": 100.0000,
        "unitCost": 900.0000,
        "baseUnitCost": 18.0000,
        "totalCost": 1800.0000
      }
    ]
  },
  "message": "Stock receipt processed successfully",
  "timestamp": "2026-10-28T14:25:00Z"
}
```
- **Idempotency Protection:**
  - If a request with the identical `idempotencyKey` is repeated, the backend detects the duplicate, skips double-crediting balances, and returns the original `StockReceivingResponseDto` with `200 OK`.

---

### 5.2 List Stock Receipts
Lists paged stock receiving transaction history.

- **Method & Path:** `GET /api/v1/inventory/receipts`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Query Parameters:** `page`, `size`, `sort` (default `createdAt,desc`)
- **Success Response (`200 OK`):** Page of `StockReceivingResponseDto`.

---

### 5.3 Get Stock Receipt by ID
Retrieves details of a specific stock receiving transaction.

- **Method & Path:** `GET /api/v1/inventory/receipts/{id}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Success Response (`200 OK`):** `StockReceivingResponseDto`.

---

## 6. Stock Adjustment APIs

### 6.1 Post Stock Adjustment
Executes stock adjustments for inventory shrinkage, breakage, physical cycle counts, or audits. Supports explicit quantity deltas (`quantityAdjusted`) or physical count targets (`targetQuantity`).

- **Method & Path:** `POST /api/v1/inventory/adjustments`
- **Access Control:** `STOREKEEPER`, `SHOP_MANAGER`, `OWNER`, or `ROLE_PLATFORM_ADMIN` (`@tenantSecurity.canAdjustStock()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Request Body (Explicit Delta):**
```json
{
  "reason": "DAMAGED",
  "notes": "2 bags broken during forklift transit",
  "idempotencyKey": "adj-tx-20261028-002",
  "items": [
    {
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "unitDefinitionId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
      "quantityAdjusted": -2.0000
    }
  ]
}
```
- **Request Body (Audit Count Reconciliation Mode):**
```json
{
  "reason": "PHYSICAL_COUNT",
  "notes": "Month-end physical count audit",
  "idempotencyKey": "adj-tx-20261028-003",
  "items": [
    {
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "unitDefinitionId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
      "targetQuantity": 198.0000
    }
  ]
}
```
- **Costing Rules on Adjustments:**
  - **Downward Adjustments:** Preserves weighted-average unit cost exactly ($C_{\text{new}} = C_{\text{current}}$); reduces total inventory value proportionally.
  - **Upward Adjustments:** Inherits current weighted-average cost to maintain valuation consistency.
  - **Negative Stock Prevention:** Rejects any adjustment that would cause on-hand quantity to drop below zero ($Q_{\text{on-hand}} + \Delta Q < 0$).
- **Success Response (`201 Created`):**
```json
{
  "success": true,
  "data": {
    "id": "99887766-5544-3322-1100-aabbccddeeff",
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "reason": "DAMAGED",
    "notes": "2 bags broken during forklift transit",
    "adjustedBy": "e8f30d08-f80d-4116-b043-224450b6c51d",
    "idempotencyKey": "adj-tx-20261028-002",
    "createdAt": "2026-10-28T14:30:00Z",
    "items": [
      {
        "id": "bb22cc33-dd44-ee55-ff66-001122334455",
        "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
        "productName": "Portland Grey Cement 50KG",
        "productSku": "CEM-OPC-50KG",
        "unitDefinitionId": "3e2b4f5c-0d9b-5e8f-c7a2-4b1d6f8c0e2a",
        "unitCode": "BAG",
        "previousQuantity": 200.0000,
        "quantityAdjusted": -2.0000,
        "newQuantity": 198.0000,
        "unitCost": 18.0000
      }
    ]
  },
  "message": "Stock adjustment processed successfully",
  "timestamp": "2026-10-28T14:30:00Z"
}
```
- **Error Responses:**
  - `400 Bad Request`: Adjustment would result in negative on-hand inventory.
  - `400 Bad Request`: Neither `quantityAdjusted` nor `targetQuantity` was provided.

---

### 6.2 List Stock Adjustments
Lists paged stock adjustment transaction history.

- **Method & Path:** `GET /api/v1/inventory/adjustments`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Query Parameters:** `page`, `size`, `sort` (default `createdAt,desc`)
- **Success Response (`200 OK`):** Page of `StockAdjustmentResponseDto`.

---

### 6.3 Get Stock Adjustment by ID
Retrieves details of a specific stock adjustment.

- **Method & Path:** `GET /api/v1/inventory/adjustments/{id}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Success Response (`200 OK`):** `StockAdjustmentResponseDto`.

---

## 7. Inventory Balances & Stock Movement APIs

### 7.1 List Inventory Balances
Lists current on-hand quantities, average costs, total valuation, and alert thresholds across products.

- **Method & Path:** `GET /api/v1/inventory/balances`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Query Parameters:**
  - `lowStockOnly`: Boolean (optional, default `false`). If true, filters strictly to items where $Q_{\text{on-hand}} \le \text{minStockAlert}$.
  - `page`: Page index (optional, default `0`).
  - `size`: Page size (optional, default `20`).
  - `sort`: Sort specification (optional, default `product.name,asc`).
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "77665544-3322-1100-ffeeddccbbaa",
        "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
        "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
        "productSku": "CEM-OPC-50KG",
        "productName": "Portland Grey Cement 50KG",
        "categoryName": "Cement & Aggregates",
        "baseUnitCode": "BAG",
        "quantityOnHand": 198.0000,
        "averageCost": 18.0000,
        "totalValue": 3564.0000,
        "minStockAlert": 20.0000,
        "maxStockAlert": 500.0000,
        "lowStock": false,
        "lastMovementAt": "2026-10-28T14:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "message": "Inventory balances retrieved successfully",
  "timestamp": "2026-10-28T14:35:00Z"
}
```

---

### 7.2 Get Product Balance by ID
Retrieves live stock balance for an individual product.

- **Method & Path:** `GET /api/v1/inventory/balances/{productId}`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Success Response (`200 OK`):** `StockBalanceResponseDto` (matches item in 7.1).
- **Error Responses:**
  - `404 Not Found`: Balance record not found for product in this tenant.

---

### 7.3 List Stock Movements Ledger
Queries the immutable audit ledger of all inventory inflows and outflows.

- **Method & Path:** `GET /api/v1/inventory/movements`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Query Parameters:**
  - `productId`: UUID (optional).
  - `movementType`: MovementType enum (optional; `RECEIPT`, `ADJUSTMENT`, `SALE`, `TRANSFER`, `RETURN`).
  - `page`: Page index (optional, default `0`).
  - `size`: Page size (optional, default `20`).
  - `sort`: Sort specification (optional, default `createdAt,desc`).
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "c1d2e3f4-a5b6-7c8d-9e0f-1a2b3c4d5e6f",
        "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
        "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
        "productSku": "CEM-OPC-50KG",
        "productName": "Portland Grey Cement 50KG",
        "movementType": "ADJUSTMENT",
        "quantity": -2.0000,
        "unitCost": 18.0000,
        "totalCost": -36.0000,
        "balanceAfter": 198.0000,
        "referenceType": "STOCK_ADJUSTMENT",
        "referenceId": "99887766-5544-3322-1100-aabbccddeeff",
        "notes": "2 bags broken during forklift transit",
        "performedBy": "e8f30d08-f80d-4116-b043-224450b6c51d",
        "createdAt": "2026-10-28T14:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "message": "Stock movements retrieved successfully",
  "timestamp": "2026-10-28T14:36:00Z"
}
```

---

## 8. Inventory Reports APIs

### 8.1 Inventory Valuation Report
Generates live portfolio valuation metrics including total SKUs, aggregated inventory value, total units on hand, and categorized breakdowns.

- **Method & Path:** `GET /api/v1/inventory/reports/valuation`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": {
    "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
    "generatedAt": "2026-10-28T14:40:00Z",
    "totalSkus": 14,
    "totalQuantityOnHand": 1250.0000,
    "totalInventoryValue": 45320.0000,
    "categoryBreakdowns": [
      {
        "categoryId": "7b8e1f02-a89c-43df-9750-f823f6630f14",
        "categoryName": "Cement & Aggregates",
        "skuCount": 4,
        "totalQuantity": 450.0000,
        "totalValuation": 12800.0000
      },
      {
        "categoryId": "8c9f2a13-b90d-54ea-a861-093407741025",
        "categoryName": "Steel Reinforcement & Mesh",
        "skuCount": 6,
        "totalQuantity": 600.0000,
        "totalValuation": 28500.0000
      }
    ]
  },
  "message": "Inventory valuation report generated successfully",
  "timestamp": "2026-10-28T14:40:00Z"
}
```

---

### 8.2 Low-Stock Replenishment Report
Retrieves an actionable list of products whose on-hand quantity has breached or reached their configured minimum threshold ($Q_{\text{on-hand}} \le \text{minStockAlert}$).

- **Method & Path:** `GET /api/v1/inventory/reports/low-stock`
- **Access Control:** Any tenant member (`@tenantSecurity.canReadInventory()`)
- **Headers:**
  - `Authorization: Bearer <jwt>`
  - `X-Tenant-ID: <tenant-uuid>`
- **Success Response (`200 OK`):**
```json
{
  "success": true,
  "data": [
    {
      "id": "77665544-3322-1100-ffeeddccbbaa",
      "tenantId": "c273db71-b5fe-4ca7-9370-c9817400bb06",
      "productId": "e4f8d2a1-7c9b-4e8f-a1b2-3c4d5e6f7a8b",
      "productSku": "CEM-OPC-50KG",
      "productName": "Portland Grey Cement 50KG",
      "categoryName": "Cement & Aggregates",
      "baseUnitCode": "BAG",
      "quantityOnHand": 18.0000,
      "averageCost": 18.0000,
      "totalValue": 324.0000,
      "minStockAlert": 20.0000,
      "maxStockAlert": 500.0000,
      "lowStock": true,
      "lastMovementAt": "2026-10-28T14:30:00Z"
    }
  ],
  "message": "Low stock report generated successfully",
  "timestamp": "2026-10-28T14:41:00Z"
}
```

---

## 9. Error and Status Code Mapping

| Status Code | Meaning | Common Scenarios |
| :--- | :--- | :--- |
| `200 OK` | Success | Reading resources, reports, balances, or idempotent transaction replays. |
| `201 Created` | Resource Created | Creating categories, unit definitions, products, unit conversions, receipts, or adjustments. |
| `400 Bad Request` | Validation Failure | Missing required fields, negative unit cost, adjustment leading to negative stock, or assigning base unit as conversion unit. |
| `401 Unauthorized` | Unauthenticated | Missing or expired JWT bearer token. |
| `403 Forbidden` | Access Denied | Tenant membership mismatch or insufficient role (e.g. `CASHIER` attempting to receive stock or mutate catalog). |
| `404 Not Found` | Resource Not Found | Referenced category, product, or unit does not exist in the active tenant. |
| `409 Conflict` | Unique Violation | Duplicate SKU, duplicate category name, or duplicate unit code in the tenant. |
| `500 Internal Error` | Server Exception | Uncaught runtime exception or database connectivity loss. |
