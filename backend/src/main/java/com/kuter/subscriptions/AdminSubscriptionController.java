package com.kuter.subscriptions;

import com.kuter.common.ApiResponse;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.subscriptions.dto.ActivateSubscriptionDto;
import com.kuter.subscriptions.dto.AdminSubscriptionDetailDto;
import com.kuter.subscriptions.dto.AssignSubscriptionDto;
import com.kuter.subscriptions.dto.CancelSubscriptionDto;
import com.kuter.subscriptions.dto.ChangePlanDto;
import com.kuter.subscriptions.dto.ExtendSubscriptionDto;
import com.kuter.subscriptions.dto.ReactivateSubscriptionDto;
import com.kuter.subscriptions.dto.StartTrialDto;
import com.kuter.subscriptions.dto.SubscriptionHistoryDto;
import com.kuter.subscriptions.dto.SubscriptionSummaryDto;
import com.kuter.subscriptions.dto.SuspendSubscriptionDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Platform administration REST controller for managing tenant subscriptions,
 * lifecycle transitions, extensions, suspensions, and subscription audit records.
 */
@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@PreAuthorize("@tenantSecurity.isPlatformAdmin()")
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    public AdminSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminSubscriptionDetailDto>>> listSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) UUID planId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<AdminSubscriptionDetailDto> page = subscriptionService.listSubscriptions(pageable, status, planId, tenantId, search);
        return ResponseEntity.ok(ApiResponse.success(page, "Subscriptions retrieved successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SubscriptionSummaryDto>> getSummary() {
        SubscriptionSummaryDto summary = subscriptionService.getSubscriptionSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Subscription summary statistics retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> getSubscription(@PathVariable UUID id) {
        AdminSubscriptionDetailDto sub = subscriptionService.getSubscription(id);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription details retrieved successfully"));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> assignSubscription(
            @Valid @RequestBody AssignSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto assigned = subscriptionService.assignSubscription(dto, admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(assigned, "Subscription plan assigned successfully"));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> activateSubscription(
            @PathVariable UUID id,
            @RequestBody(required = false) ActivateSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.activateSubscription(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription activated successfully"));
    }

    @PostMapping("/{id}/start-trial")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> startTrial(
            @PathVariable UUID id,
            @RequestBody(required = false) StartTrialDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.startTrial(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription trial started successfully"));
    }

    @PostMapping("/{id}/change-plan")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> changePlan(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePlanDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.changePlan(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription plan changed successfully"));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> extendSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody ExtendSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.extendSubscription(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription extended successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> suspendSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody SuspendSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.suspendSubscription(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription suspended successfully"));
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> reactivateSubscription(
            @PathVariable UUID id,
            @RequestBody(required = false) ReactivateSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.reactivateSubscription(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription reactivated successfully"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> cancelSubscription(
            @PathVariable UUID id,
            @Valid @RequestBody CancelSubscriptionDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        AdminSubscriptionDetailDto sub = subscriptionService.cancelSubscription(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(sub, "Subscription cancelled successfully"));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<SubscriptionHistoryDto>>> getSubscriptionHistory(@PathVariable UUID id) {
        List<SubscriptionHistoryDto> history = subscriptionService.getSubscriptionHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history, "Subscription history retrieved successfully"));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<AdminSubscriptionDetailDto>> getTenantSubscription(@PathVariable UUID tenantId) {
        AdminSubscriptionDetailDto sub = subscriptionService.getTenantSubscriptionForAdmin(tenantId);
        return ResponseEntity.ok(ApiResponse.success(sub, "Tenant subscription retrieved successfully"));
    }

    @GetMapping("/tenant/{tenantId}/history")
    public ResponseEntity<ApiResponse<List<SubscriptionHistoryDto>>> getTenantSubscriptionHistory(@PathVariable UUID tenantId) {
        List<SubscriptionHistoryDto> history = subscriptionService.getTenantSubscriptionHistory(tenantId);
        return ResponseEntity.ok(ApiResponse.success(history, "Tenant subscription history retrieved successfully"));
    }
}
