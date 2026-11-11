-- =============================================================================
-- Kuter SaaS - Phase Seven: Reports, Audit, and Operational Enhancements
-- Flyway Migration: V7__reports_and_audit_enhancements.sql
-- Compatible with PostgreSQL 18.6 and H2 (PostgreSQL Mode)
-- =============================================================================

-- 1. Add outcome column to audit_logs for explicit operational outcome tracking
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS outcome VARCHAR(50) NOT NULL DEFAULT 'SUCCESS';

-- 2. Audit logs query performance indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_action ON audit_logs (tenant_id, action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);

-- 3. Stock movements date-range and type query performance indexes
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_type_date ON stock_movements (tenant_id, movement_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_date ON stock_movements (tenant_id, created_at DESC);
