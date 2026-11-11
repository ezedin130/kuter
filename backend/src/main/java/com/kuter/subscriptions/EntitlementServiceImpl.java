package com.kuter.subscriptions;

import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.common.ConflictException;
import com.kuter.common.ForbiddenException;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.subscriptions.dto.SubscriptionUsageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for plan entitlements, resource limits, and subscription effective status evaluation.
 */
@Service
@Transactional(readOnly = true)
public class EntitlementServiceImpl implements EntitlementService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementServiceImpl.class);

    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final ProductRepository productRepository;
    private final TenantMembershipRepository membershipRepository;
    private Clock clock;

    public EntitlementServiceImpl(TenantSubscriptionRepository subscriptionRepository,
                                  SubscriptionPlanRepository planRepository,
                                  ProductRepository productRepository,
                                  TenantMembershipRepository membershipRepository,
                                  Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.productRepository = productRepository;
        this.membershipRepository = membershipRepository;
        this.clock = clock;
    }

    @Override
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public SubscriptionStatus getEffectiveStatus(UUID tenantId) {
        if (tenantId == null) {
            return SubscriptionStatus.ACTIVE;
        }

        Optional<TenantSubscription> subOpt = subscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId));

        if (subOpt.isEmpty()) {
            return SubscriptionStatus.ACTIVE;
        }

        TenantSubscription sub = subOpt.get();
        SubscriptionStatus storedStatus = sub.getStatus();

        if (storedStatus == SubscriptionStatus.SUSPENDED
                || storedStatus == SubscriptionStatus.CANCELLED
                || storedStatus == SubscriptionStatus.PENDING) {
            return storedStatus;
        }

        if (storedStatus == SubscriptionStatus.ACTIVE || storedStatus == SubscriptionStatus.TRIALING) {
            if (clock.instant().isAfter(sub.getCurrentPeriodEnd())) {
                return SubscriptionStatus.EXPIRED;
            }
            return storedStatus;
        }

        return storedStatus;
    }

    @Override
    public boolean isSubscriptionActive(UUID tenantId) {
        SubscriptionStatus effectiveStatus = getEffectiveStatus(tenantId);
        return effectiveStatus == SubscriptionStatus.ACTIVE || effectiveStatus == SubscriptionStatus.TRIALING;
    }

    @Override
    public void assertActiveSubscription(UUID tenantId) {
        if (tenantId == null) {
            return;
        }

        SubscriptionStatus effectiveStatus = getEffectiveStatus(tenantId);

        if (effectiveStatus == SubscriptionStatus.EXPIRED) {
            throw new ForbiddenException("Subscription for this shop has expired. Access to restricted features is blocked until renewed.");
        }
        if (effectiveStatus == SubscriptionStatus.SUSPENDED) {
            throw new ForbiddenException("Subscription for this shop is suspended. Access to restricted features is blocked.");
        }
        if (effectiveStatus == SubscriptionStatus.CANCELLED) {
            throw new ForbiddenException("Subscription for this shop has been cancelled. Access to restricted features is blocked.");
        }
        if (effectiveStatus == SubscriptionStatus.PENDING) {
            throw new ForbiddenException("Subscription for this shop is pending activation.");
        }
    }

    @Override
    public void checkProductLimit(UUID tenantId) {
        assertActiveSubscription(tenantId);

        SubscriptionPlan plan = resolveCurrentPlan(tenantId);
        if (plan == null || plan.getProductLimit() == null) {
            return; // Unlimited
        }

        long activeProductsCount = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE);
        if (activeProductsCount >= plan.getProductLimit()) {
            throw new ConflictException("Catalog product limit of " + plan.getProductLimit() +
                    " reached for your current plan (" + plan.getName() + "). Please upgrade your plan to add more products.");
        }
    }

    @Override
    public void checkUserLimit(UUID tenantId) {
        assertActiveSubscription(tenantId);

        SubscriptionPlan plan = resolveCurrentPlan(tenantId);
        int planLimit = (plan != null && plan.getUserLimit() != null && plan.getUserLimit() > 0) ? plan.getUserLimit() : 4;
        
        // Critical Requirements Addendum: initial shop user limit is 4 accounts (1 owner + up to 3 staff).
        // If a plan explicitly defines a lower limit (e.g. Micro plan with limit 2), respect the lower limit.
        // For standard plans (Starter with 3 staff seats, Growth, Enterprise), limit is capped at 4 accounts.
        int effectiveLimit = (planLimit <= 2) ? planLimit : 4;

        long activeUsersCount = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);
        if (activeUsersCount >= effectiveLimit) {
            if (effectiveLimit < 4 && plan != null) {
                throw new ConflictException("Staff user seat limit of " + effectiveLimit +
                        " reached for your current plan (" + plan.getName() + "). Please upgrade your plan to add more staff.");
            } else {
                throw new ConflictException("Shop user limit reached. A shop can have a maximum of 4 active user accounts (1 owner and up to 3 staff). Please deactivate an existing staff member before adding or reactivating another.");
            }
        }
    }

    @Override
    public boolean isFeatureEnabled(UUID tenantId, String featureKey) {
        if (!isSubscriptionActive(tenantId)) {
            return false;
        }

        SubscriptionPlan plan = resolveCurrentPlan(tenantId);
        if (plan == null || plan.getFeaturesSummary() == null) {
            return true;
        }

        return plan.getFeaturesSummary().toLowerCase().contains(featureKey.toLowerCase());
    }

    @Override
    public SubscriptionUsageDto getUsage(UUID tenantId) {
        SubscriptionPlan plan = resolveCurrentPlan(tenantId);
        SubscriptionStatus effectiveStatus = getEffectiveStatus(tenantId);

        Optional<TenantSubscription> subOpt = subscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId));

        SubscriptionStatus storedStatus = subOpt.map(TenantSubscription::getStatus).orElse(effectiveStatus);

        long activeProducts = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE);
        long activeUsers = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);

        Integer productLimit = plan != null ? plan.getProductLimit() : null;
        Integer userLimit = plan != null ? plan.getUserLimit() : null;

        return SubscriptionUsageDto.builder()
                .tenantId(tenantId)
                .planCode(plan != null ? plan.getCode() : "unknown")
                .planName(plan != null ? plan.getName() : "Unknown Plan")
                .status(storedStatus)
                .effectiveStatus(effectiveStatus)
                .activeProductsCount(activeProducts)
                .productLimit(productLimit)
                .productLimitReached(productLimit != null && activeProducts >= productLimit)
                .activeUsersCount(activeUsers)
                .userLimit(userLimit)
                .userLimitReached(userLimit != null && activeUsers >= userLimit)
                .build();
    }

    private SubscriptionPlan resolveCurrentPlan(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }

        return subscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId))
                .flatMap(sub -> planRepository.findById(sub.getPlanId()))
                .or(() -> planRepository.findByCode("starter"))
                .orElse(null);
    }
}
