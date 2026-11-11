package com.kuter.identity.dto;

import com.kuter.identity.MembershipStatus;
import com.kuter.identity.ShopPermission;
import com.kuter.identity.TenantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Detailed view of an authorized tenant member.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberDetailResponse {

    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String email;
    private String fullName;
    private String phoneNumber;
    private TenantRole role;
    private MembershipStatus status;
    private Set<ShopPermission> permissions;
    private Instant createdAt;
}
