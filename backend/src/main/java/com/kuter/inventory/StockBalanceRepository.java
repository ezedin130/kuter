package com.kuter.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for StockBalance persistence with pessimistic locking support for concurrent stock operations.
 */
@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID> {

    Optional<StockBalance> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sb FROM StockBalance sb WHERE sb.tenantId = :tenantId AND sb.productId = :productId")
    Optional<StockBalance> findByTenantIdAndProductIdForUpdate(@Param("tenantId") UUID tenantId, @Param("productId") UUID productId);

    List<StockBalance> findAllByTenantId(UUID tenantId);

    Page<StockBalance> findAllByTenantId(UUID tenantId, Pageable pageable);
}
