package com.kuter.subscriptions.dto;

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
public class ChangePlanDto {

    @NotNull(message = "New plan ID is required")
    private UUID newPlanId;

    @Builder.Default
    private Boolean immediate = true;

    private String notes;
}
