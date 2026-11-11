package com.kuter.tenancy.dto;

import com.kuter.subscriptions.SubscriptionStatus;
import com.kuter.tenancy.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Representation of tenant operational status combined with subscription lifecycle metadata.
 * Keeps tenant operational status strictly separated from subscription lifecycle status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantStatusDetailDto {

    private UUID tenantId;
    private String code;
    private String name;
    private TenantStatus operationalStatus;
    private Instant createdAt;
    private String onboardingReference;

    // Subscription status metadata
    private UUID subscriptionId;
    private String planCode;
    private String planName;
    private SubscriptionStatus lifecycleStatus;
    private SubscriptionStatus effectiveStatus;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private Instant trialStart;
    private Instant trialEnd;
    private long daysRemaining;
    private boolean cancelAtPeriodEnd;
}
