package com.kuter.subscriptions.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendSubscriptionDto {

    private Integer additionalDays;
    private Instant newPeriodEnd;

    @NotBlank(message = "Reason for extension is mandatory")
    private String reason;
}
