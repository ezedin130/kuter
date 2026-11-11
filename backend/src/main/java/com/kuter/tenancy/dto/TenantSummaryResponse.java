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
 * Summary representation of a tenant for paginated directory listings.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSummaryResponse {

    private UUID id;
    private String code;
    private String name;
    private TenantStatus status;
    private String currency;
    private Instant createdAt;
}
