package com.kuter.sales;

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
import java.time.Instant;
import java.util.UUID;

/**
 * Sale entity representing a retail counter transaction at a building materials yard.
 */
@Entity
@Table(name = "sales")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Sale extends TenantAwareBaseEntity {

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SaleStatus status = SaleStatus.COMPLETED;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "total_cost", nullable = false, precision = 15, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "sold_at", nullable = false)
    private Instant soldAt;

    @Column(name = "cashier_id")
    private UUID cashierId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;
}
