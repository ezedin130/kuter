package com.kuter.catalog.dto;

import com.kuter.catalog.ProductStatus;
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
public class ProductSummaryResponse {

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID baseUnitId;
    private String baseUnitCode;
    private String baseUnitName;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal reorderThreshold;
    private ProductStatus status;
    private String imageUrl;
    private String imageStorageKey;
    private String barcode;
    private String brand;
    private BigDecimal quantityOnHand;
    private BigDecimal averageCost;
    private boolean isLowStock;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isLowStock() {
        return isLowStock;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("currentStock")
    public BigDecimal getCurrentStock() {
        return quantityOnHand != null ? quantityOnHand : BigDecimal.ZERO;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("lowStock")
    public boolean getLowStock() {
        return isLowStock;
    }
}
