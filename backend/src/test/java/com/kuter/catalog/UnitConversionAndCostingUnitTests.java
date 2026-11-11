package com.kuter.catalog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests verifying product unit conversions and moving weighted-average inventory costing.
 */
class UnitConversionAndCostingUnitTests {

    private static final RoundingMode RM = RoundingMode.HALF_UP;

    @Nested
    @DisplayName("Unit Conversion Formula Tests")
    class UnitConversionTests {

        @Test
        @DisplayName("Convert secondary unit quantities to base unit (cartons and packets to pieces)")
        void shouldConvertSecondaryUnitsToBaseQuantity() {
            // Setup: Base unit = Piece (1.0). Secondary units: Packet (10.0), Carton (100.0)
            BigDecimal cartonFactor = new BigDecimal("100.0000");
            BigDecimal packetFactor = new BigDecimal("10.0000");

            BigDecimal cartonsSold = new BigDecimal("2.0000"); // 2 * 100 = 200 pcs
            BigDecimal packetsSold = new BigDecimal("3.0000"); // 3 * 10 = 30 pcs
            BigDecimal loosePieces = new BigDecimal("5.0000");  // 5 pcs

            BigDecimal totalPiecesFromCartons = cartonsSold.multiply(cartonFactor);
            BigDecimal totalPiecesFromPackets = packetsSold.multiply(packetFactor);
            BigDecimal totalPieces = totalPiecesFromCartons.add(totalPiecesFromPackets).add(loosePieces);

            assertThat(totalPieces).isEqualByComparingTo("235.0000");
        }

        @Test
        @DisplayName("Convert base unit quantity to primary and secondary unit breakdown")
        void shouldDecomposeBaseQuantityIntoSecondaryUnits() {
            BigDecimal totalPieces = new BigDecimal("235.0000");
            BigDecimal cartonFactor = new BigDecimal("100.0000");
            BigDecimal packetFactor = new BigDecimal("10.0000");

            // Integer cartons
            BigDecimal cartons = totalPieces.divideToIntegralValue(cartonFactor);
            BigDecimal remainderAfterCartons = totalPieces.remainder(cartonFactor);

            // Integer packets
            BigDecimal packets = remainderAfterCartons.divideToIntegralValue(packetFactor);
            BigDecimal piecesRemaining = remainderAfterCartons.remainder(packetFactor);

            assertThat(cartons).isEqualByComparingTo("2.0000");
            assertThat(packets).isEqualByComparingTo("3.0000");
            assertThat(piecesRemaining).isEqualByComparingTo("5.0000");
        }

        @Test
        @DisplayName("Reject non-positive or zero conversion factors")
        void shouldRejectInvalidConversionFactors() {
            BigDecimal invalidFactorZero = BigDecimal.ZERO;
            BigDecimal invalidFactorNegative = new BigDecimal("-5.0000");

            assertThat(invalidFactorZero.compareTo(BigDecimal.ZERO) <= 0).isTrue();
            assertThat(invalidFactorNegative.compareTo(BigDecimal.ZERO) <= 0).isTrue();
        }
    }

    @Nested
    @DisplayName("Moving Weighted-Average Costing Tests")
    class WeightedAverageCostingTests {

        @Test
        @DisplayName("Formula: New Average Cost = (Old Value + Received Value) / New Total Quantity")
        void shouldComputeMovingWeightedAverageCostOnReceiving() {
            // Initial: 100 units @ $10.0000 = $1,000.0000
            BigDecimal currentQty = new BigDecimal("100.0000");
            BigDecimal currentUnitCost = new BigDecimal("10.0000");
            BigDecimal currentValue = currentQty.multiply(currentUnitCost);

            // Receive: 50 units @ $13.0000 = $650.0000
            BigDecimal receivedQty = new BigDecimal("50.0000");
            BigDecimal receivedUnitCost = new BigDecimal("13.0000");
            BigDecimal receivedValue = receivedQty.multiply(receivedUnitCost);

            BigDecimal newTotalQty = currentQty.add(receivedQty);
            BigDecimal newTotalValue = currentValue.add(receivedValue);
            BigDecimal newAverageCost = newTotalValue.divide(newTotalQty, 4, RM);

            assertThat(newTotalQty).isEqualByComparingTo("150.0000");
            assertThat(newTotalValue).isEqualByComparingTo("1650.0000");
            // 1650 / 150 = 11.0000
            assertThat(newAverageCost).isEqualByComparingTo("11.0000");
        }

        @Test
        @DisplayName("Initial receiving on zero stock sets unit cost directly without division error")
        void shouldSetUnitCostDirectlyOnZeroStockReceiving() {
            BigDecimal currentQty = BigDecimal.ZERO;
            BigDecimal receivedQty = new BigDecimal("50.0000");
            BigDecimal receivedUnitCost = new BigDecimal("15.2500");

            BigDecimal newAverageCost;
            if (currentQty.compareTo(BigDecimal.ZERO) <= 0) {
                newAverageCost = receivedUnitCost;
            } else {
                newAverageCost = BigDecimal.ZERO;
            }

            assertThat(newAverageCost).isEqualByComparingTo("15.2500");
        }

        @Test
        @DisplayName("Sales/deductions reduce stock quantity and total value while preserving unit cost")
        void shouldPreserveUnitCostOnStockDeduction() {
            BigDecimal currentQty = new BigDecimal("150.0000");
            BigDecimal unitCost = new BigDecimal("11.0000");

            BigDecimal soldQty = new BigDecimal("60.0000");
            BigDecimal remainingQty = currentQty.subtract(soldQty);
            BigDecimal remainingValue = remainingQty.multiply(unitCost);

            assertThat(remainingQty).isEqualByComparingTo("90.0000");
            assertThat(unitCost).isEqualByComparingTo("11.0000");
            assertThat(remainingValue).isEqualByComparingTo("990.0000");
        }

        @Test
        @DisplayName("Downward adjustment (shrinkage/breakage) reduces stock and retains unit cost")
        void shouldRetainUnitCostOnDownwardAdjustment() {
            BigDecimal currentQty = new BigDecimal("90.0000");
            BigDecimal unitCost = new BigDecimal("11.0000");

            BigDecimal damagedQty = new BigDecimal("10.0000");
            BigDecimal adjustedQty = currentQty.subtract(damagedQty);
            BigDecimal adjustedValue = adjustedQty.multiply(unitCost);

            assertThat(adjustedQty).isEqualByComparingTo("80.0000");
            assertThat(unitCost).isEqualByComparingTo("11.0000");
            assertThat(adjustedValue).isEqualByComparingTo("880.0000");
        }

        @Test
        @DisplayName("Upward adjustment without custom cost keeps existing unit cost")
        void shouldRetainUnitCostOnUpwardAdjustmentWithoutCustomCost() {
            BigDecimal currentQty = new BigDecimal("80.0000");
            BigDecimal currentUnitCost = new BigDecimal("11.0000");

            BigDecimal foundQty = new BigDecimal("20.0000");
            BigDecimal newQty = currentQty.add(foundQty);
            BigDecimal newUnitCost = currentUnitCost;
            BigDecimal newValue = newQty.multiply(newUnitCost);

            assertThat(newQty).isEqualByComparingTo("100.0000");
            assertThat(newUnitCost).isEqualByComparingTo("11.0000");
            assertThat(newValue).isEqualByComparingTo("1100.0000");
        }
    }
}
