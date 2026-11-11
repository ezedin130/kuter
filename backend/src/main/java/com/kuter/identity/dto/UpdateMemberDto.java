package com.kuter.identity.dto;

import com.kuter.identity.MembershipStatus;
import com.kuter.identity.ShopPermission;
import com.kuter.identity.TenantRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Payload for updating a tenant member's role or operational status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberDto {

    @NotNull(message = "Role is required")
    private TenantRole role;

    private MembershipStatus status;

    private Set<ShopPermission> permissions;
}
