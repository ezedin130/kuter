-- =============================================================================
-- Migration: V5__sales_enhancements.sql
-- Project: Kuter Building Materials Inventory Management SaaS
-- Description: Enhances sales and sale_items tables with idempotency keys,
--              void metadata, product snapshots, and query performance indexes.
-- =============================================================================

-- 1. Add idempotency key support to sales
ALTER TABLE sales ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sales_tenant_idempotency
    ON sales (tenant_id, idempotency_key);

-- 2. Add notes and void metadata to sales
ALTER TABLE sales ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS void_reason TEXT;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS voided_by UUID;

-- 3. Add product snapshots and item-level discount to sale_items
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS product_sku VARCHAR(100);
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000;

-- 4. Create performance indexes for sales lookups, date ranges, and reporting
CREATE INDEX IF NOT EXISTS idx_sales_tenant_status ON sales (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_sold_at ON sales (tenant_id, sold_at DESC);
CREATE INDEX IF NOT EXISTS idx_sales_tenant_cashier ON sales (tenant_id, cashier_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_tenant_product ON sale_items (tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_tenant_sale ON sale_items (tenant_id, sale_id);
