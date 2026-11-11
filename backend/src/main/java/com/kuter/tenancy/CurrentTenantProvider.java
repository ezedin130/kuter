package com.kuter.tenancy;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for resolving the current authenticated tenant ID in application services.
 */
public interface CurrentTenantProvider {

    Optional<UUID> getCurrentTenantId();

    UUID requireCurrentTenantId();

    boolean hasTenantContext();
}
