package com.kuter.subscriptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job periodically marking expired subscriptions in the database.
 * Note: Real-time authorization does NOT rely solely on this job; effective status
 * is evaluated dynamically from the trusted currentPeriodEnd timestamp via EntitlementService.
 */
@Component
@ConditionalOnProperty(name = "kuter.subscriptions.expiry-scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SubscriptionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryScheduler.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionExpiryScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "${kuter.subscriptions.expiry-cron:0 0 * * * *}")
    public void runExpiryProcessing() {
        try {
            subscriptionService.processExpirations();
        } catch (Exception e) {
            log.error("Error executing subscription expiry scheduler: {}", e.getMessage(), e);
        }
    }
}
