package com.kuter.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductDto {

    @NotBlank(message = "Product SKU is required")
    @Size(min = 1, max = 100, message = "SKU must be between 1 and 100 characters")
    private String sku;

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private UUID categoryId;

    @NotNull(message = "Base inventory unit ID is required")
    private UUID baseUnitId;

    @DecimalMin(value = "0.0000", message = "Cost price must be non-negative")
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0000", message = "Selling price must be non-negative")
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @DecimalMin(value = "0.0000", message = "Reorder threshold must be non-negative")
    @Builder.Default
    private BigDecimal reorderThreshold = BigDecimal.ZERO;

    @Size(max = 1024, message = "Image URL cannot exceed 1024 characters")
    private String imageUrl;

    @Size(max = 255, message = "Image storage key cannot exceed 255 characters")
    private String imageStorageKey;

    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @DecimalMin(value = "0.0000", message = "Initial quantity must be non-negative")
    private BigDecimal initialQuantity;

    @Valid
    private List<CreateProductUnitConversionDto> conversions;
}
