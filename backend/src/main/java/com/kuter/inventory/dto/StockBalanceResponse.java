package com.kuter.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceResponse {

    private UUID productId;
    private String productSku;
    private String productName;
    private UUID categoryId;
    private String categoryName;
    private UUID baseUnitId;
    private String baseUnitCode;
    private String baseUnitName;
    private BigDecimal quantityOnHand;
    private BigDecimal averageCost;
    private BigDecimal totalValuation;
    private BigDecimal reorderThreshold;
    private boolean isLowStock;
    private Instant lastCountedAt;
    private Instant updatedAt;

    public BigDecimal getCurrentBalance() {
        return this.quantityOnHand;
    }

    public BigDecimal getMovingAverageCost() {
        return this.averageCost;
    }
}
