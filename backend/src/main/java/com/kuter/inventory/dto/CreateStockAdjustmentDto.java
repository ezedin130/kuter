package com.kuter.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentDto {

    @NotBlank(message = "Adjustment reason code is required (e.g. STOCK_COUNT, DAMAGED, LOST, SPILLAGE, CORRECTION)")
    @Size(min = 2, max = 50, message = "Reason code must be between 2 and 50 characters")
    private String reasonCode;

    @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
    private String notes;

    @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
    private String idempotencyKey;

    @NotEmpty(message = "At least one item must be adjusted")
    @Valid
    private List<CreateStockAdjustmentItemDto> items;
}
