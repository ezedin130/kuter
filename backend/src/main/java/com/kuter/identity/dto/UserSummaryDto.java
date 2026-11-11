package com.kuter.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kuter.identity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Summary DTO of a platform user without exposing password or internal credentials.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    private UUID id;
    private String email;
    private String fullName;
    private String phoneNumber;

    @JsonProperty("isPlatformAdmin")
    private boolean isPlatformAdmin;

    private UserStatus status;
}
