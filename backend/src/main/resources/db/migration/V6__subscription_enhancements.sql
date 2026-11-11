-- =============================================================================
-- Migration: V6__subscription_enhancements.sql
-- Project: Kuter Building Materials Inventory Management SaaS
-- Description: Enhances subscription plans, tenant subscriptions, introduces
--              immutable subscription history ledger, and adds performance indexes.
-- =============================================================================

-- 1. Subscription plans enhancements
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'USD';
ALTER TABLE subscription_plans ADD COLUMN IF NOT EXISTS trial_days INTEGER DEFAULT 14;

-- 2. Tenant subscriptions enhancements
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS trial_start TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS trial_end TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS last_modified_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS is_current BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tenant_subscriptions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Subscription History Table for full lifecycle audit
CREATE TABLE IF NOT EXISTS subscription_history (
    id UUID PRIMARY KEY,
    subscription_id UUID REFERENCES tenant_subscriptions(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id UUID NOT NULL REFERENCES subscription_plans(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    previous_plan_id UUID,
    new_plan_id UUID,
    previous_period_end TIMESTAMP WITH TIME ZONE,
    new_period_end TIMESTAMP WITH TIME ZONE,
    reason TEXT,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 4. Composite and performance indexes
CREATE INDEX IF NOT EXISTS idx_sub_tenant_current ON tenant_subscriptions(tenant_id, is_current);
CREATE INDEX IF NOT EXISTS idx_sub_status_end ON tenant_subscriptions(status, current_period_end);
CREATE INDEX IF NOT EXISTS idx_sub_history_sub ON subscription_history(subscription_id);
CREATE INDEX IF NOT EXISTS idx_sub_history_tenant ON subscription_history(tenant_id, created_at DESC);
