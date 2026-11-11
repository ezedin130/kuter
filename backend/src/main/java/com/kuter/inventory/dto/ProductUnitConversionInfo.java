package com.kuter.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representation of a product-specific secondary unit and converted equivalent quantity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitConversionInfo {

    private UUID unitId;
    private String unitCode;
    private String unitName;
    private BigDecimal conversionFactor;
    private BigDecimal equivalentQuantity;
    private boolean isSellingUnit;
    private boolean isReceivingUnit;
}
