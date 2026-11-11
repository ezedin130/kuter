package com.kuter.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed item for current inventory reporting including dual-UOM conversions and stock valuation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentInventoryItemDto {

    private UUID productId;
    private String productSku;
    private String productName;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal quantityOnHand;
    private UUID baseUnitId;
    private String baseUnitCode;
    private String baseUnitName;
    private BigDecimal averageCost;
    private BigDecimal totalValuation;
    private BigDecimal reorderThreshold;
    private String stockStatus; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    private List<ProductUnitConversionInfo> conversions;
    private Instant lastCountedAt;
    private Instant updatedAt;
}
