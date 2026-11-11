package com.kuter.subscriptions;

/**
 * Lifecycle status of a tenant's subscription to a plan.
 */
public enum SubscriptionStatus {
    PENDING,
    TRIALING,
    ACTIVE,
    EXPIRED,
    SUSPENDED,
    CANCELLED,
    PAST_DUE
}
