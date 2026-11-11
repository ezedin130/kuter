package com.kuter.subscriptions;

import com.kuter.subscriptions.dto.SubscriptionUsageDto;

import java.time.Clock;
import java.util.UUID;

/**
 * Service evaluating plan entitlements, usage limits, and effective subscription status.
 */
public interface EntitlementService {

    SubscriptionStatus getEffectiveStatus(UUID tenantId);

    boolean isSubscriptionActive(UUID tenantId);

    void assertActiveSubscription(UUID tenantId);

    void checkProductLimit(UUID tenantId);

    void checkUserLimit(UUID tenantId);

    boolean isFeatureEnabled(UUID tenantId, String featureKey);

    SubscriptionUsageDto getUsage(UUID tenantId);

    void setClock(Clock clock);
}
