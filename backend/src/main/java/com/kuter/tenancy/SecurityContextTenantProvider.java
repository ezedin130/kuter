package com.kuter.tenancy;

import com.kuter.identity.KuterUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of CurrentTenantProvider that resolves tenant context from
 * ThreadLocal TenantContext first, and then from the authenticated SecurityContext.
 */
@Component
public class SecurityContextTenantProvider implements CurrentTenantProvider {

    @Override
    public Optional<UUID> getCurrentTenantId() {
        // Check ThreadLocal context
        Optional<UUID> threadTenant = TenantContext.getTenantId();
        if (threadTenant.isPresent()) {
            return threadTenant;
        }

        // Check authenticated principal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof KuterUserPrincipal principal) {
            UUID tenantId = principal.getCurrentTenantId();
            if (tenantId != null) {
                return Optional.of(tenantId);
            }
        }

        return Optional.empty();
    }

    @Override
    public UUID requireCurrentTenantId() {
        return getCurrentTenantId().orElseThrow(() ->
                new TenantNotFoundException("No active tenant context resolved for current operation"));
    }

    @Override
    public boolean hasTenantContext() {
        return getCurrentTenantId().isPresent();
    }
}
