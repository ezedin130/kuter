package com.kuter.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StockReceiving persistence.
 */
@Repository
public interface StockReceivingRepository extends JpaRepository<StockReceiving, UUID> {

    Optional<StockReceiving> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StockReceiving> findByTenantIdAndReceivingNumber(UUID tenantId, String receivingNumber);

    Optional<StockReceiving> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    boolean existsByTenantIdAndReceivingNumber(UUID tenantId, String receivingNumber);

    Page<StockReceiving> findAllByTenantIdOrderByReceivedAtDesc(UUID tenantId, Pageable pageable);
}
