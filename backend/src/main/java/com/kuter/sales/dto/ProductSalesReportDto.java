package com.kuter.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSalesReportDto {
    private UUID tenantId;
    private Instant startDate;
    private Instant endDate;
    private Instant generatedAt;
    private List<ProductSalesMetricDto> products;
}
