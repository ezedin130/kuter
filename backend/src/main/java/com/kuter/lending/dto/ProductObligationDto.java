package com.kuter.lending.dto;

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
public class ProductObligationDto {

    private UUID productId;
    private String productSku;
    private String productName;
    private String baseUnitCode;
    private BigDecimal totalBorrowedBaseQuantity;
    private BigDecimal totalLoanedBaseQuantity;
    private BigDecimal netObligationBaseQuantity; // positive = net borrowed, negative = net loaned out
}
