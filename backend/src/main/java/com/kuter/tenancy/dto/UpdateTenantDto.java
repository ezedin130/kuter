package com.kuter.tenancy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for updating tenant profile details.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantDto {

    @NotBlank(message = "Shop name cannot be blank")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Size(max = 100, message = "Business registration number cannot exceed 100 characters")
    private String businessRegistrationNumber;

    @Size(max = 10, message = "Currency cannot exceed 10 characters")
    private String currency;

    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
}
