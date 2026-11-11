package com.kuter.tenancy.dto;

import com.kuter.tenancy.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed tenant representation for platform administrators and shop settings.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDetailResponse {

    private UUID id;
    private String code;
    private String name;
    private String businessRegistrationNumber;
    private TenantStatus status;
    private String currency;
    private String timezone;
    private int memberCount;
    private Instant createdAt;
    private Instant updatedAt;
}
