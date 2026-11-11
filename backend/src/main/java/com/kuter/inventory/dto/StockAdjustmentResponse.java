package com.kuter.inventory.dto;

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
public class StockAdjustmentResponse {

    private UUID id;
    private String adjustmentNumber;
    private String reasonCode;
    private String notes;
    private String status;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
    private List<StockAdjustmentItemResponse> items;
}
