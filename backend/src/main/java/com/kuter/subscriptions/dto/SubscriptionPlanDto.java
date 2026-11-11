package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.BillingInterval;
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
public class SubscriptionPlanDto {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String currency;
    private BillingInterval billingInterval;
    private Integer productLimit;
    private Integer userLimit;
    private Integer branchLimit;
    private Integer trialDays;
    private boolean isPublic;
    private boolean isActive;
    private String featuresSummary;
    private Instant createdAt;
    private Instant updatedAt;
}
