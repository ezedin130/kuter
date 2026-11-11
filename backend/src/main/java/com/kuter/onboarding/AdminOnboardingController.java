package com.kuter.onboarding;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.onboarding.dto.ApprovalResultDto;
import com.kuter.onboarding.dto.ApproveOnboardingDto;
import com.kuter.onboarding.dto.OnboardingDetailResponse;
import com.kuter.onboarding.dto.RejectOnboardingDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform Administration Controller for reviewing, approving, and rejecting
 * tenant shop onboarding requests.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/admin/onboarding-requests")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminOnboardingController {

    private final OnboardingService onboardingService;

    public AdminOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OnboardingDetailResponse>>> listRequests(
            @RequestParam(required = false) OnboardingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<OnboardingDetailResponse> response = onboardingService.listRequests(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding requests retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OnboardingDetailResponse>> getRequestById(@PathVariable UUID id) {
        OnboardingDetailResponse response = onboardingService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding request details retrieved successfully"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ApprovalResultDto>> approveRequest(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveOnboardingDto dto,
            @AuthenticationPrincipal KuterUserPrincipal adminPrincipal,
            HttpServletRequest httpRequest) {
        ApprovalResultDto result = onboardingService.approveRequest(id, dto, adminPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result, "Onboarding request approved and tenant provisioned successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<OnboardingDetailResponse>> rejectRequest(
            @PathVariable UUID id,
            @Valid @RequestBody RejectOnboardingDto dto,
            @AuthenticationPrincipal KuterUserPrincipal adminPrincipal,
            HttpServletRequest httpRequest) {
        OnboardingDetailResponse result = onboardingService.rejectRequest(id, dto, adminPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(result, "Onboarding request rejected successfully"));
    }
}
