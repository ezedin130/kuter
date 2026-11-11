-- =============================================================================
-- Migration: V4__inventory_enhancements.sql
-- Project: Kuter Building Materials Inventory Management SaaS
-- Description: Enhances inventory tables with idempotency keys, image storage
--              identifiers, and performance indexes for catalog & inventory reports.
-- =============================================================================

-- 1. Add idempotency key support to stock_receivings
ALTER TABLE stock_receivings ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_receiving_tenant_idempotency
    ON stock_receivings (tenant_id, idempotency_key);

-- 2. Add idempotency key support to stock_adjustments
ALTER TABLE stock_adjustments ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_adj_tenant_idempotency
    ON stock_adjustments (tenant_id, idempotency_key);

-- 3. Add image storage key to products for object-storage metadata
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_storage_key VARCHAR(255);

-- 4. Create performance indexes for inventory search, movements, and reports
CREATE INDEX IF NOT EXISTS idx_products_tenant_sku_lookup ON products (tenant_id, sku);
CREATE INDEX IF NOT EXISTS idx_products_tenant_name ON products (tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_date ON stock_movements (tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_receivings_tenant_date ON stock_receivings (tenant_id, received_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_adjustments_tenant_date ON stock_adjustments (tenant_id, created_at DESC);
