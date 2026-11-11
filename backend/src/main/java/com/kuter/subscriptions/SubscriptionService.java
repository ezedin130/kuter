package com.kuter.subscriptions;

import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.subscriptions.dto.ActivateSubscriptionDto;
import com.kuter.subscriptions.dto.AdminSubscriptionDetailDto;
import com.kuter.subscriptions.dto.AssignSubscriptionDto;
import com.kuter.subscriptions.dto.CancelSubscriptionDto;
import com.kuter.subscriptions.dto.ChangePlanDto;
import com.kuter.subscriptions.dto.CreateSubscriptionPlanDto;
import com.kuter.subscriptions.dto.ExtendSubscriptionDto;
import com.kuter.subscriptions.dto.PublicSubscriptionPlanDto;
import com.kuter.subscriptions.dto.ReactivateSubscriptionDto;
import com.kuter.subscriptions.dto.StartTrialDto;
import com.kuter.subscriptions.dto.SubscriptionHistoryDto;
import com.kuter.subscriptions.dto.SubscriptionPlanDto;
import com.kuter.subscriptions.dto.SubscriptionSummaryDto;
import com.kuter.subscriptions.dto.SuspendSubscriptionDto;
import com.kuter.subscriptions.dto.TenantSubscriptionResponseDto;
import com.kuter.subscriptions.dto.UpdateSubscriptionPlanDto;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for subscription plan management, subscription assignments,
 * lifecycle transitions, tenant subscriptions, and reporting.
 */
public interface SubscriptionService {

    // Plan Management (Platform Admin)
    SubscriptionPlanDto createPlan(CreateSubscriptionPlanDto dto, KuterUserPrincipal admin);

    PageResponse<SubscriptionPlanDto> getPlans(Pageable pageable, Boolean isActive, Boolean isPublic, String search);

    SubscriptionPlanDto getPlan(UUID id);

    SubscriptionPlanDto updatePlan(UUID id, UpdateSubscriptionPlanDto dto, KuterUserPrincipal admin);

    SubscriptionPlanDto updatePlanStatus(UUID id, boolean isActive, KuterUserPrincipal admin);

    void deletePlan(UUID id, KuterUserPrincipal admin);

    // Public Plans (PermitAll)
    List<PublicSubscriptionPlanDto> getPublicPlans();

    // Subscription Assignment & Lifecycle (Platform Admin)
    AdminSubscriptionDetailDto assignSubscription(AssignSubscriptionDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto activateSubscription(UUID subscriptionId, ActivateSubscriptionDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto startTrial(UUID subscriptionId, StartTrialDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto changePlan(UUID subscriptionId, ChangePlanDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto extendSubscription(UUID subscriptionId, ExtendSubscriptionDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto suspendSubscription(UUID subscriptionId, SuspendSubscriptionDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto reactivateSubscription(UUID subscriptionId, ReactivateSubscriptionDto dto, KuterUserPrincipal admin);

    AdminSubscriptionDetailDto cancelSubscription(UUID subscriptionId, CancelSubscriptionDto dto, KuterUserPrincipal admin);

    // Platform Admin Inquiries
    PageResponse<AdminSubscriptionDetailDto> listSubscriptions(Pageable pageable, SubscriptionStatus status, UUID planId, UUID tenantId, String search);

    AdminSubscriptionDetailDto getSubscription(UUID subscriptionId);

    AdminSubscriptionDetailDto getTenantSubscriptionForAdmin(UUID tenantId);

    SubscriptionSummaryDto getSubscriptionSummary();

    List<SubscriptionHistoryDto> getSubscriptionHistory(UUID subscriptionId);

    List<SubscriptionHistoryDto> getTenantSubscriptionHistory(UUID tenantId);

    // Shop Portal Inquiries (Tenant Scoped)
    TenantSubscriptionResponseDto getCurrentTenantSubscription(UUID tenantId);

    List<SubscriptionHistoryDto> getTenantSubscriptionHistoryForShop(UUID tenantId);

    // Operational Expiry Processing & Clock
    void processExpirations();

    void setClock(Clock clock);
}
