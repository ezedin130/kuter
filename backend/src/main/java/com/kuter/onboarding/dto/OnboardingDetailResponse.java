package com.kuter.onboarding.dto;

import com.kuter.onboarding.OnboardingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed representation of an onboarding request for Platform Administration review.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDetailResponse {

    private UUID id;
    private String referenceCode;
    private String shopName;
    private String ownerFullName;
    private String ownerEmail;
    private String ownerPhone;
    private String requestedPlanCode;
    private String preferredShopCode;
    private String businessAddress;
    private String currency;
    private String applicantNotes;
    private OnboardingStatus status;
    private UUID reviewerId;
    private String reviewerNotes;
    private Instant reviewedAt;
    private UUID createdTenantId;
    private Instant createdAt;
}
