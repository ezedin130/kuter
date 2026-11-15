package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSubscriptionPlanDto {

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @Builder.Default
    private String currency = "ETB";

    @NotNull(message = "Billing interval is required")
    private BillingInterval billingInterval;

    private Integer productLimit;
    private Integer userLimit;
    private Integer branchLimit;
    private Integer trialDays;
    private Boolean isPublic;
    private Boolean isActive;
    private String featuresSummary;
}
