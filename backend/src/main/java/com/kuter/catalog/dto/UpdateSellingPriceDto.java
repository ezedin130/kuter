package com.kuter.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for updating a product's authoritative selling price.
 * Strictly restricted to authenticated shop owners.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSellingPriceDto {

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.0001", message = "Selling price must be strictly positive")
    private BigDecimal sellingPrice;
}
