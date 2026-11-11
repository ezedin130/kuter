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
 * StockReceiving entity representing an inbound supplier shipment delivery.
 */
@Entity
@Table(name = "stock_receivings")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceiving extends TenantAwareBaseEntity {

    @Column(name = "receiving_number", nullable = false, length = 100)
    private String receivingNumber;

    @Column(name = "supplier_name", nullable = false, length = 255)
    private String supplierName;

    @Column(name = "delivery_note_ref", length = 100)
    private String deliveryNoteRef;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "received_by")
    private UUID receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
