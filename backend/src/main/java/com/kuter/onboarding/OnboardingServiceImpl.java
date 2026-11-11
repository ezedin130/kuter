package com.kuter.onboarding;

import com.kuter.audit.AuditService;
import com.kuter.common.BadRequestException;
import com.kuter.common.ConflictException;
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
    private final AuditService auditService;

    public OnboardingServiceImpl(TenantOnboardingRequestRepository requestRepository,
                                 TenantRepository tenantRepository,
                                 UserRepository userRepository,
                                 TenantMembershipRepository membershipRepository,
                                 SubscriptionPlanRepository subscriptionPlanRepository,
                                 TenantSubscriptionRepository tenantSubscriptionRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuditService auditService) {
        this.requestRepository = requestRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public OnboardingPublicResponse submitRequest(CreateOnboardingRequestDto dto, HttpServletRequest httpRequest) {
        String email = dto.getOwnerEmail().trim().toLowerCase();
        String shopName = dto.getShopName().trim();

        // Prevent duplicate pending requests for the same applicant email
        if (requestRepository.existsByOwnerEmailIgnoreCaseAndStatus(email, OnboardingStatus.PENDING)) {
            throw new ConflictException("A pending registration request for email '" + email + "' already exists.");
        }

        String referenceCode = generateReferenceCode();
        String planCode = dto.getRequestedPlanCode().trim().toLowerCase();

        TenantOnboardingRequest request = TenantOnboardingRequest.builder()
                .referenceCode(referenceCode)
                .shopName(shopName)
                .ownerFullName(dto.getOwnerFullName().trim())
                .ownerEmail(email)
                .ownerPhone(dto.getOwnerPhone() != null ? dto.getOwnerPhone().trim() : null)
                .requestedPlanCode(planCode)
                .preferredShopCode(dto.getPreferredShopCode() != null ? slugify(dto.getPreferredShopCode()) : null)
                .businessAddress(dto.getBusinessAddress() != null ? dto.getBusinessAddress().trim() : null)
                .currency(StringUtils.hasText(dto.getCurrency()) ? dto.getCurrency().trim().toUpperCase() : "USD")
                .applicantNotes(dto.getApplicantNotes() != null ? dto.getApplicantNotes().trim() : null)
                .status(OnboardingStatus.PENDING)
                .build();

        TenantOnboardingRequest saved = requestRepository.save(request);

        String ipAddress = extractClientIp(httpRequest);
        auditService.logEvent(null, null, email, "ONBOARDING_REQUEST_SUBMITTED",
                "ONBOARDING_REQUEST", saved.getId().toString(),
                "Submitted request for shop: " + shopName + ", ref: " + referenceCode, ipAddress);

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

        if (existingUserOpt.isPresent()) {
            ownerUser = existingUserOpt.get();
            log.info("Linking existing user '{}' as owner of new tenant '{}'", ownerEmail, tenantCode);
        } else {
            // Default secure initial password for approved applicant
            String temporaryPassword = "Admin@123456";
            ownerUser = User.builder()
                    .email(ownerEmail)
                    .fullName(request.getOwnerFullName())
                    .phoneNumber(request.getOwnerPhone())
                    .passwordHash(passwordEncoder.encode(temporaryPassword))
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

        // 5. Create the TenantSubscription for requested plan
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
        requestRepository.save(request);

        // 7. Record immutable audit log
        String ipAddress = extractClientIp(httpRequest);
        UUID adminId = adminPrincipal != null ? adminPrincipal.getUserId() : null;
        String adminEmail = adminPrincipal != null ? adminPrincipal.getEmail() : "system";

        auditService.logEvent(tenant.getId(), adminId, adminEmail, "ONBOARDING_REQUEST_APPROVED",
                "ONBOARDING_REQUEST", request.getId().toString(),
                "Approved onboarding request " + request.getReferenceCode() + ", created tenant: " + tenant.getCode(),
                ipAddress);

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

        String ipAddress = extractClientIp(httpRequest);
        UUID adminId = adminPrincipal != null ? adminPrincipal.getUserId() : null;
        String adminEmail = adminPrincipal != null ? adminPrincipal.getEmail() : "system";

        auditService.logEvent(null, adminId, adminEmail, "ONBOARDING_REQUEST_REJECTED",
                "ONBOARDING_REQUEST", request.getId().toString(),
                "Rejected onboarding request " + request.getReferenceCode() + ". Reason: " + dto.getReason(),
                ipAddress);

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
