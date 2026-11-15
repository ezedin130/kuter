package com.kuter.inventory.dto;

import com.kuter.inventory.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private UUID id;
    private UUID productId;
    private String productSku;
    private String productName;
    private String baseUnitCode;
    private MovementType movementType;
    private UUID referenceId;
    private String referenceType;
    private BigDecimal quantityChange;
    private BigDecimal balanceAfter;
    private BigDecimal unitCost;
    private String notes;
    private UUID actorId;
    private String actorEmail;
    private Instant createdAt;

    @com.fasterxml.jackson.annotation.JsonProperty("unitCostSnapshot")
    public BigDecimal getUnitCostSnapshot() {
        return this.unitCost;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("costAtTimeOfTransaction")
    public BigDecimal getCostAtTimeOfTransaction() {
        return this.unitCost;
    }
}
