package com.kuter.tenancy;

import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.TenantRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Security authorization evaluation bean for method-level security expressions.
 * Usage examples in @PreAuthorize:
 *   "@tenantSecurity.isMemberOf(#tenantId)"
 *   "@tenantSecurity.canManageMembersOf(#tenantId)"
 *   "@tenantSecurity.hasAnyRole('OWNER', 'MANAGER')"
 */
@Service("tenantSecurity")
public class TenantSecurityService {

    public boolean isMemberOf(UUID tenantId) {
        if (tenantId == null) {
            return false;
        }

        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null) {
            return false;
        }

        return principal.hasMembershipIn(tenantId);
    }

    public boolean hasRoleInTenant(UUID tenantId, String requiredRole) {
        if (tenantId == null || requiredRole == null) {
            return false;
        }

        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null) {
            return false;
        }

        Optional<TenantRole> userRole = principal.getRoleIn(tenantId);
        return userRole.isPresent() && userRole.get().name().equalsIgnoreCase(requiredRole);
    }

    public boolean hasAnyRoleInTenant(UUID tenantId, String... requiredRoles) {
        if (tenantId == null || requiredRoles == null) {
            return false;
        }

        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null) {
            return false;
        }

        Optional<TenantRole> userRole = principal.getRoleIn(tenantId);
        if (userRole.isEmpty()) {
            return false;
        }

        String actualRole = userRole.get().name();
        return Arrays.stream(requiredRoles).anyMatch(r -> r.equalsIgnoreCase(actualRole));
    }

    public boolean canManageMembersOf(UUID tenantId) {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasRoleInTenant(tenantId, "OWNER");
    }

    public boolean hasPermission(String permissionName) {
        UUID currentTenantId = TenantContext.getTenantId().orElse(null);
        if (currentTenantId == null) {
            return false;
        }
        return hasPermissionInTenant(currentTenantId, permissionName);
    }

    public boolean hasPermissionInTenant(UUID tenantId, String permissionName) {
        if (isPlatformAdmin()) {
            return true;
        }
        if (tenantId == null || permissionName == null) {
            return false;
        }
        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        if (principal == null) {
            return false;
        }
        // If caller is OWNER of this tenant, OWNER possesses all permissions
        if (principal.getRoleIn(tenantId).map(r -> r == TenantRole.OWNER).orElse(false)) {
            return true;
        }
        // Non-owners can NEVER possess PRICES_MANAGE or USERS_MANAGE
        if ("PRICES_MANAGE".equalsIgnoreCase(permissionName) || "USERS_MANAGE".equalsIgnoreCase(permissionName)) {
            return false;
        }
        return principal.getMembershipIn(tenantId)
                .map(m -> m.getPermissions() != null && m.getPermissions().stream()
                        .anyMatch(p -> p.name().equalsIgnoreCase(permissionName)))
                .orElse(false);
    }

    public boolean hasAnyRole(String... requiredRoles) {
        UUID currentTenantId = TenantContext.getTenantId().orElse(null);
        if (currentTenantId == null) {
            return false;
        }
        return hasAnyRoleInTenant(currentTenantId, requiredRoles);
    }

    public boolean canManageCatalog() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER");
    }

    public boolean canReceiveStock() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER", "STOREKEEPER");
    }

    public boolean canAdjustStock() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER", "STOREKEEPER");
    }

    public boolean canReadInventory() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER", "STOREKEEPER", "CASHIER", "VIEWER");
    }

    public boolean canCreateSale() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER", "CASHIER");
    }

    public boolean canVoidSale() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER");
    }

    public boolean canReadSales() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER", "CASHIER", "STOREKEEPER", "VIEWER");
    }

    public boolean canViewSalesCostReports() {
        if (isPlatformAdmin()) {
            return true;
        }
        return hasAnyRole("OWNER", "MANAGER");
    }

    public boolean isPlatformAdmin() {
        KuterUserPrincipal principal = getAuthenticatedPrincipal();
        return principal != null && principal.isPlatformAdmin();
    }

    private KuterUserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof KuterUserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
