package com.kuter.inventory;

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
 * StockAdjustmentItem entity representing an individual material line item being adjusted.
 */
@Entity
@Table(name = "stock_adjustment_items")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentItem extends TenantAwareBaseEntity {

    @Column(name = "adjustment_id", nullable = false)
    private UUID adjustmentId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_change", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityChange;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "adjustment_type", length = 20)
    private String adjustmentType;

    @Column(name = "entered_quantity", precision = 15, scale = 4)
    private BigDecimal enteredQuantity;

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "unit_code", length = 50)
    private String unitCode;

    @Column(name = "conversion_factor", precision = 15, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "quantity_before", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 15, scale = 4)
    private BigDecimal quantityAfter;

    @Column(name = "valuation_impact", nullable = false, precision = 15, scale = 4)
    private BigDecimal valuationImpact;
}
