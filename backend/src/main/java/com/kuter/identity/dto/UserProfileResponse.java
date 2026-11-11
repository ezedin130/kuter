package com.kuter.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuter.identity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Detailed profile response for current authenticated user.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;

    @JsonProperty("isPlatformAdmin")
    private boolean isPlatformAdmin;

    private UserStatus status;
    private List<TenantMembershipDto> memberships;
    private UUID activeTenantId;
}
