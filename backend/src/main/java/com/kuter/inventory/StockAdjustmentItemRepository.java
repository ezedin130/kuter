package com.kuter.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for StockAdjustmentItem persistence.
 */
@Repository
public interface StockAdjustmentItemRepository extends JpaRepository<StockAdjustmentItem, UUID> {

    List<StockAdjustmentItem> findAllByTenantIdAndAdjustmentId(UUID tenantId, UUID adjustmentId);

    List<StockAdjustmentItem> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);
}
