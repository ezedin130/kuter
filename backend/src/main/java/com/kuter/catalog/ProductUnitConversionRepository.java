package com.kuter.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProductUnitConversion persistence.
 */
@Repository
public interface ProductUnitConversionRepository extends JpaRepository<ProductUnitConversion, UUID> {

    List<ProductUnitConversion> findAllByTenantId(UUID tenantId);

    List<ProductUnitConversion> findAllByTenantIdAndProductId(UUID tenantId, UUID productId);

    Optional<ProductUnitConversion> findByProductIdAndUnitId(UUID productId, UUID unitId);

    Optional<ProductUnitConversion> findByTenantIdAndProductIdAndUnitId(UUID tenantId, UUID productId, UUID unitId);

    boolean existsByTenantIdAndProductIdAndUnitId(UUID tenantId, UUID productId, UUID unitId);

    void deleteByTenantIdAndProductIdAndUnitId(UUID tenantId, UUID productId, UUID unitId);
}
