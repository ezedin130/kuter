package com.kuter.inventory.dto;

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
public class CategoryValuationSummary {

    private UUID categoryId;
    private String categoryName;
    private long productCount;
    private BigDecimal totalQuantity;
    private BigDecimal totalValuation;
}
