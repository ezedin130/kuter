package com.kuter.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SalesSummaryReportDto {

    private UUID tenantId;
    private Instant startDate;
    private Instant endDate;
    private Instant generatedAt;

    private long totalSalesCount;
    private BigDecimal totalSalesAmount;

    // Financial performance metrics (only for OWNER/MANAGER)
    private BigDecimal totalCostOfGoodsSold;
    private BigDecimal grossProfit;
    private BigDecimal grossMarginPercentage;

    // Void summaries
    private long voidedSalesCount;
    private BigDecimal voidedSalesAmount;

    private List<DailySalesSummaryDto> dailyBreakdowns;
}
