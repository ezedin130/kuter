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

import java.util.UUID;

/**
 * StockAdjustment entity representing an audit-controlled quantity adjustment
 * (e.g. Breakage, Spillage, Bag Burst, Expiry, Theft, Physical Count Correction).
 */
@Entity
@Table(name = "stock_adjustments")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustment extends TenantAwareBaseEntity {

    @Column(name = "adjustment_number", nullable = false, length = 100)
    private String adjustmentNumber;

    @Column(name = "reason_code", nullable = false, length = 50)
    private String reasonCode;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "created_by")
    private UUID createdBy;
}
