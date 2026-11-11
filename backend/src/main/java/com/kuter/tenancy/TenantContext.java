package com.kuter.tenancy;

import java.util.Optional;
import java.util.UUID;

/**
 * ThreadLocal container holding the resolved Tenant ID for the current request thread.
 * Must always be cleared upon request completion.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Prevent instantiation
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static UUID requireTenantId() {
        return getTenantId().orElseThrow(() ->
                new TenantNotFoundException("No tenant context available on current thread"));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
