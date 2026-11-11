package com.kuter.subscriptions;

import com.kuter.common.ApiResponse;
import com.kuter.common.ForbiddenException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.subscriptions.dto.SubscriptionHistoryDto;
import com.kuter.subscriptions.dto.TenantSubscriptionResponseDto;
import com.kuter.tenancy.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped REST controller for shop members to inspect their current subscription,
 * usage, plan limits, and subscription history.
 */
@RestController
@RequestMapping("/api/v1")
public class TenantSubscriptionController {

    private final SubscriptionService subscriptionService;

    public TenantSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping({"/me/subscription", "/tenants/me/subscription"})
    public ResponseEntity<ApiResponse<TenantSubscriptionResponseDto>> getMySubscription() {
        UUID tenantId = TenantContext.requireTenantId();
        TenantSubscriptionResponseDto response = subscriptionService.getCurrentTenantSubscription(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription details retrieved successfully"));
    }

    @GetMapping({"/me/subscription/history", "/tenants/me/subscription/history"})
    @PreAuthorize("@tenantSecurity.hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SubscriptionHistoryDto>>> getMySubscriptionHistory() {
        UUID tenantId = TenantContext.requireTenantId();
        List<SubscriptionHistoryDto> history = subscriptionService.getTenantSubscriptionHistoryForShop(tenantId);
        return ResponseEntity.ok(ApiResponse.success(history, "Subscription history retrieved successfully"));
    }

    @GetMapping("/tenants/{tenantId}/subscription")
    public ResponseEntity<ApiResponse<TenantSubscriptionResponseDto>> getTenantSubscription(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal KuterUserPrincipal principal) {
        validateAccess(tenantId, principal);
        TenantSubscriptionResponseDto response = subscriptionService.getCurrentTenantSubscription(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Subscription details retrieved successfully"));
    }

    @GetMapping("/tenants/{tenantId}/subscription/history")
    @PreAuthorize("@tenantSecurity.isPlatformAdmin() or @tenantSecurity.canManageMembersOf(#tenantId)")
    public ResponseEntity<ApiResponse<List<SubscriptionHistoryDto>>> getTenantSubscriptionHistory(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal KuterUserPrincipal principal) {
        validateAccess(tenantId, principal);
        List<SubscriptionHistoryDto> history = subscriptionService.getTenantSubscriptionHistoryForShop(tenantId);
        return ResponseEntity.ok(ApiResponse.success(history, "Subscription history retrieved successfully"));
    }

    private void validateAccess(UUID tenantId, KuterUserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (principal.isPlatformAdmin()) {
            return;
        }
        if (!principal.hasMembershipIn(tenantId)) {
            throw new ForbiddenException("Access denied: You are not a member of shop " + tenantId);
        }
    }
}
