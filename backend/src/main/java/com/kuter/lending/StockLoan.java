package com.kuter.lending;

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
import java.time.Instant;
import java.util.UUID;

/**
 * StockLoan entity representing an incoming (BORROW) or outgoing (LOAN) inventory transaction
 * with an external counterparty shop.
 */
@Entity
@Table(name = "stock_loans")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLoan extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "loan_reference", nullable = false, unique = true, length = 64, updatable = false)
    private String loanReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20, updatable = false)
    private LoanDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private LoanStatus status = LoanStatus.ACTIVE;

    // External Counterparty information
    @Column(name = "counterparty_name", nullable = false)
    private String counterpartyName;

    @Column(name = "counterparty_contact_person")
    private String counterpartyContactPerson;

    @Column(name = "counterparty_phone", length = 50)
    private String counterpartyPhone;

    @Column(name = "counterparty_address", columnDefinition = "TEXT")
    private String counterpartyAddress;

    // Local Product Snapshot
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "product_sku", nullable = false, length = 100, updatable = false)
    private String productSku;

    @Column(name = "product_name", nullable = false, updatable = false)
    private String productName;

    @Column(name = "unit_id", nullable = false, updatable = false)
    private UUID unitId;

    @Column(name = "unit_code", nullable = false, length = 50, updatable = false)
    private String unitCode;

    @Column(name = "conversion_factor", nullable = false, precision = 15, scale = 4, updatable = false)
    @Builder.Default
    private BigDecimal conversionFactor = BigDecimal.ONE;

    // Quantities (entered unit and canonical base unit)
    @Column(name = "initial_quantity", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal initialQuantity;

    @Column(name = "initial_base_quantity", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal initialBaseQuantity;

    @Column(name = "returned_quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal returnedQuantity = BigDecimal.ZERO;

    @Column(name = "returned_base_quantity", nullable = false, precision = 15, scale = 4)
    @Builder.Default
    private BigDecimal returnedBaseQuantity = BigDecimal.ZERO;

    @Column(name = "outstanding_base_quantity", nullable = false, precision = 15, scale = 4)
    private BigDecimal outstandingBaseQuantity;

    // Valuation Cost Snapshot
    @Column(name = "unit_cost_snapshot", nullable = false, precision = 15, scale = 4, updatable = false)
    @Builder.Default
    private BigDecimal unitCostSnapshot = BigDecimal.ZERO;

    // Dates & Notes
    @Column(name = "transaction_date", nullable = false, updatable = false)
    private Instant transactionDate;

    @Column(name = "expected_return_date")
    private Instant expectedReturnDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // Auditing
    @Column(name = "created_by_user_id", updatable = false)
    private UUID createdByUserId;
}
