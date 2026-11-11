package com.kuter.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSalesMetricDto {
    private UUID productId;
    private String productSku;
    private String productName;
    private String baseUnitCode;
    private BigDecimal totalQuantitySold;
    private BigDecimal totalRevenue;
    private long transactionCount;

    // Costing data only for authorized roles (null for cashiers)
    private BigDecimal totalCost;
    private BigDecimal grossProfit;
}
