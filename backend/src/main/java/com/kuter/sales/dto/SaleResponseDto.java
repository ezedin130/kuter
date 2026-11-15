package com.kuter.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kuter.sales.SaleStatus;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaleResponseDto {

    private UUID id;
    private UUID tenantId;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private SaleStatus status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    // Cost and profit data only exposed to roles with profitability permission (null for cashiers)
    private BigDecimal totalCost;
    private BigDecimal grossProfit;

    private Instant soldAt;
    private UUID cashierId;
    private String cashierName;
    private String notes;
    private String idempotencyKey;

    // Void details
    private String voidReason;
    private Instant voidedAt;
    private UUID voidedBy;
    private String voidedByName;

    private List<SaleItemResponseDto> items;
    private Instant createdAt;

    public BigDecimal getTotalRevenue() {
        return totalAmount;
    }
}
