package com.kuter.tenancy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregated platform metrics overview for platform administrators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewMetricsDto {

    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long pendingOnboardingRequests;

    private long totalSubscriptions;
    private long activeSubscriptions;
    private long trialingSubscriptions;
    private long expiredSubscriptions;
    private long suspendedSubscriptions;
    private long cancelledSubscriptions;

    private BigDecimal estimatedMonthlyRevenue;
    private String currency;
    private Instant generatedAt;
}
