-- =============================================================================
-- Flyway Migration: V11__remove_audit_logs.sql
-- Description: Completely remove the platform audit_logs table, dedicated indexes,
--              and foreign key constraints.
-- =============================================================================

DROP TABLE IF EXISTS audit_logs CASCADE;
