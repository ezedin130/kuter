package com.kuter.subscriptions;

import com.kuter.audit.AuditService;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.common.BadRequestException;
import com.kuter.common.ConflictException;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
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
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation managing plans, subscriptions, lifecycle transitions, and audit history.
 */
@Service
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final SubscriptionHistoryRepository historyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AuditService auditService;
    private Clock clock;

    public SubscriptionServiceImpl(SubscriptionPlanRepository planRepository,
                                   TenantSubscriptionRepository subscriptionRepository,
                                   SubscriptionHistoryRepository historyRepository,
                                   TenantRepository tenantRepository,
                                   UserRepository userRepository,
                                   ProductRepository productRepository,
                                   TenantMembershipRepository membershipRepository,
                                   AuditService auditService,
                                   Clock clock) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.historyRepository = historyRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.membershipRepository = membershipRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    // -------------------------------------------------------------------------
    // PLAN MANAGEMENT (PLATFORM ADMIN)
    // -------------------------------------------------------------------------

    @Override
    public SubscriptionPlanDto createPlan(CreateSubscriptionPlanDto dto, KuterUserPrincipal admin) {
        String code = dto.getCode().trim().toLowerCase();
        if (planRepository.existsByCode(code)) {
            throw new ConflictException("Subscription plan with code '" + code + "' already exists.");
        }

        SubscriptionPlan plan = SubscriptionPlan.builder()
                .code(code)
                .name(dto.getName().trim())
                .description(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .price(dto.getPrice())
                .currency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency().trim().toUpperCase() : "USD")
                .billingInterval(dto.getBillingInterval())
                .productLimit(dto.getProductLimit())
                .userLimit(dto.getUserLimit())
                .branchLimit(dto.getBranchLimit())
                .trialDays(dto.getTrialDays() != null ? dto.getTrialDays() : 14)
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : true)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .featuresSummary(StringUtils.hasText(dto.getFeaturesSummary()) ? dto.getFeaturesSummary().trim() : null)
                .build();

        SubscriptionPlan saved = planRepository.save(plan);

        logAudit(null, admin, "SUBSCRIPTION_PLAN_CREATED", "SUBSCRIPTION_PLAN", saved.getId(),
                "Created plan: code=" + saved.getCode() + ", name=" + saved.getName());

        return mapToPlanDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SubscriptionPlanDto> getPlans(Pageable pageable, Boolean isActive, Boolean isPublic, String search) {
        Specification<SubscriptionPlan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (isPublic != null) {
                predicates.add(cb.equal(root.get("isPublic"), isPublic));
            }
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codeLike = cb.like(cb.lower(root.get("code")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameLike, codeLike, descLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SubscriptionPlan> page = planRepository.findAll(spec, pageable);
        List<SubscriptionPlanDto> dtos = page.getContent().stream().map(this::mapToPlanDto).toList();
        return PageResponse.of(page, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionPlanDto getPlan(UUID id) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + id));
        return mapToPlanDto(plan);
    }

    @Override
    public SubscriptionPlanDto updatePlan(UUID id, UpdateSubscriptionPlanDto dto, KuterUserPrincipal admin) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + id));

        plan.setName(dto.getName().trim());
        plan.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        plan.setPrice(dto.getPrice());
        if (StringUtils.hasText(dto.getCurrency())) {
            plan.setCurrency(dto.getCurrency().trim().toUpperCase());
        }
        plan.setBillingInterval(dto.getBillingInterval());
        plan.setProductLimit(dto.getProductLimit());
        plan.setUserLimit(dto.getUserLimit());
        plan.setBranchLimit(dto.getBranchLimit());
        if (dto.getTrialDays() != null) {
            plan.setTrialDays(dto.getTrialDays());
        }
        if (dto.getIsPublic() != null) {
            plan.setPublic(dto.getIsPublic());
        }
        if (dto.getIsActive() != null) {
            plan.setActive(dto.getIsActive());
        }
        if (StringUtils.hasText(dto.getFeaturesSummary())) {
            plan.setFeaturesSummary(dto.getFeaturesSummary().trim());
        }

        SubscriptionPlan saved = planRepository.save(plan);

        logAudit(null, admin, "SUBSCRIPTION_PLAN_UPDATED", "SUBSCRIPTION_PLAN", saved.getId(),
                "Updated plan limits/pricing: code=" + saved.getCode());

        return mapToPlanDto(saved);
    }

    @Override
    public SubscriptionPlanDto updatePlanStatus(UUID id, boolean isActive, KuterUserPrincipal admin) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + id));

        plan.setActive(isActive);
        SubscriptionPlan saved = planRepository.save(plan);

        logAudit(null, admin, "SUBSCRIPTION_PLAN_STATUS_CHANGED", "SUBSCRIPTION_PLAN", saved.getId(),
                "Plan " + saved.getCode() + " active status set to " + isActive);

        return mapToPlanDto(saved);
    }

    @Override
    public void deletePlan(UUID id, KuterUserPrincipal admin) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + id));

        if (subscriptionRepository.existsByPlanId(id)) {
            throw new ConflictException("Cannot delete plan '" + plan.getName() + "' because it is referenced by existing tenant subscriptions. Deactivate the plan instead.");
        }

        planRepository.delete(plan);

        logAudit(null, admin, "SUBSCRIPTION_PLAN_DELETED", "SUBSCRIPTION_PLAN", id,
                "Deleted plan code=" + plan.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicSubscriptionPlanDto> getPublicPlans() {
        return planRepository.findAllByIsActiveTrueAndIsPublicTrue().stream()
                .map(p -> PublicSubscriptionPlanDto.builder()
                        .id(p.getId())
                        .code(p.getCode())
                        .name(p.getName())
                        .description(p.getDescription())
                        .price(p.getPrice())
                        .currency(p.getCurrency())
                        .billingInterval(p.getBillingInterval())
                        .productLimit(p.getProductLimit())
                        .userLimit(p.getUserLimit())
                        .branchLimit(p.getBranchLimit())
                        .trialDays(p.getTrialDays())
                        .featuresSummary(p.getFeaturesSummary())
                        .build())
                .toList();
    }

    // -------------------------------------------------------------------------
    // SUBSCRIPTION ASSIGNMENT & LIFECYCLE (PLATFORM ADMIN)
    // -------------------------------------------------------------------------

    @Override
    public AdminSubscriptionDetailDto assignSubscription(AssignSubscriptionDto dto, KuterUserPrincipal admin) {
        Tenant tenant = tenantRepository.findById(dto.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + dto.getTenantId()));

        SubscriptionPlan plan = planRepository.findById(dto.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found with ID: " + dto.getPlanId()));

        if (!plan.isActive()) {
            throw new BadRequestException("Cannot assign inactive subscription plan: " + plan.getName());
        }

        // De-flag existing active subscriptions for this tenant
        List<TenantSubscription> existingSubs = subscriptionRepository.findAllByTenantId(tenant.getId());
        for (TenantSubscription existing : existingSubs) {
            if (existing.isCurrent()) {
                existing.setCurrent(false);
                subscriptionRepository.save(existing);
            }
        }

        Instant now = clock.instant();
        SubscriptionStatus status = dto.getStatus() != null ? dto.getStatus() : SubscriptionStatus.ACTIVE;
        int periodDays = dto.getPeriodDays() != null && dto.getPeriodDays() > 0 ? dto.getPeriodDays() : 30;

        Instant periodStart = now;
        Instant periodEnd = now.plus(periodDays, ChronoUnit.DAYS);
        Instant trialStart = null;
        Instant trialEnd = null;

        if (status == SubscriptionStatus.TRIALING) {
            int trialDays = dto.getTrialDays() != null && dto.getTrialDays() > 0
                    ? dto.getTrialDays()
                    : (plan.getTrialDays() != null ? plan.getTrialDays() : 14);
            trialStart = now;
            trialEnd = now.plus(trialDays, ChronoUnit.DAYS);
            periodEnd = trialEnd;
        }

        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenant.getId())
                .planId(plan.getId())
                .status(status)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .trialStart(trialStart)
                .trialEnd(trialEnd)
                .cancelAtPeriodEnd(false)
                .notes(dto.getNotes())
                .lastModifiedBy(admin != null ? admin.getUserId() : null)
                .isCurrent(true)
                .build();

        TenantSubscription saved = subscriptionRepository.save(subscription);

        recordHistory(saved, SubscriptionAction.ASSIGNED, null, saved.getStatus(),
                null, plan.getId(), null, saved.getCurrentPeriodEnd(),
                dto.getNotes() != null ? dto.getNotes() : "Assigned plan " + plan.getCode(),
                admin != null ? admin.getUserId() : null);

        logAudit(tenant.getId(), admin, "SUBSCRIPTION_ASSIGNED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Assigned plan " + plan.getCode() + " with status " + saved.getStatus());

        return mapToAdminDetailDto(saved, tenant, plan);
    }

    @Override
    public AdminSubscriptionDetailDto activateSubscription(UUID subscriptionId, ActivateSubscriptionDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Cannot activate a cancelled subscription. Please assign a new plan.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setTrialStart(null);
        sub.setTrialEnd(null);
        sub.setCancelAtPeriodEnd(false);

        Instant now = clock.instant();
        int days = dto != null && dto.getPeriodDays() != null && dto.getPeriodDays() > 0 ? dto.getPeriodDays() : 30;

        // If currently expired or ended, reset period to start from now
        if (sub.getCurrentPeriodEnd() == null || sub.getCurrentPeriodEnd().isBefore(now)) {
            sub.setCurrentPeriodStart(now);
            sub.setCurrentPeriodEnd(now.plus(days, ChronoUnit.DAYS));
        }

        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);
        if (dto != null && StringUtils.hasText(dto.getNotes())) {
            sub.setNotes(dto.getNotes());
        }

        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.ACTIVATED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), sub.getCurrentPeriodEnd(), saved.getCurrentPeriodEnd(),
                dto != null && dto.getNotes() != null ? dto.getNotes() : "Subscription activated",
                admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_ACTIVATED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Subscription activated: periodEnd=" + saved.getCurrentPeriodEnd());

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto startTrial(UUID subscriptionId, StartTrialDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Cannot start trial for a cancelled subscription.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();
        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);

        int trialDays = dto != null && dto.getTrialDays() != null && dto.getTrialDays() > 0
                ? dto.getTrialDays()
                : (plan != null && plan.getTrialDays() != null ? plan.getTrialDays() : 14);

        Instant now = clock.instant();
        sub.setStatus(SubscriptionStatus.TRIALING);
        sub.setTrialStart(now);
        sub.setTrialEnd(now.plus(trialDays, ChronoUnit.DAYS));
        sub.setCurrentPeriodStart(now);
        sub.setCurrentPeriodEnd(sub.getTrialEnd());
        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);
        if (dto != null && StringUtils.hasText(dto.getNotes())) {
            sub.setNotes(dto.getNotes());
        }

        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.TRIAL_STARTED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), null, saved.getCurrentPeriodEnd(),
                dto != null && dto.getNotes() != null ? dto.getNotes() : "Started " + trialDays + "-day trial",
                admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_TRIAL_STARTED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Trial started for " + trialDays + " days");

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto changePlan(UUID subscriptionId, ChangePlanDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        SubscriptionPlan newPlan = planRepository.findById(dto.getNewPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("New subscription plan not found with ID: " + dto.getNewPlanId()));

        if (!newPlan.isActive()) {
            throw new BadRequestException("Cannot switch to an inactive subscription plan: " + newPlan.getName());
        }

        UUID prevPlanId = sub.getPlanId();
        sub.setPlanId(newPlan.getId());
        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);
        if (StringUtils.hasText(dto.getNotes())) {
            sub.setNotes(dto.getNotes());
        }

        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.PLAN_CHANGED, saved.getStatus(), saved.getStatus(),
                prevPlanId, newPlan.getId(), saved.getCurrentPeriodEnd(), saved.getCurrentPeriodEnd(),
                dto.getNotes() != null ? dto.getNotes() : "Plan changed to " + newPlan.getCode(),
                admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_PLAN_CHANGED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Switched plan to " + newPlan.getCode());

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto extendSubscription(UUID subscriptionId, ExtendSubscriptionDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (!StringUtils.hasText(dto.getReason())) {
            throw new BadRequestException("Mandatory reason must be provided for subscription extension.");
        }

        Instant prevEnd = sub.getCurrentPeriodEnd();
        Instant newEnd;

        if (dto.getNewPeriodEnd() != null) {
            newEnd = dto.getNewPeriodEnd();
        } else if (dto.getAdditionalDays() != null && dto.getAdditionalDays() != 0) {
            Instant base = prevEnd != null && prevEnd.isAfter(clock.instant()) ? prevEnd : clock.instant();
            newEnd = base.plus(dto.getAdditionalDays(), ChronoUnit.DAYS);
        } else {
            throw new BadRequestException("Either additionalDays or newPeriodEnd must be specified.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();
        sub.setCurrentPeriodEnd(newEnd);

        // If extending an expired subscription beyond now, restore to ACTIVE
        if (sub.getStatus() == SubscriptionStatus.EXPIRED && newEnd.isAfter(clock.instant())) {
            sub.setStatus(SubscriptionStatus.ACTIVE);
        }

        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);
        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.EXTENDED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), prevEnd, newEnd,
                dto.getReason(), admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_EXTENDED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Extended subscription period to " + newEnd + ". Reason: " + dto.getReason());

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto suspendSubscription(UUID subscriptionId, SuspendSubscriptionDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (!StringUtils.hasText(dto.getReason())) {
            throw new BadRequestException("Mandatory reason must be provided for subscription suspension.");
        }

        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Cannot suspend an already cancelled subscription.");
        }
        if (sub.getStatus() == SubscriptionStatus.SUSPENDED) {
            throw new BadRequestException("Subscription is already suspended.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();
        sub.setStatus(SubscriptionStatus.SUSPENDED);
        sub.setSuspendedAt(clock.instant());
        sub.setSuspensionReason(dto.getReason().trim());
        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);

        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.SUSPENDED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), saved.getCurrentPeriodEnd(), saved.getCurrentPeriodEnd(),
                dto.getReason(), admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_SUSPENDED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Subscription suspended. Reason: " + dto.getReason());

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto reactivateSubscription(UUID subscriptionId, ReactivateSubscriptionDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (sub.getStatus() != SubscriptionStatus.SUSPENDED) {
            throw new BadRequestException("Only suspended subscriptions can be reactivated.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setSuspendedAt(null);
        sub.setSuspensionReason(null);
        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);

        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.REACTIVATED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), saved.getCurrentPeriodEnd(), saved.getCurrentPeriodEnd(),
                dto != null && dto.getNotes() != null ? dto.getNotes() : "Subscription reactivated",
                admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_REACTIVATED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Subscription reactivated from suspension.");

        return loadAndMapAdminDetail(saved);
    }

    @Override
    public AdminSubscriptionDetailDto cancelSubscription(UUID subscriptionId, CancelSubscriptionDto dto, KuterUserPrincipal admin) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);

        if (!StringUtils.hasText(dto.getReason())) {
            throw new BadRequestException("Mandatory reason must be provided for subscription cancellation.");
        }
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled.");
        }

        SubscriptionStatus prevStatus = sub.getStatus();

        if (dto.isImmediate()) {
            sub.setStatus(SubscriptionStatus.CANCELLED);
            sub.setCancelledAt(clock.instant());
            sub.setCancellationReason(dto.getReason().trim());
            sub.setCancelAtPeriodEnd(false);
        } else {
            sub.setCancelAtPeriodEnd(true);
            sub.setCancellationReason(dto.getReason().trim());
        }

        sub.setLastModifiedBy(admin != null ? admin.getUserId() : null);
        TenantSubscription saved = subscriptionRepository.save(sub);

        recordHistory(saved, SubscriptionAction.CANCELLED, prevStatus, saved.getStatus(),
                saved.getPlanId(), saved.getPlanId(), saved.getCurrentPeriodEnd(), saved.getCurrentPeriodEnd(),
                dto.getReason(), admin != null ? admin.getUserId() : null);

        logAudit(saved.getTenantId(), admin, "SUBSCRIPTION_CANCELLED", "TENANT_SUBSCRIPTION", saved.getId(),
                "Subscription cancelled (immediate=" + dto.isImmediate() + "). Reason: " + dto.getReason());

        return loadAndMapAdminDetail(saved);
    }

    // -------------------------------------------------------------------------
    // PLATFORM ADMIN INQUIRIES
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminSubscriptionDetailDto> listSubscriptions(Pageable pageable, SubscriptionStatus status, UUID planId, UUID tenantId, String search) {
        Specification<TenantSubscription> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (planId != null) {
                predicates.add(cb.equal(root.get("planId"), planId));
            }
            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TenantSubscription> page = subscriptionRepository.findAll(spec, pageable);

        // Pre-fetch tenants and plans in bulk
        List<UUID> tenantIds = page.map(TenantSubscription::getTenantId).stream().distinct().toList();
        List<UUID> planIds = page.map(TenantSubscription::getPlanId).stream().distinct().toList();

        Map<UUID, Tenant> tenantMap = tenantRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity()));
        Map<UUID, SubscriptionPlan> planMap = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity()));

        List<AdminSubscriptionDetailDto> dtos = page.getContent().stream()
                .map(sub -> mapToAdminDetailDto(sub, tenantMap.get(sub.getTenantId()), planMap.get(sub.getPlanId())))
                .toList();

        return PageResponse.of(page, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSubscriptionDetailDto getSubscription(UUID subscriptionId) {
        TenantSubscription sub = findSubscriptionOrThrow(subscriptionId);
        return loadAndMapAdminDetail(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSubscriptionDetailDto getTenantSubscriptionForAdmin(UUID tenantId) {
        TenantSubscription sub = subscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("No subscription found for tenant: " + tenantId));
        return loadAndMapAdminDetail(sub);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionSummaryDto getSubscriptionSummary() {
        long total = subscriptionRepository.count();
        long active = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long trialing = subscriptionRepository.countByStatus(SubscriptionStatus.TRIALING);
        long expired = subscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);
        long suspended = subscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);
        long cancelled = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED);

        // Calculate estimated monthly revenue from active subscriptions
        List<TenantSubscription> activeSubs = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .toList();

        Map<UUID, SubscriptionPlan> planMap = planRepository.findAll().stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity()));

        BigDecimal mrr = BigDecimal.ZERO;
        for (TenantSubscription sub : activeSubs) {
            SubscriptionPlan plan = planMap.get(sub.getPlanId());
            if (plan != null && plan.getPrice() != null) {
                if (plan.getBillingInterval() == BillingInterval.YEARLY) {
                    mrr = mrr.add(plan.getPrice().divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP));
                } else {
                    mrr = mrr.add(plan.getPrice());
                }
            }
        }

        return SubscriptionSummaryDto.builder()
                .totalSubscriptions(total)
                .activeCount(active)
                .trialingCount(trialing)
                .expiredCount(expired)
                .suspendedCount(suspended)
                .cancelledCount(cancelled)
                .estimatedMonthlyRevenue(mrr)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionHistoryDto> getSubscriptionHistory(UUID subscriptionId) {
        List<SubscriptionHistory> historyList = historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId);
        return mapHistoryList(historyList);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionHistoryDto> getTenantSubscriptionHistory(UUID tenantId) {
        List<SubscriptionHistory> historyList = historyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return mapHistoryList(historyList);
    }

    // -------------------------------------------------------------------------
    // SHOP PORTAL INQUIRIES (TENANT SCOPED)
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TenantSubscriptionResponseDto getCurrentTenantSubscription(UUID tenantId) {
        TenantSubscription sub = subscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> subscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId))
                .orElse(null);

        SubscriptionPlan plan = sub != null
                ? planRepository.findById(sub.getPlanId()).orElse(null)
                : planRepository.findByCode("starter").orElse(null);

        SubscriptionStatus storedStatus = sub != null ? sub.getStatus() : SubscriptionStatus.ACTIVE;
        SubscriptionStatus effectiveStatus = storedStatus;

        if (sub != null && (storedStatus == SubscriptionStatus.ACTIVE || storedStatus == SubscriptionStatus.TRIALING)) {
            if (clock.instant().isAfter(sub.getCurrentPeriodEnd())) {
                effectiveStatus = SubscriptionStatus.EXPIRED;
            }
        }

        long activeProducts = productRepository.countByTenantIdAndStatus(tenantId, ProductStatus.ACTIVE);
        long activeUsers = membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE);

        Integer productLimit = plan != null ? plan.getProductLimit() : null;
        Integer userLimit = plan != null ? plan.getUserLimit() : null;

        Long daysRemaining = null;
        if (sub != null && sub.getCurrentPeriodEnd() != null) {
            long seconds = ChronoUnit.SECONDS.between(clock.instant(), sub.getCurrentPeriodEnd());
            daysRemaining = Math.max(0, seconds / (24 * 3600));
        }

        List<String> features = new ArrayList<>();
        if (plan != null && StringUtils.hasText(plan.getFeaturesSummary())) {
            features = Arrays.stream(plan.getFeaturesSummary().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
        }

        return TenantSubscriptionResponseDto.builder()
                .subscriptionId(sub != null ? sub.getId() : null)
                .planId(plan != null ? plan.getId() : null)
                .planName(plan != null ? plan.getName() : "Starter Plan")
                .planCode(plan != null ? plan.getCode() : "starter")
                .planDescription(plan != null ? plan.getDescription() : "Default tier")
                .price(plan != null ? plan.getPrice() : BigDecimal.ZERO)
                .currency(plan != null ? plan.getCurrency() : "USD")
                .billingInterval(plan != null ? plan.getBillingInterval() : BillingInterval.MONTHLY)
                .status(storedStatus)
                .effectiveStatus(effectiveStatus)
                .currentPeriodStart(sub != null ? sub.getCurrentPeriodStart() : clock.instant())
                .currentPeriodEnd(sub != null ? sub.getCurrentPeriodEnd() : clock.instant().plus(30, ChronoUnit.DAYS))
                .trialStart(sub != null ? sub.getTrialStart() : null)
                .trialEnd(sub != null ? sub.getTrialEnd() : null)
                .daysRemaining(daysRemaining)
                .cancelAtPeriodEnd(sub != null && sub.isCancelAtPeriodEnd())
                .productLimit(productLimit)
                .activeProductsCount(activeProducts)
                .productLimitReached(productLimit != null && activeProducts >= productLimit)
                .userLimit(userLimit)
                .activeUsersCount(activeUsers)
                .userLimitReached(userLimit != null && activeUsers >= userLimit)
                .branchLimit(plan != null ? plan.getBranchLimit() : 1)
                .features(features)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionHistoryDto> getTenantSubscriptionHistoryForShop(UUID tenantId) {
        List<SubscriptionHistory> historyList = historyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        return mapHistoryList(historyList);
    }

    // -------------------------------------------------------------------------
    // EXPIRY PROCESSING
    // -------------------------------------------------------------------------

    @Override
    public void processExpirations() {
        Instant now = clock.instant();
        List<TenantSubscription> expiring = subscriptionRepository.findByStatusInAndCurrentPeriodEndBefore(
                List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.TRIALING), now);

        if (expiring.isEmpty()) {
            return;
        }

        log.info("Processing {} expired subscriptions at timestamp {}", expiring.size(), now);

        for (TenantSubscription sub : expiring) {
            SubscriptionStatus prev = sub.getStatus();
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            recordHistory(sub, SubscriptionAction.EXPIRED, prev, SubscriptionStatus.EXPIRED,
                    sub.getPlanId(), sub.getPlanId(), sub.getCurrentPeriodEnd(), sub.getCurrentPeriodEnd(),
                    "Automatic expiration: period end date elapsed", null);

            logAudit(sub.getTenantId(), null, "SUBSCRIPTION_EXPIRED", "TENANT_SUBSCRIPTION", sub.getId(),
                    "Subscription marked expired after period end " + sub.getCurrentPeriodEnd());
        }
    }

    // -------------------------------------------------------------------------
    // INTERNAL HELPERS & MAPPERS
    // -------------------------------------------------------------------------

    private void logAudit(UUID tenantId, KuterUserPrincipal admin, String action, String entityType, UUID entityId, String details) {
        UUID actorId = admin != null ? admin.getUserId() : null;
        String actorEmail = admin != null ? admin.getEmail() : "system";
        auditService.logEvent(tenantId, actorId, actorEmail, action, entityType, entityId != null ? entityId.toString() : null, details, null);
    }

    private TenantSubscription findSubscriptionOrThrow(UUID id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant subscription not found with ID: " + id));
    }

    private AdminSubscriptionDetailDto loadAndMapAdminDetail(TenantSubscription sub) {
        Tenant tenant = tenantRepository.findById(sub.getTenantId()).orElse(null);
        SubscriptionPlan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        return mapToAdminDetailDto(sub, tenant, plan);
    }

    private AdminSubscriptionDetailDto mapToAdminDetailDto(TenantSubscription sub, Tenant tenant, SubscriptionPlan plan) {
        SubscriptionStatus effective = sub.getStatus();
        if ((sub.getStatus() == SubscriptionStatus.ACTIVE || sub.getStatus() == SubscriptionStatus.TRIALING)
                && clock.instant().isAfter(sub.getCurrentPeriodEnd())) {
            effective = SubscriptionStatus.EXPIRED;
        }

        long activeProducts = productRepository.countByTenantIdAndStatus(sub.getTenantId(), ProductStatus.ACTIVE);
        long activeUsers = membershipRepository.countByTenantIdAndStatus(sub.getTenantId(), MembershipStatus.ACTIVE);

        String modifierEmail = null;
        if (sub.getLastModifiedBy() != null) {
            modifierEmail = userRepository.findById(sub.getLastModifiedBy()).map(User::getEmail).orElse(null);
        }

        return AdminSubscriptionDetailDto.builder()
                .id(sub.getId())
                .tenantId(sub.getTenantId())
                .tenantName(tenant != null ? tenant.getName() : "Unknown Shop")
                .tenantCode(tenant != null ? tenant.getCode() : "unknown")
                .planId(plan != null ? plan.getId() : sub.getPlanId())
                .planName(plan != null ? plan.getName() : "Unknown Plan")
                .planCode(plan != null ? plan.getCode() : "unknown")
                .price(plan != null ? plan.getPrice() : BigDecimal.ZERO)
                .currency(plan != null ? plan.getCurrency() : "USD")
                .billingInterval(plan != null ? plan.getBillingInterval() : BillingInterval.MONTHLY)
                .status(sub.getStatus())
                .effectiveStatus(effective)
                .currentPeriodStart(sub.getCurrentPeriodStart())
                .currentPeriodEnd(sub.getCurrentPeriodEnd())
                .trialStart(sub.getTrialStart())
                .trialEnd(sub.getTrialEnd())
                .cancelAtPeriodEnd(sub.isCancelAtPeriodEnd())
                .cancelledAt(sub.getCancelledAt())
                .cancellationReason(sub.getCancellationReason())
                .suspendedAt(sub.getSuspendedAt())
                .suspensionReason(sub.getSuspensionReason())
                .notes(sub.getNotes())
                .lastModifiedBy(sub.getLastModifiedBy())
                .lastModifiedByEmail(modifierEmail)
                .isCurrent(sub.isCurrent())
                .version(sub.getVersion())
                .productLimit(plan != null ? plan.getProductLimit() : null)
                .activeProductsCount(activeProducts)
                .userLimit(plan != null ? plan.getUserLimit() : null)
                .activeUsersCount(activeUsers)
                .branchLimit(plan != null ? plan.getBranchLimit() : 1)
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }

    private SubscriptionPlanDto mapToPlanDto(SubscriptionPlan plan) {
        return SubscriptionPlanDto.builder()
                .id(plan.getId())
                .code(plan.getCode())
                .name(plan.getName())
                .description(plan.getDescription())
                .price(plan.getPrice())
                .currency(plan.getCurrency())
                .billingInterval(plan.getBillingInterval())
                .productLimit(plan.getProductLimit())
                .userLimit(plan.getUserLimit())
                .branchLimit(plan.getBranchLimit())
                .trialDays(plan.getTrialDays())
                .isPublic(plan.isPublic())
                .isActive(plan.isActive())
                .featuresSummary(plan.getFeaturesSummary())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private void recordHistory(TenantSubscription sub,
                               SubscriptionAction action,
                               SubscriptionStatus prevStatus,
                               SubscriptionStatus newStatus,
                               UUID prevPlanId,
                               UUID newPlanId,
                               Instant prevPeriodEnd,
                               Instant newPeriodEnd,
                               String reason,
                               UUID actorId) {
        SubscriptionHistory history = SubscriptionHistory.builder()
                .subscriptionId(sub.getId())
                .tenantId(sub.getTenantId())
                .planId(sub.getPlanId())
                .action(action)
                .previousStatus(prevStatus)
                .newStatus(newStatus)
                .previousPlanId(prevPlanId)
                .newPlanId(newPlanId)
                .previousPeriodEnd(prevPeriodEnd)
                .newPeriodEnd(newPeriodEnd)
                .reason(reason)
                .actorId(actorId)
                .build();

        historyRepository.save(history);
    }

    private List<SubscriptionHistoryDto> mapHistoryList(List<SubscriptionHistory> historyList) {
        if (historyList.isEmpty()) {
            return List.of();
        }

        List<UUID> planIds = historyList.stream()
                .flatMap(h -> java.util.stream.Stream.of(h.getPreviousPlanId(), h.getNewPlanId(), h.getPlanId()))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> planNameMap = planRepository.findAllById(planIds).stream()
                .collect(Collectors.toMap(SubscriptionPlan::getId, SubscriptionPlan::getName));

        List<UUID> actorIds = historyList.stream()
                .map(SubscriptionHistory::getActorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, User> actorMap = userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return historyList.stream().map(h -> {
            User actor = h.getActorId() != null ? actorMap.get(h.getActorId()) : null;
            return SubscriptionHistoryDto.builder()
                    .id(h.getId())
                    .subscriptionId(h.getSubscriptionId())
                    .tenantId(h.getTenantId())
                    .action(h.getAction())
                    .previousStatus(h.getPreviousStatus())
                    .newStatus(h.getNewStatus())
                    .previousPlanId(h.getPreviousPlanId())
                    .previousPlanName(h.getPreviousPlanId() != null ? planNameMap.get(h.getPreviousPlanId()) : null)
                    .newPlanId(h.getNewPlanId())
                    .newPlanName(h.getNewPlanId() != null ? planNameMap.get(h.getNewPlanId()) : null)
                    .previousPeriodEnd(h.getPreviousPeriodEnd())
                    .newPeriodEnd(h.getNewPeriodEnd())
                    .reason(h.getReason())
                    .actorId(h.getActorId())
                    .actorName(actor != null ? actor.getFullName() : null)
                    .actorEmail(actor != null ? actor.getEmail() : null)
                    .createdAt(h.getCreatedAt())
                    .build();
        }).toList();
    }
}
