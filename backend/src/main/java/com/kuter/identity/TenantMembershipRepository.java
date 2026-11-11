package com.kuter.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TenantMembership queries.
 */
@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findAllByUserId(UUID userId);

    List<TenantMembership> findAllByTenantId(UUID tenantId);

    List<TenantMembership> findAllByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    Optional<TenantMembership> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<TenantMembership> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

    boolean existsByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, MembershipStatus status);

    long countByTenantIdAndRoleAndStatus(UUID tenantId, TenantRole role, MembershipStatus status);

    long countByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    long countByTenantId(UUID tenantId);
}
