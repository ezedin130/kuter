package com.kuter.inventory.dto;

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
public class CreateStockAdjustmentItemDto {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    /**
     * Explicit delta in base units. Can be positive (found/increase) or negative (breakage/loss).
     * If targetQuantity is provided instead, quantityChange will be computed as (targetQuantity - currentOnHand).
     */
    private BigDecimal quantityChange;

    /**
     * Absolute target count from physical stock audit.
     * If provided, quantityChange will be automatically computed.
     */
    private BigDecimal targetQuantity;

    /**
     * Optional unit cost for upward adjustments. If null, current weighted average cost is used.
     */
    private BigDecimal unitCost;
}
