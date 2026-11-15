-- =============================================================================
-- Migration V10: Reconstruct Historical Stock Movement Costs from Source Transactions
-- =============================================================================

-- 1. Reconstruct unit_cost for RECEIPT movements from stock_receiving_items
UPDATE stock_movements sm
SET unit_cost = (
    SELECT sri.unit_cost
    FROM stock_receiving_items sri
    WHERE sri.receiving_id = sm.reference_id
      AND sri.product_id = sm.product_id
    LIMIT 1
)
WHERE sm.unit_cost IS NULL
  AND (sm.movement_type = 'RECEIPT' OR sm.reference_type = 'STOCK_RECEIVING')
  AND sm.reference_id IS NOT NULL;

-- 2. Reconstruct unit_cost for SALE movements from sale_items
UPDATE stock_movements sm
SET unit_cost = (
    SELECT si.unit_cost
    FROM sale_items si
    WHERE si.sale_id = sm.reference_id
      AND si.product_id = sm.product_id
    LIMIT 1
)
WHERE sm.unit_cost IS NULL
  AND (sm.movement_type = 'SALE' OR sm.reference_type = 'SALE')
  AND sm.reference_id IS NOT NULL;

-- 3. Reconstruct unit_cost for ADJUSTMENT movements from stock_adjustment_items
UPDATE stock_movements sm
SET unit_cost = (
    SELECT sai.unit_cost
    FROM stock_adjustment_items sai
    WHERE sai.adjustment_id = sm.reference_id
      AND sai.product_id = sm.product_id
    LIMIT 1
)
WHERE sm.unit_cost IS NULL
  AND (sm.movement_type = 'ADJUSTMENT' OR sm.reference_type = 'STOCK_ADJUSTMENT')
  AND sm.reference_id IS NOT NULL;

-- 4. Fallback for any remaining unlinked movements using the product's catalog cost_price
UPDATE stock_movements sm
SET unit_cost = (
    SELECT p.cost_price
    FROM products p
    WHERE p.id = sm.product_id
)
WHERE sm.unit_cost IS NULL;
