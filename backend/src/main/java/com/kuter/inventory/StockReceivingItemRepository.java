package com.kuter.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for StockReceivingItem persistence.
 */
@Repository
public interface StockReceivingItemRepository extends JpaRepository<StockReceivingItem, UUID> {

    List<StockReceivingItem> findAllByTenantIdAndReceivingId(UUID tenantId, UUID receivingId);

    List<StockReceivingItem> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);
}
