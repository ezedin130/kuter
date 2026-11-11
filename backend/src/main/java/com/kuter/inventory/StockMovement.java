package com.kuter.inventory;

import com.kuter.common.BaseEntity;
import com.kuter.common.TenantAware;
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
 * StockMovement entity representing an immutable ledger record of any quantity change.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50, updatable = false)
    private MovementType movementType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "reference_type", length = 50, updatable = false)
    private String referenceType;

    @Column(name = "quantity_change", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal quantityChange;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal unitCost;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;
}
