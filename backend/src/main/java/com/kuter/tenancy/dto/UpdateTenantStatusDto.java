package com.kuter.tenancy.dto;

import com.kuter.tenancy.TenantStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload for activating or suspending a tenant shop.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantStatusDto {

    @NotNull(message = "Status is required")
    private TenantStatus status;

    private String reason;
}
