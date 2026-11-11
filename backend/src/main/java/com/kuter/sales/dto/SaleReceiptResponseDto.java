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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaleReceiptResponseDto {

    private String shopName;
    private String shopPhone;
    private String shopEmail;
    private String shopAddress;
    private String shopCity;
    private String shopCountry;

    private String invoiceNumber;
    private Instant soldAt;
    private String cashierName;
    private String customerName;
    private String customerPhone;

    private List<SaleReceiptItemDto> items;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    private String notes;
    private SaleStatus status;
    private String voidReason;
    private Instant voidedAt;

    @Builder.Default
    private String disclaimer = "Operational Sales Record - Not a Proof of Payment";

    public String getOperationalDisclaimer() {
        return this.disclaimer;
    }
}
