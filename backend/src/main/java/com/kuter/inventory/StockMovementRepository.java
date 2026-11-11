package com.kuter.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for StockMovement ledger queries.
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID>, JpaSpecificationExecutor<StockMovement> {

    Page<StockMovement> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    Page<StockMovement> findAllByTenantIdAndProductIdOrderByCreatedAtDesc(UUID tenantId, UUID productId, Pageable pageable);

    Page<StockMovement> findAllByTenantIdAndMovementTypeOrderByCreatedAtDesc(UUID tenantId, MovementType movementType, Pageable pageable);

    Page<StockMovement> findAllByTenantIdAndProductIdAndMovementTypeOrderByCreatedAtDesc(UUID tenantId, UUID productId, MovementType movementType, Pageable pageable);

    List<StockMovement> findByTenantIdAndProductIdOrderByCreatedAtDesc(UUID tenantId, UUID productId);

    List<StockMovement> findAllByTenantIdAndReferenceId(UUID tenantId, UUID referenceId);

    List<StockMovement> findAllByTenantId(UUID tenantId);
}
