package com.kuter.identity.dto;

import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO representing user membership in a specific tenant shop.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMembershipDto {

    private UUID id;
    private UUID tenantId;
    private String tenantCode;
    private String tenantName;
    private TenantRole role;
    private MembershipStatus status;
}
