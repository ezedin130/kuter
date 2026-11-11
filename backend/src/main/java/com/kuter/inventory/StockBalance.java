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
import java.time.Instant;
import java.util.UUID;

/**
 * StockBalance entity representing the current on-hand quantity and moving average cost
 * for a product within a tenant yard.
 */
@Entity
@Table(name = "stock_balances")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockBalance extends TenantAwareBaseEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_on_hand", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(name = "average_cost", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(name = "last_counted_at")
    private Instant lastCountedAt;
}
