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
 * StockReceivingItem entity representing an individual material line item on a receiving record.
 */
@Entity
@Table(name = "stock_receiving_items")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceivingItem extends TenantAwareBaseEntity {

    @Column(name = "receiving_id", nullable = false)
    private UUID receivingId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "received_unit_id", nullable = false)
    private UUID receivedUnitId;

    @Column(name = "received_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal receivedQuantity;

    @Column(name = "conversion_factor", nullable = false, precision = 15, scale = 4)
    private BigDecimal conversionFactor;

    @Column(name = "base_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal baseQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalCost;
}
