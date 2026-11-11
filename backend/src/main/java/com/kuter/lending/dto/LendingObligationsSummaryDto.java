package com.kuter.lending.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LendingObligationsSummaryDto {

    private long totalActiveBorrowings;
    private long totalActiveLoans;
    private BigDecimal totalOutstandingBorrowedBaseUnits;
    private BigDecimal totalOutstandingLoanedBaseUnits;
    private List<ProductObligationDto> productObligations;
}
