package com.kuter.onboarding.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload submitted by Platform Administrator to approve an onboarding request.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproveOnboardingDto {

    @Size(max = 50, message = "Custom shop code cannot exceed 50 characters")
    private String customShopCode;

    private String reviewerNotes;
}
