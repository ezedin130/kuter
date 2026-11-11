package com.kuter.sales.dto;

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
public class CreateSaleItemRequestDto {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Selling unit ID is required")
    private UUID unitDefinitionId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be strictly positive")
    private BigDecimal quantity;

    /**
     * Optional custom unit selling price in the selected selling unit.
     * If omitted or null, the server calculates: baseSellingPrice * conversionFactor.
     */
    @DecimalMin(value = "0.0000", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    /**
     * Optional line-level discount amount.
     */
    @DecimalMin(value = "0.0000", message = "Discount amount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
}
