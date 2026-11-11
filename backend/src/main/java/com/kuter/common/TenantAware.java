package com.kuter.common;

import java.util.UUID;

/**
 * Interface implemented by all entities that belong to a specific tenant shop.
 */
public interface TenantAware {

    UUID getTenantId();

    void setTenantId(UUID tenantId);
}
