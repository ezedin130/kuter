package com.kuter.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UnitDefinition persistence with explicit tenant isolation.
 */
@Repository
public interface UnitDefinitionRepository extends JpaRepository<UnitDefinition, UUID> {

    Optional<UnitDefinition> findByTenantIdAndId(UUID tenantId, UUID id);

    List<UnitDefinition> findAllByTenantId(UUID tenantId);

    Optional<UnitDefinition> findByTenantIdAndCode(UUID tenantId, String code);

    Optional<UnitDefinition> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
