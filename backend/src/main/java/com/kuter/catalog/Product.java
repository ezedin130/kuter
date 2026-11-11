package com.kuter.catalog;

import com.kuter.common.TenantAwareBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Product entity representing a building material SKU.
 * Uses a base unit for normalized inventory tracking and allows multiple selling/receiving units.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends TenantAwareBaseEntity {

    @Column(name = "sku", nullable = false, length = 100)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "base_unit_id", nullable = false)
    private UUID baseUnitId;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "reorder_threshold", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal reorderThreshold = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "image_storage_key", length = 255)
    private String imageStorageKey;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Column(name = "brand", length = 100)
    private String brand;
}
