package com.kuter.inventory.dto;

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
public class CreateStockReceivingItemDto {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Received unit ID is required")
    private UUID receivedUnitId;

    @NotNull(message = "Received quantity is required")
    @DecimalMin(value = "0.0001", message = "Received quantity must be greater than zero")
    private BigDecimal receivedQuantity;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.0000", message = "Unit cost must be non-negative")
    private BigDecimal unitCost;
}
