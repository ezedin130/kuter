package com.kuter.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StockAdjustment persistence.
 */
@Repository
public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, UUID> {

    Optional<StockAdjustment> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StockAdjustment> findByTenantIdAndAdjustmentNumber(UUID tenantId, String adjustmentNumber);

    Optional<StockAdjustment> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    boolean existsByTenantIdAndAdjustmentNumber(UUID tenantId, String adjustmentNumber);

    Page<StockAdjustment> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
