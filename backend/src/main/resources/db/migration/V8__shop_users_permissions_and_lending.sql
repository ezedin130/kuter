-- =============================================================================
-- Migration V8: Shop Users, Granular Permissions, and Inventory Lending/Borrowing
-- =============================================================================

-- 1. Tenant Membership Permissions
CREATE TABLE tenant_membership_permissions (
    membership_id UUID NOT NULL,
    permission VARCHAR(50) NOT NULL,
    PRIMARY KEY (membership_id, permission),
    CONSTRAINT fk_mem_perm_membership FOREIGN KEY (membership_id) 
        REFERENCES tenant_memberships(id) ON DELETE CASCADE
);

CREATE INDEX idx_mem_perm_membership ON tenant_membership_permissions(membership_id);

-- Seed existing memberships with safe default permissions
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'PRODUCTS_VIEW' FROM tenant_memberships;
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'INVENTORY_VIEW' FROM tenant_memberships;
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'SALES_VIEW' FROM tenant_memberships;

-- Owner exclusive defaults
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'PRICES_MANAGE' FROM tenant_memberships WHERE role = 'OWNER';
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'USERS_MANAGE' FROM tenant_memberships WHERE role = 'OWNER';

-- Role-based delegable defaults
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'PRODUCTS_MANAGE' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'INVENTORY_RECEIVE' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'STOREKEEPER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'INVENTORY_ADJUST' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'STOREKEEPER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'SALES_CREATE' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'CASHIER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'SALES_REPORTS_VIEW' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'LENDING_REQUEST' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'STOREKEEPER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'LENDING_MANAGE' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'STOREKEEPER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'LENDING_VIEW' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER', 'STOREKEEPER');
INSERT INTO tenant_membership_permissions (membership_id, permission)
SELECT id, 'SUBSCRIPTION_VIEW' FROM tenant_memberships WHERE role IN ('OWNER', 'MANAGER');

-- 2. Simplified Stock Loans (Lending & Borrowing with External Counterparties)
CREATE TABLE stock_loans (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    loan_reference VARCHAR(64) NOT NULL UNIQUE,
    direction VARCHAR(20) NOT NULL, -- 'BORROW' or 'LOAN'
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'PARTIALLY_RETURNED', 'RETURNED', 'CANCELLED'
    
    -- External Counterparty details
    counterparty_name VARCHAR(255) NOT NULL,
    counterparty_contact_person VARCHAR(255),
    counterparty_phone VARCHAR(50),
    counterparty_address TEXT,
    
    -- Local Product Snapshot
    product_id UUID NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_id UUID NOT NULL,
    unit_code VARCHAR(50) NOT NULL,
    conversion_factor NUMERIC(15, 4) NOT NULL DEFAULT 1.0000,
    
    -- Quantities
    initial_quantity NUMERIC(15, 4) NOT NULL,
    initial_base_quantity NUMERIC(15, 4) NOT NULL,
    returned_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    returned_base_quantity NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    outstanding_base_quantity NUMERIC(15, 4) NOT NULL,
    
    -- Cost Snapshot
    unit_cost_snapshot NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    
    -- Dates & Notes
    transaction_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expected_return_date TIMESTAMP WITHOUT TIME ZONE,
    notes TEXT,
    cancellation_reason TEXT,
    
    -- Auditing
    created_by_user_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    
    CONSTRAINT fk_stock_loans_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_loans_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_loans_unit FOREIGN KEY (unit_id) REFERENCES unit_definitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_loans_user FOREIGN KEY (created_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_stock_loans_tenant_dir ON stock_loans(tenant_id, direction, status);
CREATE INDEX idx_stock_loans_tenant_prod ON stock_loans(tenant_id, product_id);
CREATE INDEX idx_stock_loans_reference ON stock_loans(loan_reference);

-- 3. Stock Loan Return Records (Partial & Complete Return History)
CREATE TABLE stock_loan_returns (
    id UUID PRIMARY KEY,
    loan_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    return_reference VARCHAR(64) NOT NULL,
    return_quantity NUMERIC(15, 4) NOT NULL,
    return_base_quantity NUMERIC(15, 4) NOT NULL,
    unit_id UUID NOT NULL,
    unit_code VARCHAR(50) NOT NULL,
    return_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    notes TEXT,
    received_by_user_id UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    
    CONSTRAINT fk_loan_returns_loan FOREIGN KEY (loan_id) REFERENCES stock_loans(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_returns_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_returns_unit FOREIGN KEY (unit_id) REFERENCES unit_definitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_loan_returns_user FOREIGN KEY (received_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_loan_returns_loan ON stock_loan_returns(loan_id);
CREATE INDEX idx_loan_returns_tenant ON stock_loan_returns(tenant_id, return_date DESC);
