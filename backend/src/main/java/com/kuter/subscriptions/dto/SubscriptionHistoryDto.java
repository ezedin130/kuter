package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.SubscriptionAction;
import com.kuter.subscriptions.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionHistoryDto {
    private UUID id;
    private UUID subscriptionId;
    private UUID tenantId;
    private SubscriptionAction action;
    private SubscriptionStatus previousStatus;
    private SubscriptionStatus newStatus;
    private UUID previousPlanId;
    private String previousPlanName;
    private UUID newPlanId;
    private String newPlanName;
    private Instant previousPeriodEnd;
    private Instant newPeriodEnd;
    private String reason;
    private UUID actorId;
    private String actorName;
    private String actorEmail;
    private Instant createdAt;
}
