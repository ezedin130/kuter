package com.kuter.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailySalesSummaryDto {
    private LocalDate date;
    private long salesCount;
    private BigDecimal salesAmount;
    private BigDecimal costOfGoodsSold;
    private BigDecimal grossProfit;
}
