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
public class StockReceivingItemResponse {

    private UUID id;
    private UUID productId;
    private String productSku;
    private String productName;
    private UUID receivedUnitId;
    private String receivedUnitCode;
    private BigDecimal receivedQuantity;
    private BigDecimal conversionFactor;
    private BigDecimal baseQuantity;
    private String baseUnitCode;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}
