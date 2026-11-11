package com.kuter.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Sale persistence.
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<Sale> findByTenantIdAndInvoiceNumber(UUID tenantId, String invoiceNumber);

    Optional<Sale> findByTenantIdAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    Page<Sale> findAllByTenantIdOrderBySoldAtDesc(UUID tenantId, Pageable pageable);

    Page<Sale> findAllByTenantIdAndStatusOrderBySoldAtDesc(UUID tenantId, SaleStatus status, Pageable pageable);

    List<Sale> findAllByTenantIdAndSoldAtBetween(UUID tenantId, Instant start, Instant end);

    List<Sale> findAllByTenantIdAndStatusAndSoldAtBetween(UUID tenantId, SaleStatus status, Instant start, Instant end);
}
