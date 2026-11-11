package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.BillingInterval;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionPlanDto {

    @NotBlank(message = "Plan code is required")
    @Pattern(regexp = "^[a-z0-9_-]{2,50}$", message = "Plan code must be 2-50 characters lowercase alphanumeric or dashes")
    private String code;

    @NotBlank(message = "Plan name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;

    @Builder.Default
    private String currency = "USD";

    @NotNull(message = "Billing interval is required")
    private BillingInterval billingInterval;

    private Integer productLimit;
    private Integer userLimit;
    private Integer branchLimit;

    @Builder.Default
    private Integer trialDays = 14;

    @Builder.Default
    private Boolean isPublic = true;

    @Builder.Default
    private Boolean isActive = true;

    private String featuresSummary;
}
