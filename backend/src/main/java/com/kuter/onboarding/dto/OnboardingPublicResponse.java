package com.kuter.onboarding.dto;

import com.kuter.onboarding.OnboardingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Public response returned after submitting or querying status of an onboarding request.
 * Contains no internal reviewer notes, administrator identities, or sensitive internal IDs.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingPublicResponse {

    private String referenceCode;
    private String shopName;
    private String ownerEmail;
    private String requestedPlanCode;
    private OnboardingStatus status;
    private Instant createdAt;
}
