package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.BillingInterval;
import com.kuter.subscriptions.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSubscriptionResponseDto {
    private UUID subscriptionId;
    private UUID planId;
    private String planName;
    private String planCode;
    private String planDescription;
    private BigDecimal price;
    private String currency;
    private BillingInterval billingInterval;
    private SubscriptionStatus status;
    private SubscriptionStatus effectiveStatus;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialStart;
    private Instant trialEnd;
    private Long daysRemaining;
    private boolean cancelAtPeriodEnd;
    private Integer productLimit;
    private long activeProductsCount;
    private boolean productLimitReached;
    private Integer userLimit;
    private long activeUsersCount;
    private boolean userLimitReached;
    private Integer branchLimit;
    private List<String> features;

    @com.fasterxml.jackson.annotation.JsonProperty("limits")
    public java.util.Map<String, Object> getLimits() {
        java.util.Map<String, Object> limits = new java.util.HashMap<>();
        limits.put("maxProducts", productLimit);
        limits.put("maxUsers", userLimit);
        limits.put("branchLimit", branchLimit);
        limits.put("allowApiAccess", true);
        limits.put("allowAdvancedReports", true);
        return limits;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("usage")
    public java.util.Map<String, Object> getUsage() {
        java.util.Map<String, Object> usage = new java.util.HashMap<>();
        usage.put("productCount", activeProductsCount);
        usage.put("userCount", activeUsersCount);
        return usage;
    }
}
