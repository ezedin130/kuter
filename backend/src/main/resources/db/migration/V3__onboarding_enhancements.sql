-- =============================================================================
-- Migration: V3__onboarding_enhancements.sql
-- Project: Kuter Building Materials Inventory Management SaaS
-- Description: Enhances tenant onboarding requests with Figma-aligned fields,
--              links created tenant ID, and seeds baseline subscription tiers.
-- =============================================================================

-- 1. Enhance tenant_onboarding_requests with business fields and tenant link
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS preferred_shop_code VARCHAR(50);
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS business_address VARCHAR(255);
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'USD';
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS applicant_notes TEXT;
ALTER TABLE tenant_onboarding_requests ADD COLUMN IF NOT EXISTS created_tenant_id UUID;

ALTER TABLE tenant_onboarding_requests
    ADD CONSTRAINT fk_onboarding_created_tenant
    FOREIGN KEY (created_tenant_id) REFERENCES tenants(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_onboarding_created_tenant ON tenant_onboarding_requests (created_tenant_id);

-- 2. Seed default subscription tiers matching Figma design plans
INSERT INTO subscription_plans (
    id, code, name, description, price, billing_interval,
    product_limit, user_limit, branch_limit, is_public, is_active,
    features_summary, created_at, updated_at
) VALUES 
(
    '11111111-1111-1111-1111-111111111101',
    'starter',
    'Starter',
    'Essential dual-unit inventory control for single yard building materials shops.',
    49.0000,
    'MONTHLY',
    500,
    3,
    1,
    TRUE,
    TRUE,
    'Up to 500 products, 3 staff seats, counter checkout, basic UOM conversion',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '11111111-1111-1111-1111-111111111102',
    'growth',
    'Growth',
    'High-volume materials distribution, multiple warehouse locations, and advanced conversion rules.',
    119.0000,
    'MONTHLY',
    2500,
    10,
    3,
    TRUE,
    TRUE,
    'Up to 2,500 products, 10 staff seats, delivery note receiving, multi-location stock ledger',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '11111111-1111-1111-1111-111111111103',
    'enterprise',
    'Enterprise',
    'Custom limits, dedicated support, and enterprise audit capabilities.',
    279.0000,
    'MONTHLY',
    10000,
    50,
    10,
    TRUE,
    TRUE,
    'Up to 10,000 products, 50 staff seats, unlimited branches, priority phone support',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
