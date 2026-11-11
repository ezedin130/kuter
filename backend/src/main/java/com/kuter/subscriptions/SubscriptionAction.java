package com.kuter.subscriptions;

/**
 * Enumeration of administrative actions in the subscription lifecycle.
 */
public enum SubscriptionAction {
    ASSIGNED,
    ACTIVATED,
    TRIAL_STARTED,
    PLAN_CHANGED,
    EXTENDED,
    SUSPENDED,
    REACTIVATED,
    CANCELLED,
    EXPIRED
}
