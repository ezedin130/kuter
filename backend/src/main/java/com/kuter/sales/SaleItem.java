package com.kuter.sales;

import com.kuter.common.TenantAwareBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * SaleItem entity representing a material line item on a counter sale invoice.
 */
@Entity
@Table(name = "sale_items")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItem extends TenantAwareBaseEntity {

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "product_sku", length = 100)
    private String productSku;

    @Column(name = "sold_unit_id", nullable = false)
    private UUID soldUnitId;

    @Column(name = "sold_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal soldQuantity;

    @Column(name = "conversion_factor", nullable = false, precision = 15, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "base_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal baseQuantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitPrice;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_price", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalPrice;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalCost;
}
