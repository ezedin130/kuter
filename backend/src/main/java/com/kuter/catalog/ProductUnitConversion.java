package com.kuter.catalog;

import com.kuter.common.BaseEntity;
import com.kuter.common.TenantAware;
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
 * ProductUnitConversion defines the multiplication factor from a secondary unit
 * (e.g. Tonne, Pallet, Bundle) to the product's base unit (e.g. Bag, Piece).
 *
 * Formula: base_quantity = secondary_quantity * conversion_factor
 */
@Entity
@Table(name = "product_unit_conversions")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUnitConversion extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "conversion_factor", nullable = false, precision = 15, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "is_selling_unit", nullable = false)
    @Builder.Default
    private boolean isSellingUnit = true;

    @Column(name = "is_receiving_unit", nullable = false)
    @Builder.Default
    private boolean isReceivingUnit = true;
}
