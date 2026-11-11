package com.kuter.catalog.dto;

import com.kuter.catalog.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class UpdateProductDto {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    private String name;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    private UUID categoryId;

    @DecimalMin(value = "0.0000", message = "Selling price must be non-negative")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.0000", message = "Reorder threshold must be non-negative")
    private BigDecimal reorderThreshold;

    @Size(max = 1024, message = "Image URL cannot exceed 1024 characters")
    private String imageUrl;

    @Size(max = 255, message = "Image storage key cannot exceed 255 characters")
    private String imageStorageKey;

    @Size(max = 100, message = "Barcode cannot exceed 100 characters")
    private String barcode;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    private ProductStatus status;
}
