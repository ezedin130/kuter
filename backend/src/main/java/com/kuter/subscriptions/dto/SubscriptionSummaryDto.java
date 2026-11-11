package com.kuter.subscriptions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionSummaryDto {
    private long totalSubscriptions;
    private long activeCount;
    private long trialingCount;
    private long expiredCount;
    private long suspendedCount;
    private long cancelledCount;
    private BigDecimal estimatedMonthlyRevenue;
}
