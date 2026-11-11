package com.kuter.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for direct tenant creation by Platform Administrator.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantDto {

    @NotBlank(message = "Tenant shop name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    @Size(max = 100, message = "Business registration number cannot exceed 100 characters")
    private String businessRegistrationNumber;

    @Builder.Default
    private String currency = "USD";

    @Builder.Default
    private String timezone = "UTC";

    @JsonAlias({"initialOwnerEmail", "owner_email"})
    @NotBlank(message = "Owner email is required")
    @Email(message = "Must be a valid email address")
    private String ownerEmail;

    @JsonAlias({"initialOwnerFullName", "owner_full_name"})
    @NotBlank(message = "Owner full name is required")
    private String ownerFullName;

    @JsonAlias({"initialOwnerPhone", "owner_phone"})
    private String ownerPhone;

    @JsonAlias({"initialOwnerPassword", "owner_password"})
    @Size(min = 6, max = 100, message = "Owner password must be at least 6 characters")
    private String ownerPassword;

    @JsonAlias({"plan_code"})
    private String planCode;
}
