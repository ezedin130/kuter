package com.kuter.lending;

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
import java.time.Instant;
import java.util.UUID;

/**
 * StockLoanReturn entity representing an individual partial or complete return event
 * for a StockLoan record.
 */
@Entity
@Table(name = "stock_loan_returns")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLoanReturn extends BaseEntity implements TenantAware {

    @Column(name = "loan_id", nullable = false, updatable = false)
    private UUID loanId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "return_reference", nullable = false, length = 64, updatable = false)
    private String returnReference;

    @Column(name = "return_quantity", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal returnQuantity;

    @Column(name = "return_base_quantity", nullable = false, precision = 15, scale = 4, updatable = false)
    private BigDecimal returnBaseQuantity;

    @Column(name = "unit_id", nullable = false, updatable = false)
    private UUID unitId;

    @Column(name = "unit_code", nullable = false, length = 50, updatable = false)
    private String unitCode;

    @Column(name = "return_date", nullable = false, updatable = false)
    private Instant returnDate;

    @Column(name = "notes", columnDefinition = "TEXT", updatable = false)
    private String notes;

    @Column(name = "received_by_user_id", updatable = false)
    private UUID receivedByUserId;
}
