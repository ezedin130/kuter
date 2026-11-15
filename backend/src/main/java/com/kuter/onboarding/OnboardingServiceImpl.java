package com.kuter.onboarding;

import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
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
import com.kuter.notification.EmailService;
import com.kuter.onboarding.dto.ApprovalResultDto;
import com.kuter.onboarding.dto.ApproveOnboardingDto;
import com.kuter.onboarding.dto.CreateOnboardingRequestDto;
import com.kuter.onboarding.dto.OnboardingDetailResponse;
import com.kuter.onboarding.dto.OnboardingPublicResponse;
import com.kuter.onboarding.dto.RejectOnboardingDto;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionPlanRepository;
import com.kuter.subscriptions.SubscriptionStatus;
import com.kuter.subscriptions.TenantSubscription;
import com.kuter.subscriptions.TenantSubscriptionRepository;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of OnboardingService coordinating public registration requests,
 * state validation, and transactional tenant provisioning upon administrative approval.
 */
@Service
public class OnboardingServiceImpl implements OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingServiceImpl.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantOnboardingRequestRepository requestRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CategoryRepository categoryRepository;
    private final UnitDefinitionRepository unitRepository;

    public OnboardingServiceImpl(TenantOnboardingRequestRepository requestRepository,
                                 TenantRepository tenantRepository,
                                 UserRepository userRepository,
                                 TenantMembershipRepository membershipRepository,
                                 SubscriptionPlanRepository subscriptionPlanRepository,
                                 TenantSubscriptionRepository tenantSubscriptionRepository,
                                 PasswordEncoder passwordEncoder,
                                 EmailService emailService,
                                 CategoryRepository categoryRepository,
                                 UnitDefinitionRepository unitRepository) {
        this.requestRepository = requestRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    @Transactional
    public OnboardingPublicResponse submitRequest(CreateOnboardingRequestDto dto, HttpServletRequest httpRequest) {
        String email = dto.getOwnerEmail().trim().toLowerCase();
        String shopName = dto.getShopName().trim();

        // 1. Check duplicate pending requests for the same applicant email
        if (requestRepository.existsByOwnerEmailIgnoreCaseAndStatus(email, OnboardingStatus.PENDING)) {
            throw new ConflictException("A pending registration request for email '" + email + "' already exists. Please wait for administrator approval.");
        }

        // 2. Check duplicate registered user account
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with email '" + email + "' already exists. Please sign in to your existing account.");
        }

        // 3. Check duplicate shop name
        if (tenantRepository.existsByNameIgnoreCase(shopName)) {
            throw new ConflictException("A registered shop with name '" + shopName + "' already exists. Please choose a different shop name.");
        }

        // 4. Check preferred shop code if supplied
        String preferredShopCode = null;
        if (StringUtils.hasText(dto.getPreferredShopCode())) {
            preferredShopCode = slugify(dto.getPreferredShopCode());
            if (tenantRepository.existsByCodeIgnoreCase(preferredShopCode)) {
                throw new ConflictException("The preferred shop code '" + preferredShopCode + "' is already in use. Please choose another code.");
            }
        }

        // 5. Validate and normalize Ethiopian mobile phone number
        String normalizedPhone = null;
        if (StringUtils.hasText(dto.getOwnerPhone())) {
            normalizedPhone = EthiopianPhoneNumberUtils.normalizeIfValid(dto.getOwnerPhone());
        }

        // 6. Validate password requirements
        String passwordHash = null;
        if (StringUtils.hasText(dto.getPassword())) {
            if (dto.getPassword().trim().length() < 8) {
                throw new BadRequestException("Password must be at least 8 characters long.");
            }
            passwordHash = passwordEncoder.encode(dto.getPassword().trim());
        }

        // 7. Validate requested subscription plan existence
        String planCode = dto.getRequestedPlanCode().trim().toLowerCase();
        Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findByCode(planCode.toLowerCase());
        if (planOpt.isEmpty()) {
            planOpt = subscriptionPlanRepository.findByCode(planCode.toUpperCase());
        }
        if (planOpt.isEmpty()) {
            planOpt = subscriptionPlanRepository.findByCode(planCode);
        }
        if (planOpt.isEmpty()) {
            throw new BadRequestException("The requested subscription plan '" + planCode + "' does not exist or is inactive.");
        }
        SubscriptionPlan plan = planOpt.get();
        if (!plan.isActive()) {
            throw new BadRequestException("The requested subscription plan '" + plan.getName() + "' is currently inactive.");
        }

        String referenceCode = generateReferenceCode();

        TenantOnboardingRequest request = TenantOnboardingRequest.builder()
                .referenceCode(referenceCode)
                .shopName(shopName)
                .ownerFullName(dto.getOwnerFullName().trim())
                .ownerEmail(email)
                .ownerPhone(normalizedPhone)
                .requestedPlanCode(planCode)
                .preferredShopCode(preferredShopCode)
                .businessAddress(dto.getBusinessAddress() != null ? dto.getBusinessAddress().trim() : null)
                .currency("ETB")
                .applicantNotes(dto.getApplicantNotes() != null ? dto.getApplicantNotes().trim() : null)
                .passwordHash(passwordHash)
                .status(OnboardingStatus.PENDING)
                .build();

        TenantOnboardingRequest saved = requestRepository.save(request);

        try {
            emailService.sendRegistrationReceivedEmail(saved.getOwnerEmail(), saved.getShopName(), saved.getReferenceCode());
            saved.setEmailSentAt(Instant.now());
            saved = requestRepository.save(saved);
        } catch (Exception ex) {
            log.warn("Failed to dispatch registration confirmation email for {}: {}", saved.getReferenceCode(), ex.getMessage());
        }

        log.info("New tenant onboarding request submitted: ref={}, shop='{}', email='{}'",
                referenceCode, shopName, email);

        return OnboardingPublicResponse.builder()
                .referenceCode(saved.getReferenceCode())
                .shopName(saved.getShopName())
                .ownerEmail(saved.getOwnerEmail())
                .requestedPlanCode(saved.getRequestedPlanCode())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingPublicResponse getPublicStatus(String referenceCode) {
        TenantOnboardingRequest request = requestRepository.findByReferenceCode(referenceCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding request with reference '" + referenceCode + "' not found"));

        return OnboardingPublicResponse.builder()
                .referenceCode(request.getReferenceCode())
                .shopName(request.getShopName())
                .ownerEmail(request.getOwnerEmail())
                .requestedPlanCode(request.getRequestedPlanCode())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OnboardingDetailResponse> listRequests(OnboardingStatus status, String search, Pageable pageable) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        Page<TenantOnboardingRequest> page;
        if (status == null && cleanSearch == null) {
            page = requestRepository.findAll(pageable);
        } else if (status != null && cleanSearch == null) {
            page = requestRepository.findAllByStatus(status, pageable);
        } else if (status == null) {
            page = requestRepository.searchRequests(cleanSearch, pageable);
        } else {
            page = requestRepository.searchRequestsWithStatus(status, cleanSearch, pageable);
        }

        return PageResponse.of(page, page.getContent().stream()
                .map(this::mapToDetailResponse)
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingDetailResponse getRequestById(UUID id) {
        TenantOnboardingRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding request not found with ID: " + id));

        return mapToDetailResponse(request);
    }

    @Override
    @Transactional
    public ApprovalResultDto approveRequest(UUID id,
                                            ApproveOnboardingDto dto,
                                            KuterUserPrincipal adminPrincipal,
                                            HttpServletRequest httpRequest) {
        TenantOnboardingRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding request not found with ID: " + id));

        // Enforce state transitions: only PENDING requests can be approved
        if (request.getStatus() != OnboardingStatus.PENDING) {
            throw new BadRequestException("Cannot approve onboarding request in status: " + request.getStatus());
        }

        // 1. Determine unique tenant slug / code
        String tenantCode;
        if (dto != null && StringUtils.hasText(dto.getCustomShopCode())) {
            tenantCode = slugify(dto.getCustomShopCode());
        } else if (StringUtils.hasText(request.getPreferredShopCode())) {
            tenantCode = slugify(request.getPreferredShopCode());
        } else {
            tenantCode = slugify(request.getShopName());
        }

        tenantCode = resolveUniqueTenantCode(tenantCode);

        // 2. Create the Tenant
        Tenant tenant = Tenant.builder()
                .name(request.getShopName())
                .code(tenantCode)
                .currency(request.getCurrency())
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build();

        tenant = tenantRepository.save(tenant);

        // 3. Resolve or Create the Owner User Account
        String ownerEmail = request.getOwnerEmail().toLowerCase();
        Optional<User> existingUserOpt = userRepository.findByEmail(ownerEmail);
        User ownerUser;

        String passwordHash = StringUtils.hasText(request.getPasswordHash())
                ? request.getPasswordHash()
                : passwordEncoder.encode("Admin@123456");

        if (existingUserOpt.isPresent()) {
            ownerUser = existingUserOpt.get();
            if (StringUtils.hasText(request.getPasswordHash())) {
                ownerUser.setPasswordHash(request.getPasswordHash());
                ownerUser = userRepository.save(ownerUser);
            }
            log.info("Linking existing user '{}' as owner of new tenant '{}'", ownerEmail, tenantCode);
        } else {
            ownerUser = User.builder()
                    .email(ownerEmail)
                    .fullName(request.getOwnerFullName())
                    .phoneNumber(request.getOwnerPhone())
                    .passwordHash(passwordHash)
                    .status(UserStatus.ACTIVE)
                    .isPlatformAdmin(false)
                    .build();
            ownerUser = userRepository.save(ownerUser);
            log.info("Created new user '{}' for tenant owner of '{}'", ownerEmail, tenantCode);
        }

        // 4. Create the TenantMembership as OWNER
        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(ownerUser.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build();
        membershipRepository.save(membership);

        // 5. Initialize default inventory configuration (units and categories)
        initializeDefaultInventoryConfiguration(tenant.getId());

        // 6. Create the TenantSubscription for requested plan
        Optional<SubscriptionPlan> planOpt = subscriptionPlanRepository.findByCode(request.getRequestedPlanCode().toLowerCase());
        if (planOpt.isEmpty()) {
            // Fallback to default starter plan
            planOpt = subscriptionPlanRepository.findByCode("starter");
        }

        if (planOpt.isPresent()) {
            SubscriptionPlan plan = planOpt.get();
            Instant now = Instant.now();
            TenantSubscription subscription = TenantSubscription.builder()
                    .tenantId(tenant.getId())
                    .planId(plan.getId())
                    .status(SubscriptionStatus.ACTIVE)
                    .currentPeriodStart(now)
                    .currentPeriodEnd(now.plus(30, ChronoUnit.DAYS))
                    .cancelAtPeriodEnd(false)
                    .build();
            tenantSubscriptionRepository.save(subscription);
        }

        // 6. Update the Onboarding Request state
        request.setStatus(OnboardingStatus.APPROVED);
        request.setReviewerId(adminPrincipal != null ? adminPrincipal.getUserId() : null);
        request.setReviewerNotes(dto != null ? dto.getReviewerNotes() : null);
        request.setReviewedAt(Instant.now());
        request.setCreatedTenantId(tenant.getId());

        // Send approval email
        try {
            emailService.sendShopApprovalEmail(
                    ownerUser.getEmail(),
                    request.getOwnerFullName(),
                    tenant.getName(),
                    tenant.getCode(),
                    "http://localhost:5173/login"
            );
            request.setApprovalEmailSentAt(Instant.now());
        } catch (Exception ex) {
            log.warn("Failed to dispatch shop approval email for {}: {}", request.getReferenceCode(), ex.getMessage());
        }

        requestRepository.save(request);

        log.info("[ONBOARDING_APPROVED] requestId={} ref={} tenantCode='{}'",
                request.getId(), request.getReferenceCode(), tenant.getCode());

        return ApprovalResultDto.builder()
                .requestId(request.getId())
                .referenceCode(request.getReferenceCode())
                .tenantId(tenant.getId())
                .tenantCode(tenant.getCode())
                .tenantName(tenant.getName())
                .ownerEmail(ownerUser.getEmail())
                .status(request.getStatus().name())
                .subscriptionPlanCode(request.getRequestedPlanCode())
                .build();
    }

    @Override
    @Transactional
    public OnboardingDetailResponse rejectRequest(UUID id,
                                                  RejectOnboardingDto dto,
                                                  KuterUserPrincipal adminPrincipal,
                                                  HttpServletRequest httpRequest) {
        TenantOnboardingRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Onboarding request not found with ID: " + id));

        if (request.getStatus() != OnboardingStatus.PENDING) {
            throw new BadRequestException("Cannot reject onboarding request in status: " + request.getStatus());
        }

        request.setStatus(OnboardingStatus.REJECTED);
        request.setReviewerId(adminPrincipal != null ? adminPrincipal.getUserId() : null);
        request.setReviewerNotes(dto.getReason().trim());
        request.setReviewedAt(Instant.now());
        TenantOnboardingRequest updated = requestRepository.save(request);

        log.info("[ONBOARDING_REJECTED] requestId={} ref={} reason='{}'",
                request.getId(), request.getReferenceCode(), dto.getReason());

        return mapToDetailResponse(updated);
    }

    private OnboardingDetailResponse mapToDetailResponse(TenantOnboardingRequest req) {
        return OnboardingDetailResponse.builder()
                .id(req.getId())
                .referenceCode(req.getReferenceCode())
                .shopName(req.getShopName())
                .ownerFullName(req.getOwnerFullName())
                .ownerEmail(req.getOwnerEmail())
                .ownerPhone(req.getOwnerPhone())
                .requestedPlanCode(req.getRequestedPlanCode())
                .preferredShopCode(req.getPreferredShopCode())
                .businessAddress(req.getBusinessAddress())
                .currency(req.getCurrency())
                .applicantNotes(req.getApplicantNotes())
                .status(req.getStatus())
                .reviewerId(req.getReviewerId())
                .reviewerNotes(req.getReviewerNotes())
                .reviewedAt(req.getReviewedAt())
                .createdTenantId(req.getCreatedTenantId())
                .createdAt(req.getCreatedAt())
                .build();
    }

    private String generateReferenceCode() {
        for (int i = 0; i < 10; i++) {
            int randomInt = RANDOM.nextInt(90000) + 10000;
            String code = "REQ-2026-" + randomInt;
            if (requestRepository.findByReferenceCode(code).isEmpty()) {
                return code;
            }
        }
        return "REQ-2026-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String resolveUniqueTenantCode(String baseCode) {
        String code = baseCode;
        if (!tenantRepository.existsByCode(code)) {
            return code;
        }

        for (int i = 1; i <= 50; i++) {
            String candidate = baseCode + "-" + i;
            if (!tenantRepository.existsByCode(candidate)) {
                return candidate;
            }
        }

        return baseCode + "-" + UUID.randomUUID().toString().substring(0, 6);
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

    private void initializeDefaultInventoryConfiguration(UUID tenantId) {
        if (unitRepository.findAllByTenantId(tenantId).isEmpty()) {
            List<UnitDefinition> defaultUnits = List.of(
                    UnitDefinition.builder().tenantId(tenantId).code("PCS").name("Piece").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("BAG").name("50kg Bag").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("KG").name("Kilogram").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("MTR").name("Meter").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("SQM").name("Square Meter").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("BOX").name("Box / Carton").build(),
                    UnitDefinition.builder().tenantId(tenantId).code("BNDL").name("Bundle").build()
            );
            unitRepository.saveAll(defaultUnits);
        }

        if (categoryRepository.findAllByTenantId(tenantId).isEmpty()) {
            List<Category> defaultCategories = List.of(
                    Category.builder().tenantId(tenantId).name("Cement & Aggregates").description("Standard cement, sand, gravel").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Steel & Reinforcement").description("Rebars, wire mesh, iron sheets").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Timber & Wood").description("Planks, poles, plywood").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Roofing & Ceilings").description("Corrugated iron sheets, gypsum, tiles").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Paints & Chemical Admixtures").description("Emulsion, gloss, waterproofers").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Plumbing & Electrical").description("Pipes, fittings, cables, switches").active(true).build(),
                    Category.builder().tenantId(tenantId).name("Hardware & Fasteners").description("Nails, screws, hinges, tools").active(true).build()
            );
            categoryRepository.saveAll(defaultCategories);
        }
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
