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
public class SaleItemResponseDto {

    private UUID id;
    private UUID productId;
    private String productName;
    private String productSku;
    private UUID soldUnitId;
    private String soldUnitCode;
    private BigDecimal soldQuantity;
    private BigDecimal conversionFactor;
    private BigDecimal baseQuantity;
    private BigDecimal unitPrice;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    // Cost details only exposed to authorized managerial roles (null for cashiers)
    private BigDecimal unitCost;
    private BigDecimal totalCost;
    private BigDecimal grossProfit;

    public String getUnitCode() {
        return soldUnitCode;
    }

    public BigDecimal getQuantity() {
        return soldQuantity;
    }

    public BigDecimal getToBaseFactor() {
        return conversionFactor;
    }

    public BigDecimal getLineTotal() {
        return totalPrice;
    }

    public BigDecimal getCostPriceSnapshot() {
        return unitCost;
    }
}
