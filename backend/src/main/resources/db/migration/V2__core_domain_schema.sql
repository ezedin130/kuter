-- =============================================================================
-- Migration: V2__core_domain_schema.sql
-- Project: Kuter Building Materials Inventory Management SaaS
-- Description: Core domain schema covering tenancy, identity, product catalog,
--              inventory, sales, subscriptions, and auditing.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. TENANCY & IDENTITY
-- -----------------------------------------------------------------------------

CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    business_registration_number VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_tenants_code UNIQUE (code)
);

CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    is_platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_membership_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_membership_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_membership_tenant ON tenant_memberships (tenant_id);
CREATE INDEX idx_membership_user ON tenant_memberships (user_id);

CREATE TABLE tenant_onboarding_requests (
    id UUID PRIMARY KEY,
    reference_code VARCHAR(50) NOT NULL,
    shop_name VARCHAR(255) NOT NULL,
    owner_full_name VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    owner_phone VARCHAR(50),
    requested_plan_code VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    reviewer_id UUID,
    reviewer_notes TEXT,
    reviewed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_onboarding_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_onboarding_ref UNIQUE (reference_code)
);

CREATE INDEX idx_onboarding_status ON tenant_onboarding_requests (status);

-- -----------------------------------------------------------------------------
-- 2. SUBSCRIPTIONS
-- -----------------------------------------------------------------------------

CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price NUMERIC(15, 4) NOT NULL,
    billing_interval VARCHAR(20) NOT NULL,
    product_limit INTEGER,
    user_limit INTEGER,
    branch_limit INTEGER,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    features_summary TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_subscription_plans_code UNIQUE (code)
);

CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    current_period_start TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_sub_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_id) REFERENCES subscription_plans(id) ON DELETE RESTRICT
);

CREATE INDEX idx_subscription_tenant ON tenant_subscriptions (tenant_id);

-- -----------------------------------------------------------------------------
-- 3. PRODUCT CATALOG
-- -----------------------------------------------------------------------------

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uk_categories_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_categories_tenant ON categories (tenant_id);

CREATE TABLE unit_definitions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_units_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uk_units_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_units_tenant ON unit_definitions (tenant_id);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id UUID,
    base_unit_id UUID NOT NULL,
    cost_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    selling_price NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    reorder_threshold NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    image_url VARCHAR(1024),
    barcode VARCHAR(100),
    brand VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_base_unit FOREIGN KEY (base_unit_id) REFERENCES unit_definitions(id) ON DELETE RESTRICT,
    CONSTRAINT uk_products_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_products_tenant ON products (tenant_id);
CREATE INDEX idx_products_tenant_category ON products (tenant_id, category_id);
CREATE INDEX idx_products_tenant_status ON products (tenant_id, status);

CREATE TABLE product_unit_conversions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    conversion_factor NUMERIC(15, 4) NOT NULL,
    is_selling_unit BOOLEAN NOT NULL DEFAULT TRUE,
    is_receiving_unit BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_conv_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_unit FOREIGN KEY (unit_id) REFERENCES unit_definitions(id) ON DELETE RESTRICT,
    CONSTRAINT uk_product_unit_conversion UNIQUE (product_id, unit_id)
);

CREATE INDEX idx_conv_tenant_product ON product_unit_conversions (tenant_id, product_id);

-- -----------------------------------------------------------------------------
-- 4. INVENTORY
-- -----------------------------------------------------------------------------

CREATE TABLE stock_balances (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity_on_hand NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    average_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    last_counted_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_stock_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_stock_balance_tenant_product UNIQUE (tenant_id, product_id)
);

CREATE INDEX idx_stock_balances_tenant ON stock_balances (tenant_id);

CREATE TABLE stock_receivings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    receiving_number VARCHAR(100) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    delivery_note_ref VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    total_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    notes TEXT,
    received_by UUID,
    received_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_receiving_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_receiving_user FOREIGN KEY (received_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_receiving_tenant_number UNIQUE (tenant_id, receiving_number)
);

CREATE INDEX idx_receivings_tenant ON stock_receivings (tenant_id);

CREATE TABLE stock_receiving_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    receiving_id UUID NOT NULL,
    product_id UUID NOT NULL,
    received_unit_id UUID NOT NULL,
    received_quantity NUMERIC(15, 4) NOT NULL,
    conversion_factor NUMERIC(15, 4) NOT NULL,
    base_quantity NUMERIC(15, 4) NOT NULL,
    unit_cost NUMERIC(15, 4) NOT NULL,
    total_cost NUMERIC(15, 4) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_rcv_item_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_rcv_item_receiving FOREIGN KEY (receiving_id) REFERENCES stock_receivings(id) ON DELETE CASCADE,
    CONSTRAINT fk_rcv_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rcv_item_unit FOREIGN KEY (received_unit_id) REFERENCES unit_definitions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_rcv_items_receiving ON stock_receiving_items (receiving_id);
CREATE INDEX idx_rcv_items_tenant_product ON stock_receiving_items (tenant_id, product_id);

CREATE TABLE stock_adjustments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    adjustment_number VARCHAR(100) NOT NULL,
    reason_code VARCHAR(50) NOT NULL,
    notes TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    created_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_adj_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_adj_user FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_adj_tenant_number UNIQUE (tenant_id, adjustment_number)
);

CREATE INDEX idx_adjustments_tenant ON stock_adjustments (tenant_id);

CREATE TABLE stock_adjustment_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    adjustment_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity_change NUMERIC(15, 4) NOT NULL,
    unit_cost NUMERIC(15, 4) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_adj_item_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_adj_item_adjustment FOREIGN KEY (adjustment_id) REFERENCES stock_adjustments(id) ON DELETE CASCADE,
    CONSTRAINT fk_adj_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT
);

CREATE INDEX idx_adj_items_adjustment ON stock_adjustment_items (adjustment_id);
CREATE INDEX idx_adj_items_tenant_product ON stock_adjustment_items (tenant_id, product_id);

CREATE TABLE stock_movements (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(50),
    quantity_change NUMERIC(15, 4) NOT NULL,
    balance_after NUMERIC(15, 4) NOT NULL,
    unit_cost NUMERIC(15, 4) NOT NULL,
    notes TEXT,
    actor_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_movements_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_movements_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_movements_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_movements_tenant_product ON stock_movements (tenant_id, product_id, created_at DESC);
CREATE INDEX idx_movements_reference ON stock_movements (reference_id);

-- -----------------------------------------------------------------------------
-- 5. SALES
-- -----------------------------------------------------------------------------

CREATE TABLE sales (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255),
    customer_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    subtotal NUMERIC(15, 4) NOT NULL,
    tax_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    discount_amount NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    total_amount NUMERIC(15, 4) NOT NULL,
    total_cost NUMERIC(15, 4) NOT NULL,
    sold_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    cashier_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_sales_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_sales_cashier FOREIGN KEY (cashier_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uk_sales_tenant_invoice UNIQUE (tenant_id, invoice_number)
);

CREATE INDEX idx_sales_tenant_date ON sales (tenant_id, sold_at DESC);

CREATE TABLE sale_items (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    sale_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sold_unit_id UUID NOT NULL,
    sold_quantity NUMERIC(15, 4) NOT NULL,
    conversion_factor NUMERIC(15, 4) NOT NULL,
    base_quantity NUMERIC(15, 4) NOT NULL,
    unit_price NUMERIC(15, 4) NOT NULL,
    unit_cost NUMERIC(15, 4) NOT NULL,
    total_price NUMERIC(15, 4) NOT NULL,
    total_cost NUMERIC(15, 4) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_sale_item_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_item_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sale_item_unit FOREIGN KEY (sold_unit_id) REFERENCES unit_definitions(id) ON DELETE RESTRICT
);

CREATE INDEX idx_sale_items_sale ON sale_items (sale_id);
CREATE INDEX idx_sale_items_tenant_product ON sale_items (tenant_id, product_id);

-- -----------------------------------------------------------------------------
-- 6. AUDIT LOGS
-- -----------------------------------------------------------------------------

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    actor_id UUID,
    actor_email VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(50),
    correlation_id VARCHAR(64),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_audit_tenant_created ON audit_logs (tenant_id, created_at DESC);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
