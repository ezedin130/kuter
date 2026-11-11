package com.kuter.onboarding;

import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.onboarding.dto.ApprovalResultDto;
import com.kuter.onboarding.dto.ApproveOnboardingDto;
import com.kuter.onboarding.dto.CreateOnboardingRequestDto;
import com.kuter.onboarding.dto.OnboardingDetailResponse;
import com.kuter.onboarding.dto.OnboardingPublicResponse;
import com.kuter.onboarding.dto.RejectOnboardingDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for shop registration requests, status querying, and administrative reviews.
 */
public interface OnboardingService {

    OnboardingPublicResponse submitRequest(CreateOnboardingRequestDto dto, HttpServletRequest httpRequest);

    OnboardingPublicResponse getPublicStatus(String referenceCode);

    PageResponse<OnboardingDetailResponse> listRequests(OnboardingStatus status, String search, Pageable pageable);

    OnboardingDetailResponse getRequestById(UUID id);

    ApprovalResultDto approveRequest(UUID id, ApproveOnboardingDto dto, KuterUserPrincipal adminPrincipal, HttpServletRequest httpRequest);

    OnboardingDetailResponse rejectRequest(UUID id, RejectOnboardingDto dto, KuterUserPrincipal adminPrincipal, HttpServletRequest httpRequest);
}
