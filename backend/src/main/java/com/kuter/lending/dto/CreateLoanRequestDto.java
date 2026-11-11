package com.kuter.lending.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payload for recording an outgoing inventory loan to an external shop.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRequestDto {

    @NotBlank(message = "Borrowing counterparty shop name is required")
    private String counterpartyName;

    private String counterpartyContactPerson;
    private String counterpartyPhone;
    private String counterpartyAddress;

    @NotNull(message = "Local product ID is required")
    private UUID productId;

    @NotNull(message = "Unit definition ID is required")
    private UUID unitDefinitionId;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be strictly positive")
    private BigDecimal quantity;

    private Instant transactionDate;
    private Instant expectedReturnDate;
    private String notes;
}
