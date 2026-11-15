package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.BillingInterval;
import com.kuter.subscriptions.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriptionDetailDto {
    private UUID id;
    private UUID tenantId;
    private String tenantName;
    private String tenantCode;
    private UUID planId;
    private String planName;
    private String planCode;
    private BigDecimal price;
    private String currency;
    private BillingInterval billingInterval;
    private SubscriptionStatus status;
    private SubscriptionStatus effectiveStatus;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialStart;
    private Instant trialEnd;
    private boolean cancelAtPeriodEnd;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant suspendedAt;
    private String suspensionReason;
    private String notes;
    private UUID lastModifiedBy;
    private String lastModifiedByEmail;
    private boolean isCurrent;
    private Long version;
    private Integer productLimit;
    private long activeProductsCount;
    private Integer userLimit;
    private long activeUsersCount;
    private Integer branchLimit;
    private Instant createdAt;
    private Instant updatedAt;

    @com.fasterxml.jackson.annotation.JsonProperty("limits")
    public java.util.Map<String, Object> getLimits() {
        java.util.Map<String, Object> limits = new java.util.HashMap<>();
        limits.put("maxProducts", productLimit);
        limits.put("maxUsers", userLimit);
        limits.put("branchLimit", branchLimit);
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
