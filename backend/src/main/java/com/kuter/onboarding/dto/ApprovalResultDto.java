package com.kuter.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Result returned upon successful approval and provisioning of a tenant shop.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalResultDto {

    private UUID requestId;
    private String referenceCode;
    private UUID tenantId;
    private String tenantCode;
    private String tenantName;
    private String ownerEmail;
    private String status;
    private String subscriptionPlanCode;
}
