package com.kuter.subscriptions;

import com.kuter.common.ApiResponse;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.subscriptions.dto.CreateSubscriptionPlanDto;
import com.kuter.subscriptions.dto.SubscriptionPlanDto;
import com.kuter.subscriptions.dto.UpdatePlanStatusDto;
import com.kuter.subscriptions.dto.UpdateSubscriptionPlanDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform administration REST controller for managing subscription plan tiers and limits.
 */
@RestController
@RequestMapping("/api/v1/subscription-plans")
@PreAuthorize("@tenantSecurity.isPlatformAdmin()")
public class SubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    public SubscriptionPlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        SubscriptionPlanDto created = subscriptionService.createPlan(dto, admin);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Subscription plan created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionPlanDto>>> getPlans(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "price", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<SubscriptionPlanDto> page = subscriptionService.getPlans(pageable, isActive, isPublic, search);
        return ResponseEntity.ok(ApiResponse.success(page, "Subscription plans retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> getPlan(@PathVariable UUID id) {
        SubscriptionPlanDto plan = subscriptionService.getPlan(id);
        return ResponseEntity.ok(ApiResponse.success(plan, "Subscription plan details retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionPlanDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        SubscriptionPlanDto updated = subscriptionService.updatePlan(id, dto, admin);
        return ResponseEntity.ok(ApiResponse.success(updated, "Subscription plan updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SubscriptionPlanDto>> updatePlanStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanStatusDto dto,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        SubscriptionPlanDto updated = subscriptionService.updatePlanStatus(id, dto.getActive(), admin);
        return ResponseEntity.ok(ApiResponse.success(updated, "Subscription plan status updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlan(
            @PathVariable UUID id,
            @AuthenticationPrincipal KuterUserPrincipal admin) {
        subscriptionService.deletePlan(id, admin);
        return ResponseEntity.ok(ApiResponse.success(null, "Subscription plan deleted successfully"));
    }
}
