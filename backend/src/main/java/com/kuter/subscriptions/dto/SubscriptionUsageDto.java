package com.kuter.subscriptions.dto;

import com.kuter.subscriptions.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionUsageDto {
    private UUID tenantId;
    private String planCode;
    private String planName;
    private SubscriptionStatus status;
    private SubscriptionStatus effectiveStatus;
    private long activeProductsCount;
    private Integer productLimit;
    private boolean productLimitReached;
    private long activeUsersCount;
    private Integer userLimit;
    private boolean userLimitReached;
}
