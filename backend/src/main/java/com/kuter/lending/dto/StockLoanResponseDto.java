package com.kuter.lending.dto;

import com.kuter.lending.LoanDirection;
import com.kuter.lending.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLoanResponseDto {

    private UUID id;
    private UUID tenantId;
    private String loanReference;
    private LoanDirection direction;
    private LoanStatus status;

    private String counterpartyName;
    private String counterpartyContactPerson;
    private String counterpartyPhone;
    private String counterpartyAddress;

    private UUID productId;
    private String productSku;
    private String productName;
    private UUID unitId;
    private String unitCode;
    private BigDecimal conversionFactor;

    private BigDecimal initialQuantity;
    private BigDecimal initialBaseQuantity;
    private BigDecimal returnedQuantity;
    private BigDecimal returnedBaseQuantity;
    private BigDecimal outstandingBaseQuantity;

    private BigDecimal unitCostSnapshot;
    private Instant transactionDate;
    private Instant expectedReturnDate;
    private String notes;
    private String cancellationReason;

    private UUID createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<StockLoanReturnResponseDto> returns = List.of();
}
