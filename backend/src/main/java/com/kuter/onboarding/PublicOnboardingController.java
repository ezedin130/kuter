package com.kuter.onboarding;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.onboarding.dto.CreateOnboardingRequestDto;
import com.kuter.onboarding.dto.OnboardingPublicResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public REST Controller for prospective building materials merchants to submit
 * shop registration requests and check application status.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/onboarding-requests")
public class PublicOnboardingController {

    private final OnboardingService onboardingService;

    public PublicOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingPublicResponse>> submitRequest(
            @Valid @RequestBody CreateOnboardingRequestDto dto,
            HttpServletRequest httpRequest) {
        OnboardingPublicResponse response = onboardingService.submitRequest(dto, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Registration request submitted successfully"));
    }

    @GetMapping("/status/{referenceCode}")
    public ResponseEntity<ApiResponse<OnboardingPublicResponse>> getStatus(
            @PathVariable String referenceCode) {
        OnboardingPublicResponse response = onboardingService.getPublicStatus(referenceCode);
        return ResponseEntity.ok(ApiResponse.success(response, "Onboarding status retrieved successfully"));
    }
}
