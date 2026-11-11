package com.kuter.inventory.dto;

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
public class StockAdjustmentItemResponse {

    private UUID id;
    private UUID productId;
    private String productSku;
    private String productName;
    private BigDecimal quantityChange;
    private BigDecimal previousQuantity;
    private BigDecimal newQuantity;
    private BigDecimal unitCost;
    private BigDecimal totalValuationImpact;

    public BigDecimal getBalanceAfter() {
        return this.newQuantity;
    }

    public BigDecimal getQuantityDelta() {
        return this.quantityChange;
    }
}
