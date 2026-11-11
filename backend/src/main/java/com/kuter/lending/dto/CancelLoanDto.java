package com.kuter.lending.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for cancelling an active unreturned loan/borrowing transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelLoanDto {

    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
