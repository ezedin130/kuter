package com.kuter.catalog.dto;

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
public class ProductUnitConversionResponse {

    private UUID id;
    private UUID unitId;
    private String unitCode;
    private String unitName;
    private BigDecimal conversionFactor;
    private boolean isBaseUnit;
    private boolean isSellingUnit;
    private boolean isReceivingUnit;

    public BigDecimal getToBaseFactor() {
        return this.conversionFactor;
    }

    public UUID getUnitDefinitionId() {
        return this.unitId;
    }
}
