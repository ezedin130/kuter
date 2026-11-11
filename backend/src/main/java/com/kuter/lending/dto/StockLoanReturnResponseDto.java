package com.kuter.lending.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLoanReturnResponseDto {

    private UUID id;
    private UUID loanId;
    private String returnReference;
    private BigDecimal returnQuantity;
    private BigDecimal returnBaseQuantity;
    private UUID unitId;
    private String unitCode;
    private Instant returnDate;
    private String notes;
    private UUID receivedByUserId;
    private Instant createdAt;
}
