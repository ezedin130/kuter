-- =============================================================================
-- Kuter SaaS — Dev & Demo Database Seed Script
-- Seeds platform administrators, sample tenants, subscriptions, and onboarding requests.
-- =============================================================================

-- 1. Platform Administrators & Demo Shop Users
-- BCrypt hash for "Admin@123456": $2a$10$cKVUIltM3kGXnoRSAl6I7e026ovb6USNEKbsjY0.sQFBVC1Lm5r5.
INSERT INTO users (
    id, email, password_hash, full_name, phone_number,
    is_platform_admin, status, created_at, updated_at
) VALUES 
(
    '00000000-0000-0000-0000-000000000001',
    'admin@kuterplatform.com',
    '$2a$10$cKVUIltM3kGXnoRSAl6I7e026ovb6USNEKbsjY0.sQFBVC1Lm5r5.',
    'Platform Administrator',
    '+1 555-010-0001',
    TRUE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000002',
    'operator@kuterplatform.com',
    '$2a$10$cKVUIltM3kGXnoRSAl6I7e026ovb6USNEKbsjY0.sQFBVC1Lm5r5.',
    'Platform Operator',
    '+1 555-010-0002',
    TRUE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '00000000-0000-0000-0000-000000000003',
    'owner@amoudi.com',
    '$2a$10$cKVUIltM3kGXnoRSAl6I7e026ovb6USNEKbsjY0.sQFBVC1Lm5r5.',
    'Tariq Al-Amoudi',
    '+1 555-020-0001',
    FALSE,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    is_platform_admin = EXCLUDED.is_platform_admin,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;

-- 2. Demo Tenants
INSERT INTO tenants (
    id, code, name, status, currency, timezone, created_at, updated_at
) VALUES
(
    'a0000000-0000-0000-0000-000000000001',
    'amoudi-materials',
    'Al-Amoudi Building Materials',
    'ACTIVE',
    'USD',
    'UTC',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000002',
    'kuter-express',
    'Kuter Express Hardware',
    'ACTIVE',
    'USD',
    'UTC',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000003',
    'redsea-timber',
    'Red Sea Timber & Steel',
    'SUSPENDED',
    'USD',
    'UTC',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- 3. Tenant Memberships
INSERT INTO tenant_memberships (
    id, tenant_id, user_id, role, status, created_at, updated_at
) VALUES
(
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000003',
    'OWNER',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (tenant_id, user_id) DO NOTHING;

-- 4. Tenant Subscriptions
INSERT INTO tenant_subscriptions (
    id, tenant_id, plan_id, status,
    current_period_start, current_period_end,
    cancel_at_period_end, is_current, version,
    created_at, updated_at
) VALUES
(
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '11111111-1111-1111-1111-111111111102',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '365 days',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'c0000000-0000-0000-0000-000000000002',
    'a0000000-0000-0000-0000-000000000002',
    '11111111-1111-1111-1111-111111111101',
    'TRIALING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (id) DO NOTHING;

-- 5. Sample Onboarding Requests for Review Queue
INSERT INTO tenant_onboarding_requests (
    id, reference_code, shop_name, owner_full_name, owner_email, owner_phone,
    requested_plan_code, status, preferred_shop_code, business_address, currency,
    applicant_notes, created_at, updated_at
) VALUES
(
    'd0000000-0000-0000-0000-000000000001',
    'REQ-2026-00001',
    'Nile Valley Concrete & Sand',
    'Ahmed Hassan',
    'ahmed@nileconcrete.com',
    '+1 555-030-0001',
    'growth',
    'PENDING',
    'nile-concrete',
    'Port Road Yard 4, Industrial Zone',
    'USD',
    'Wholesale distributor supplying structural aggregates, cement, and reinforcement steel.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'd0000000-0000-0000-0000-000000000002',
    'REQ-2026-00002',
    'Apex Masonry Supply',
    'Sarah Jenkins',
    'sarah@apexmasonry.com',
    '+1 555-030-0002',
    'starter',
    'PENDING',
    'apex-masonry',
    '108 Commercial Way, Building B',
    'USD',
    'Single location brick, paver, and mortar retailer.',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'd0000000-0000-0000-0000-000000000003',
    'REQ-2026-00003',
    'Al-Amoudi Building Materials',
    'Tariq Al-Amoudi',
    'owner@amoudi.com',
    '+1 555-020-0001',
    'growth',
    'APPROVED',
    'amoudi-materials',
    'Al-Khobar Industrial Park, Gate 3',
    'USD',
    'Approved initial customer.',
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP - INTERVAL '6 days'
)
ON CONFLICT (reference_code) DO NOTHING;
