package com.kuter.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequestDto {

    @Size(max = 255, message = "Customer name cannot exceed 255 characters")
    private String customerName;

    @Size(max = 50, message = "Customer phone cannot exceed 50 characters")
    private String customerPhone;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @DecimalMin(value = "0.0000", message = "Discount amount cannot be negative")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Size(max = 100, message = "Idempotency key cannot exceed 100 characters")
    private String idempotencyKey;

    @NotEmpty(message = "Sale must contain at least one line item")
    @Valid
    @Builder.Default
    private List<CreateSaleItemRequestDto> items = new ArrayList<>();
}
