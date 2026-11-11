package com.kuter.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class CreateProductUnitConversionDto {

    @NotNull(message = "Secondary unit ID is required")
    private UUID unitId;

    @NotNull(message = "Conversion factor is required (how many base units make 1 of this secondary unit)")
    @DecimalMin(value = "0.0001", message = "Conversion factor must be greater than 0")
    private BigDecimal conversionFactor;

    @Builder.Default
    private boolean isSellingUnit = true;

    @Builder.Default
    private boolean isReceivingUnit = true;

    public void setUnitDefinitionId(UUID unitDefinitionId) {
        if (this.unitId == null) {
            this.unitId = unitDefinitionId;
        }
    }

    public UUID getUnitDefinitionId() {
        return this.unitId;
    }

    public void setToBaseFactor(BigDecimal toBaseFactor) {
        if (this.conversionFactor == null) {
            this.conversionFactor = toBaseFactor;
        }
    }

    public BigDecimal getToBaseFactor() {
        return this.conversionFactor;
    }
}
