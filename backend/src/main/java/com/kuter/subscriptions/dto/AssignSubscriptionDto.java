package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignSubscriptionDto {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotNull(message = "Plan ID is required")
    private UUID planId;

    private SubscriptionStatus status; // defaults to ACTIVE if null
    private Integer periodDays; // defaults to 30 if null
    private Integer trialDays; // optional trial duration
    private String notes;
}
