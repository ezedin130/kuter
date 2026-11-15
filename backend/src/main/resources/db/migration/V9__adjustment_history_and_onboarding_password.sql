-- =============================================================================
-- Migration V9: Adjustment History Granular Tracking and Registration Password
-- =============================================================================

-- 1. Extend stock_adjustment_items with detailed snapshot fields
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS adjustment_type VARCHAR(20) DEFAULT 'DECREASE';
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS entered_quantity NUMERIC(15, 4);
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS unit_id UUID;
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS unit_code VARCHAR(50);
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS conversion_factor NUMERIC(15, 4) DEFAULT 1.0000;
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS quantity_before NUMERIC(15, 4) NOT NULL DEFAULT 0.0000;
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS quantity_after NUMERIC(15, 4) NOT NULL DEFAULT 0.0000;
ALTER TABLE stock_adjustment_items ADD COLUMN IF NOT EXISTS valuation_impact NUMERIC(15, 4) NOT NULL DEFAULT 0.0000;

-- 2. Extend tenant_onboarding_requests with password_hash and email audit timestamps
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS email_sent_at TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS approval_email_sent_at TIMESTAMP WITHOUT TIME ZONE;

-- 3. Update existing stock_adjustment_items so quantity_before/after and type match quantity_change
UPDATE stock_adjustment_items
SET adjustment_type = CASE WHEN quantity_change >= 0 THEN 'INCREASE' ELSE 'DECREASE' END,
    entered_quantity = ABS(quantity_change),
    conversion_factor = 1.0000,
    valuation_impact = quantity_change * unit_cost
WHERE entered_quantity IS NULL;

-- 4. Set system default currency to ETB
ALTER TABLE tenants ALTER COLUMN currency SET DEFAULT 'ETB';
ALTER TABLE tenant_onboarding_requests ALTER COLUMN currency SET DEFAULT 'ETB';
ALTER TABLE subscription_plans ALTER COLUMN currency SET DEFAULT 'ETB';

UPDATE tenants SET currency = 'ETB' WHERE currency = 'USD';
UPDATE tenant_onboarding_requests SET currency = 'ETB' WHERE currency = 'USD';
UPDATE subscription_plans SET currency = 'ETB' WHERE currency = 'USD';
