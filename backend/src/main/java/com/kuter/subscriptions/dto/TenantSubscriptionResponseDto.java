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
}
