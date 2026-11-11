package com.kuter.subscriptions;

import com.kuter.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit ledger tracking every administrative action and state change in a subscription's lifecycle.
 */
@Entity
@Table(name = "subscription_history")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionHistory extends BaseEntity {

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private SubscriptionAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private SubscriptionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private SubscriptionStatus newStatus;

    @Column(name = "previous_plan_id")
    private UUID previousPlanId;

    @Column(name = "new_plan_id")
    private UUID newPlanId;

    @Column(name = "previous_period_end")
    private Instant previousPeriodEnd;

    @Column(name = "new_period_end")
    private Instant newPeriodEnd;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "actor_id")
    private UUID actorId;
}
