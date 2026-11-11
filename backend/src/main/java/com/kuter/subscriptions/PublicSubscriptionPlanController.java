package com.kuter.subscriptions;

import com.kuter.common.ApiResponse;
import com.kuter.subscriptions.dto.PublicSubscriptionPlanDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public REST controller serving public subscription plan tiers for marketing and onboarding pages.
 */
@RestController
@RequestMapping("/api/v1/public/subscription-plans")
public class PublicSubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    public PublicSubscriptionPlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicSubscriptionPlanDto>>> getPublicPlans() {
        List<PublicSubscriptionPlanDto> plans = subscriptionService.getPublicPlans();
        return ResponseEntity.ok(ApiResponse.success(plans, "Public subscription plans retrieved successfully"));
    }
}
