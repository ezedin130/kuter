package com.kuter.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TenantSubscription persistence.
 */
@Repository
public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, UUID>, JpaSpecificationExecutor<TenantSubscription> {

    Optional<TenantSubscription> findByTenantId(UUID tenantId);

    Optional<TenantSubscription> findByTenantIdAndIsCurrentTrue(UUID tenantId);

    Optional<TenantSubscription> findFirstByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    Optional<TenantSubscription> findByTenantIdAndStatus(UUID tenantId, SubscriptionStatus status);

    List<TenantSubscription> findAllByTenantId(UUID tenantId);

    boolean existsByPlanId(UUID planId);

    long countByStatus(SubscriptionStatus status);

    List<TenantSubscription> findByStatusInAndCurrentPeriodEndBefore(Collection<SubscriptionStatus> statuses, Instant cutoff);
}
