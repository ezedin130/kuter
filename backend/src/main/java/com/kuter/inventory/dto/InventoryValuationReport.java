package com.kuter.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryValuationReport {

    private long totalSkus;
    private long inStockSkus;
    private long lowStockSkus;
    private long outOfStockSkus;
    private BigDecimal totalValuation;
    private String currency;
    @Builder.Default
    private String valuationType = "CURRENT";
    @Builder.Default
    private String calculationBasis = "Moving weighted-average unit cost (quantityOnHand * averageCost)";
    private List<CategoryValuationSummary> categoryBreakdowns;
    private Instant generatedAt;
}
