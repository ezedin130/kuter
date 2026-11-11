package com.kuter.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload for recording a partial or complete return of loaned/borrowed stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordLoanReturnDto {

    @NotNull(message = "Return quantity is required")
    @DecimalMin(value = "0.0001", message = "Return quantity must be strictly positive")
    private BigDecimal quantity;

    private UUID unitDefinitionId; // Optional: defaults to the loan's transaction unit
    private Instant returnDate;
    private String notes;
}
