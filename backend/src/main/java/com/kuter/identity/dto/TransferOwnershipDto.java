package com.kuter.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Payload for transferring shop ownership to another authorized active member.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOwnershipDto {

    @NotNull(message = "New owner user ID is required")
    private UUID newOwnerUserId;

    @NotBlank(message = "Current owner password confirmation is required")
    private String currentOwnerPassword;
}
