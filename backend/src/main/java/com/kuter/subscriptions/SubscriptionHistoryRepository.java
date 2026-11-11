package com.kuter.subscriptions;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for SubscriptionHistory persistence.
 */
@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, UUID> {

    List<SubscriptionHistory> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    List<SubscriptionHistory> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
