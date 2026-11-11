package com.kuter.onboarding.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public inbound registration request from prospective building materials merchant.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOnboardingRequestDto {

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 255, message = "Shop name must be between 2 and 255 characters")
    private String shopName;

    @NotBlank(message = "Owner full name is required")
    @Size(min = 2, max = 255, message = "Owner name must be between 2 and 255 characters")
    private String ownerFullName;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String ownerEmail;

    @Size(max = 50, message = "Phone number cannot exceed 50 characters")
    private String ownerPhone;

    @NotBlank(message = "Requested subscription plan code is required")
    @Size(max = 50, message = "Plan code cannot exceed 50 characters")
    private String requestedPlanCode;

    @Size(max = 50, message = "Preferred shop code cannot exceed 50 characters")
    private String preferredShopCode;

    @Size(max = 255, message = "Business address cannot exceed 255 characters")
    private String businessAddress;

    @Builder.Default
    private String currency = "USD";

    private String applicantNotes;
}
