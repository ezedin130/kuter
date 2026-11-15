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

    private String adjustmentType;
    private BigDecimal enteredQuantity;
    private UUID unitId;
    private String unitCode;
    private BigDecimal conversionFactor;
    private String baseUnitCode;

    public BigDecimal getBalanceAfter() {
        return this.newQuantity;
    }

    public BigDecimal getQuantityDelta() {
        return this.quantityChange;
    }

    public BigDecimal getQuantityBefore() {
        return this.previousQuantity != null ? this.previousQuantity : BigDecimal.ZERO;
    }

    public BigDecimal getQuantityAfter() {
        return this.newQuantity != null ? this.newQuantity : BigDecimal.ZERO;
    }

    public BigDecimal getFinancialValue() {
        return this.totalValuationImpact != null ? this.totalValuationImpact : BigDecimal.ZERO;
    }
}
