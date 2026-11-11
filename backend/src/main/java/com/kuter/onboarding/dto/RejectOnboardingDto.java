package com.kuter.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload submitted by Platform Administrator to reject an onboarding request.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectOnboardingDto {

    @NotBlank(message = "Rejection reason is required")
    private String reason;
}
