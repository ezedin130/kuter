package com.kuter.tenancy;

import com.kuter.common.BadRequestException;
import com.kuter.common.ConflictException;
import com.kuter.common.EthiopianPhoneNumberUtils;
import com.kuter.common.PageResponse;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.onboarding.OnboardingStatus;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionStatus;
import com.kuter.subscriptions.TenantSubscription;
import com.kuter.tenancy.dto.CreateTenantDto;
import com.kuter.tenancy.dto.PlatformOverviewMetricsDto;
import com.kuter.tenancy.dto.TenantDetailResponse;
import com.kuter.tenancy.dto.TenantStatusDetailDto;
import com.kuter.tenancy.dto.TenantSummaryResponse;
import com.kuter.tenancy.dto.UpdateTenantDto;
import com.kuter.tenancy.dto.UpdateTenantStatusDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TenantManagementService handling direct tenant creation,
 * status transitions, and self-tenant updates.
 */
@Service
public class TenantManagementServiceImpl implements TenantManagementService {

    private static final Logger log = LoggerFactory.getLogger(TenantManagementServiceImpl.class);

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.kuter.subscriptions.SubscriptionPlanRepository subscriptionPlanRepository;
    private final com.kuter.subscriptions.TenantSubscriptionRepository tenantSubscriptionRepository;
    private final com.kuter.onboarding.TenantOnboardingRequestRepository onboardingRequestRepository;
    private final com.kuter.subscriptions.SubscriptionService subscriptionService;
    private final com.kuter.subscriptions.EntitlementService entitlementService;
    private final java.time.Clock clock;

    public TenantManagementServiceImpl(TenantRepository tenantRepository,
                                       TenantMembershipRepository membershipRepository,
                                       UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       com.kuter.subscriptions.SubscriptionPlanRepository subscriptionPlanRepository,
                                       com.kuter.subscriptions.TenantSubscriptionRepository tenantSubscriptionRepository,
                                       com.kuter.onboarding.TenantOnboardingRequestRepository onboardingRequestRepository,
                                       com.kuter.subscriptions.SubscriptionService subscriptionService,
                                       com.kuter.subscriptions.EntitlementService entitlementService,
                                       java.time.Clock clock) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.onboardingRequestRepository = onboardingRequestRepository;
        this.subscriptionService = subscriptionService;
        this.entitlementService = entitlementService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TenantDetailResponse createTenant(CreateTenantDto dto,
                                             KuterUserPrincipal adminPrincipal,
                                             HttpServletRequest httpRequest) {
        String code = StringUtils.hasText(dto.getCode()) ? slugify(dto.getCode()) : slugify(dto.getName());

        if (tenantRepository.existsByCode(code)) {
            throw new ConflictException("Tenant with code '" + code + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(dto.getName().trim())
                .code(code)
                .businessRegistrationNumber(dto.getBusinessRegistrationNumber() != null ? dto.getBusinessRegistrationNumber().trim() : null)
                .currency("ETB")
                .timezone(StringUtils.hasText(dto.getTimezone()) ? dto.getTimezone().trim() : "UTC")
                .status(TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);

        // Resolve or create Owner User
        String ownerEmail = dto.getOwnerEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(ownerEmail);
        User ownerUser;

        if (userOpt.isPresent()) {
            ownerUser = userOpt.get();
        } else {
            String initialPassword = StringUtils.hasText(dto.getOwnerPassword())
                    ? dto.getOwnerPassword().trim()
                    : "Admin@123456";
            String normalizedPhone = StringUtils.hasText(dto.getOwnerPhone())
                    ? EthiopianPhoneNumberUtils.normalizeIfValid(dto.getOwnerPhone())
                    : null;
            ownerUser = User.builder()
                    .email(ownerEmail)
                    .fullName(dto.getOwnerFullName().trim())
                    .phoneNumber(normalizedPhone)
                    .passwordHash(passwordEncoder.encode(initialPassword))
                    .status(UserStatus.ACTIVE)
                    .isPlatformAdmin(false)
                    .build();
            ownerUser = userRepository.save(ownerUser);
        }

        // Create Owner Membership
        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(ownerUser.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        membershipRepository.save(membership);

        // Provision subscription (requested tier or default starter, 30 days active)
        UUID newTenantId = tenant.getId();
        String requestedPlan = StringUtils.hasText(dto.getPlanCode()) ? dto.getPlanCode().trim().toLowerCase() : "starter";
        subscriptionPlanRepository.findByCode(requestedPlan)
                .or(() -> subscriptionPlanRepository.findByCode("starter"))
                .ifPresent(plan -> {
                    java.time.Instant now = java.time.Instant.now();
                    com.kuter.subscriptions.TenantSubscription sub = com.kuter.subscriptions.TenantSubscription.builder()
                            .tenantId(newTenantId)
                            .planId(plan.getId())
                            .status(com.kuter.subscriptions.SubscriptionStatus.ACTIVE)
                            .currentPeriodStart(now)
                            .currentPeriodEnd(now.plus(30, java.time.temporal.ChronoUnit.DAYS))
                            .isCurrent(true)
                            .build();
                    tenantSubscriptionRepository.save(sub);
                });

        log.info("Tenant created directly: id={}, code='{}', owner='{}'",
                tenant.getId(), tenant.getCode(), ownerEmail);

        return mapToDetail(tenant, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TenantSummaryResponse> listTenants(TenantStatus status, String search, Pageable pageable) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<Tenant> page;
        if (status == null && cleanSearch == null) {
            page = tenantRepository.findAll(pageable);
        } else if (status != null && cleanSearch == null) {
            page = tenantRepository.findAllByStatus(status, pageable);
        } else if (status == null) {
            page = tenantRepository.searchTenants(cleanSearch, pageable);
        } else {
            page = tenantRepository.searchTenantsWithStatus(status, cleanSearch, pageable);
        }

        return PageResponse.of(page, page.getContent().stream()
                .map(this::mapToSummary)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetailResponse getTenantById(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));

        int memberCount = membershipRepository.findAllByTenantId(id).size();
        return mapToDetail(tenant, memberCount);
    }

    @Override
    @Transactional
    public TenantDetailResponse updateTenant(UUID id,
                                             UpdateTenantDto dto,
                                             KuterUserPrincipal adminPrincipal,
                                             HttpServletRequest httpRequest) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));

        tenant.setName(dto.getName().trim());
        if (dto.getBusinessRegistrationNumber() != null) {
            tenant.setBusinessRegistrationNumber(dto.getBusinessRegistrationNumber().trim());
        }
        tenant.setCurrency("ETB");
        if (StringUtils.hasText(dto.getTimezone())) {
            tenant.setTimezone(dto.getTimezone().trim());
        }

        Tenant updated = tenantRepository.save(tenant);

        log.info("[TENANT_UPDATED] tenantId={} code='{}'", tenant.getId(), tenant.getCode());

        int memberCount = membershipRepository.findAllByTenantId(id).size();
        return mapToDetail(updated, memberCount);
    }

    @Override
    @Transactional
    public TenantDetailResponse updateTenantStatus(UUID id,
                                                   UpdateTenantStatusDto dto,
                                                   KuterUserPrincipal adminPrincipal,
                                                   HttpServletRequest httpRequest) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));

        TenantStatus oldStatus = tenant.getStatus();
        tenant.setStatus(dto.getStatus());
        Tenant updated = tenantRepository.save(tenant);

        log.info("[TENANT_STATUS_CHANGED] tenantId={} from={} to={} reason='{}'",
                tenant.getId(), oldStatus, dto.getStatus(), dto.getReason());

        int memberCount = membershipRepository.findAllByTenantId(id).size();
        return mapToDetail(updated, memberCount);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetailResponse getMyTenant() {
        UUID currentTenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(currentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Current tenant shop not found with ID: " + currentTenantId));

        int memberCount = membershipRepository.findAllByTenantId(currentTenantId).size();
        return mapToDetail(tenant, memberCount);
    }

    @Override
    @Transactional
    public TenantDetailResponse updateMyTenant(UpdateTenantDto dto,
                                               KuterUserPrincipal principal,
                                               HttpServletRequest httpRequest) {
        UUID currentTenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(currentTenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Current tenant shop not found with ID: " + currentTenantId));

        tenant.setName(dto.getName().trim());
        if (dto.getBusinessRegistrationNumber() != null) {
            tenant.setBusinessRegistrationNumber(dto.getBusinessRegistrationNumber().trim());
        }
        tenant.setCurrency("ETB");
        if (StringUtils.hasText(dto.getTimezone())) {
            tenant.setTimezone(dto.getTimezone().trim());
        }

        Tenant updated = tenantRepository.save(tenant);

        log.info("[TENANT_SETTINGS_UPDATED] tenantId={} code='{}'", tenant.getId(), tenant.getCode());

        int memberCount = membershipRepository.findAllByTenantId(currentTenantId).size();
        return mapToDetail(updated, memberCount);
    }

    private TenantDetailResponse mapToDetail(Tenant tenant, int memberCount) {
        return TenantDetailResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .businessRegistrationNumber(tenant.getBusinessRegistrationNumber())
                .status(tenant.getStatus())
                .currency(tenant.getCurrency())
                .timezone(tenant.getTimezone())
                .memberCount(memberCount)
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }

    private TenantSummaryResponse mapToSummary(Tenant tenant) {
        return TenantSummaryResponse.builder()
                .id(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .status(tenant.getStatus())
                .currency(tenant.getCurrency())
                .createdAt(tenant.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformOverviewMetricsDto getPlatformOverviewMetrics() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long pendingOnboarding = onboardingRequestRepository.countByStatus(OnboardingStatus.PENDING);

        long totalSubs = tenantSubscriptionRepository.count();
        long activeSubs = tenantSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
        long trialingSubs = tenantSubscriptionRepository.countByStatus(SubscriptionStatus.TRIALING);
        long expiredSubs = tenantSubscriptionRepository.countByStatus(SubscriptionStatus.EXPIRED);
        long suspendedSubs = tenantSubscriptionRepository.countByStatus(SubscriptionStatus.SUSPENDED);
        long cancelledSubs = tenantSubscriptionRepository.countByStatus(SubscriptionStatus.CANCELLED);

        com.kuter.subscriptions.dto.SubscriptionSummaryDto subSummary = subscriptionService.getSubscriptionSummary();
        java.math.BigDecimal estimatedMrr = subSummary != null && subSummary.getEstimatedMonthlyRevenue() != null
                ? subSummary.getEstimatedMonthlyRevenue() : java.math.BigDecimal.ZERO;

        return PlatformOverviewMetricsDto.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .pendingOnboardingRequests(pendingOnboarding)
                .totalSubscriptions(totalSubs)
                .activeSubscriptions(activeSubs)
                .trialingSubscriptions(trialingSubs)
                .expiredSubscriptions(expiredSubs)
                .suspendedSubscriptions(suspendedSubs)
                .cancelledSubscriptions(cancelledSubs)
                .estimatedMonthlyRevenue(estimatedMrr)
                .currency("ETB")
                .generatedAt(Instant.now(clock))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TenantStatusDetailDto getTenantStatus(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        Optional<TenantSubscription> subOpt = tenantSubscriptionRepository.findByTenantIdAndIsCurrentTrue(tenantId)
                .or(() -> tenantSubscriptionRepository.findFirstByTenantIdOrderByCreatedAtDesc(tenantId));

        SubscriptionStatus effectiveStatus = null;
        SubscriptionPlan plan = null;
        Instant periodStart = null;
        Instant periodEnd = null;
        Instant trialStart = null;
        Instant trialEnd = null;
        long daysRemaining = 0;
        boolean cancelAtPeriodEnd = false;
        UUID subId = null;
        SubscriptionStatus lifecycleStatus = null;

        if (subOpt.isPresent()) {
            TenantSubscription sub = subOpt.get();
            subId = sub.getId();
            lifecycleStatus = sub.getStatus();
            effectiveStatus = entitlementService.getEffectiveStatus(tenantId);
            periodStart = sub.getCurrentPeriodStart();
            periodEnd = sub.getCurrentPeriodEnd();
            trialStart = sub.getTrialStart();
            trialEnd = sub.getTrialEnd();
            cancelAtPeriodEnd = sub.isCancelAtPeriodEnd();

            if (sub.getPlanId() != null) {
                plan = subscriptionPlanRepository.findById(sub.getPlanId()).orElse(null);
            }

            Instant now = Instant.now(clock);
            if (periodEnd != null && periodEnd.isAfter(now)) {
                daysRemaining = ChronoUnit.DAYS.between(now, periodEnd);
            }
        }

        return TenantStatusDetailDto.builder()
                .tenantId(tenant.getId())
                .code(tenant.getCode())
                .name(tenant.getName())
                .operationalStatus(tenant.getStatus())
                .createdAt(tenant.getCreatedAt())
                .onboardingReference(onboardingRequestRepository.findByCreatedTenantId(tenant.getId())
                        .map(com.kuter.onboarding.TenantOnboardingRequest::getReferenceCode)
                        .orElse(null))
                .subscriptionId(subId)
                .planCode(plan != null ? plan.getCode() : null)
                .planName(plan != null ? plan.getName() : null)
                .lifecycleStatus(lifecycleStatus)
                .effectiveStatus(effectiveStatus)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .trialStart(trialStart)
                .trialEnd(trialEnd)
                .daysRemaining(daysRemaining)
                .cancelAtPeriodEnd(cancelAtPeriodEnd)
                .build();
    }

    private String slugify(String input) {
        if (!StringUtils.hasText(input)) {
            return "shop-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String cleaned = input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
