package com.kuter.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceivingResponse {

    private UUID id;
    private String receivingNumber;
    private String supplierName;
    private String deliveryNoteRef;
    private String status;
    private BigDecimal totalCost;
    private String notes;
    private UUID receivedBy;
    private String receivedByName;
    private Instant receivedAt;
    private Instant createdAt;
    private List<StockReceivingItemResponse> items;
}
