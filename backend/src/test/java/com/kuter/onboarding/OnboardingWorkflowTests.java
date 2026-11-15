package com.kuter.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.config.JwtService;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.onboarding.dto.ApproveOnboardingDto;
import com.kuter.onboarding.dto.CreateOnboardingRequestDto;
import com.kuter.onboarding.dto.RejectOnboardingDto;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionPlanRepository;
import com.kuter.subscriptions.TenantSubscription;
import com.kuter.subscriptions.TenantSubscriptionRepository;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingWorkflowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantOnboardingRequestRepository requestRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private TenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private com.kuter.catalog.ProductUnitConversionRepository conversionRepository;

    @Autowired
    private com.kuter.catalog.ProductRepository productRepository;

    @Autowired
    private com.kuter.catalog.CategoryRepository categoryRepository;

    @Autowired
    private com.kuter.catalog.UnitDefinitionRepository unitDefinitionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User platformAdminUser;
    private User regularUser;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitDefinitionRepository.deleteAll();
        requestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Seed starter plan if not exists
        if (planRepository.findByCode("starter").isEmpty()) {
            planRepository.save(SubscriptionPlan.builder()
                    .code("starter")
                    .name("Starter")
                    .price(new BigDecimal("49.0000"))
                    .billingInterval(com.kuter.subscriptions.BillingInterval.MONTHLY)
                    .build());
        }

        platformAdminUser = userRepository.save(User.builder()
                .email("superadmin@kuterplatform.com")
                .fullName("Platform Administrator")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());

        regularUser = userRepository.save(User.builder()
                .email("regular@shop.com")
                .fullName("Regular User")
                .passwordHash(passwordEncoder.encode("UserPass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        KuterUserPrincipal adminPrincipal = new KuterUserPrincipal(platformAdminUser, Collections.emptyList());
        adminToken = jwtService.generateToken(adminPrincipal);

        KuterUserPrincipal regularPrincipal = new KuterUserPrincipal(regularUser, Collections.emptyList());
        userToken = jwtService.generateToken(regularPrincipal);
    }

    @Test
    @DisplayName("Public registration request creates PENDING request with unique reference code")
    void publicRegistrationSubmissionSuccess() throws Exception {
        CreateOnboardingRequestDto dto = CreateOnboardingRequestDto.builder()
                .shopName("Northstar Building Supply")
                .ownerFullName("Maya Lin")
                .ownerEmail("maya@northstar.example")
                .ownerPhone("+1 555 0182")
                .requestedPlanCode("starter")
                .preferredShopCode("northstar-building")
                .businessAddress("84 Foundry Road, Cedar Grove")
                .currency("USD")
                .applicantNotes("Looking forward to using Kuter")
                .build();

        mockMvc.perform(post("/api/v1/onboarding-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", containsString("REQ-2026-")))
                .andExpect(jsonPath("$.data.shopName", is("Northstar Building Supply")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        List<TenantOnboardingRequest> all = requestRepository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getOwnerEmail()).isEqualTo("maya@northstar.example");
        assertThat(all.get(0).getStatus()).isEqualTo(OnboardingStatus.PENDING);
    }

    @Test
    @DisplayName("Validation failures return 400 Bad Request with field details")
    void validationFailuresReturnBadRequest() throws Exception {
        CreateOnboardingRequestDto invalidDto = CreateOnboardingRequestDto.builder()
                .shopName("") // Empty
                .ownerFullName("")
                .ownerEmail("not-an-email")
                .requestedPlanCode("")
                .build();

        mockMvc.perform(post("/api/v1/onboarding-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details", notNullValue()));
    }

    @Test
    @DisplayName("Duplicate pending registration request for the same email returns 409 Conflict")
    void duplicatePendingRequestReturnsConflict() throws Exception {
        CreateOnboardingRequestDto dto1 = CreateOnboardingRequestDto.builder()
                .shopName("First Yard")
                .ownerFullName("John Builder")
                .ownerEmail("builder@materials.example")
                .requestedPlanCode("starter")
                .build();

        mockMvc.perform(post("/api/v1/onboarding-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated());

        CreateOnboardingRequestDto dto2 = CreateOnboardingRequestDto.builder()
                .shopName("Second Yard")
                .ownerFullName("John Builder")
                .ownerEmail("builder@materials.example") // Same email
                .requestedPlanCode("starter")
                .build();

        mockMvc.perform(post("/api/v1/onboarding-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Public status check returns status without leaking reviewer notes or admin identity")
    void publicStatusLookupReturnsCorrectStatusWithoutPrivateNotes() throws Exception {
        TenantOnboardingRequest request = requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-77889")
                .shopName("Public View Materials")
                .ownerFullName("Alice Stone")
                .ownerEmail("alice@stone.example")
                .requestedPlanCode("starter")
                .status(OnboardingStatus.PENDING)
                .reviewerNotes("CONFIDENTIAL: Verified commercial registry number.")
                .build());

        mockMvc.perform(get("/api/v1/onboarding-requests/status/REQ-2026-77889"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.referenceCode", is("REQ-2026-77889")))
                .andExpect(jsonPath("$.data.shopName", is("Public View Materials")))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.reviewerNotes").doesNotExist())
                .andExpect(jsonPath("$.data.reviewerId").doesNotExist());
    }

    @Test
    @DisplayName("Non-admin users cannot access administrative onboarding endpoints (403 Forbidden)")
    void unauthorizedUserCannotReviewOnboardingRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/onboarding-requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Platform administrator can list requests with pagination and status filtering")
    void platformAdminCanListAndFilterRequests() throws Exception {
        requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-0001")
                .shopName("Shop One")
                .ownerFullName("Owner One")
                .ownerEmail("one@example.com")
                .requestedPlanCode("starter")
                .status(OnboardingStatus.PENDING)
                .build());

        requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-0002")
                .shopName("Shop Two")
                .ownerFullName("Owner Two")
                .ownerEmail("two@example.com")
                .requestedPlanCode("starter")
                .status(OnboardingStatus.REJECTED)
                .build());

        mockMvc.perform(get("/api/v1/admin/onboarding-requests?status=PENDING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].referenceCode", is("REQ-2026-0001")));
    }

    @Test
    @DisplayName("Platform administrator approval transactionally creates Tenant, User, Owner Membership, and Subscription")
    void platformAdminApproveCreatesTenantOwnerAndSubscription() throws Exception {
        TenantOnboardingRequest request = requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-11223")
                .shopName("Summit Timber Yard")
                .ownerFullName("Maya Lin")
                .ownerEmail("maya@summittimber.example")
                .requestedPlanCode("starter")
                .preferredShopCode("summit-timber")
                .status(OnboardingStatus.PENDING)
                .build());

        ApproveOnboardingDto approveDto = ApproveOnboardingDto.builder()
                .customShopCode("summit-timber-yard")
                .reviewerNotes("Trade license verified by phone call.")
                .build();

        mockMvc.perform(post("/api/v1/admin/onboarding-requests/" + request.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantCode", is("summit-timber-yard")))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));

        // Verify Tenant was created
        Optional<Tenant> tenantOpt = tenantRepository.findByCode("summit-timber-yard");
        assertThat(tenantOpt).isPresent();
        Tenant tenant = tenantOpt.get();
        assertThat(tenant.getName()).isEqualTo("Summit Timber Yard");

        // Verify Owner User was created
        Optional<User> userOpt = userRepository.findByEmail("maya@summittimber.example");
        assertThat(userOpt).isPresent();
        User owner = userOpt.get();

        // Verify Tenant Membership as OWNER
        Optional<TenantMembership> membershipOpt = membershipRepository.findByTenantIdAndUserId(tenant.getId(), owner.getId());
        assertThat(membershipOpt).isPresent();
        assertThat(membershipOpt.get().getRole()).isEqualTo(TenantRole.OWNER);

        // Verify Subscription was provisioned
        List<TenantSubscription> subs = subscriptionRepository.findAll();
        assertThat(subs).anyMatch(s -> s.getTenantId().equals(tenant.getId()));

        // Verify Onboarding Request status updated
        TenantOnboardingRequest updatedReq = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updatedReq.getStatus()).isEqualTo(OnboardingStatus.APPROVED);
        assertThat(updatedReq.getReviewerNotes()).isEqualTo("Trade license verified by phone call.");
        assertThat(updatedReq.getCreatedTenantId()).isEqualTo(tenant.getId());
    }

    @Test
    @DisplayName("Platform administrator rejection updates status to REJECTED with recorded reason")
    void platformAdminRejectRecordsReasonAndUpdatesStatus() throws Exception {
        TenantOnboardingRequest request = requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-99112")
                .shopName("Rejected Supplies")
                .ownerFullName("Suspicious Applicant")
                .ownerEmail("suspicious@example.com")
                .requestedPlanCode("starter")
                .status(OnboardingStatus.PENDING)
                .build());

        RejectOnboardingDto rejectDto = RejectOnboardingDto.builder()
                .reason("Unable to verify merchant tax registration number.")
                .build();

        mockMvc.perform(post("/api/v1/admin/onboarding-requests/" + request.getId() + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("REJECTED")))
                .andExpect(jsonPath("$.data.reviewerNotes", is("Unable to verify merchant tax registration number.")));

        TenantOnboardingRequest updated = requestRepository.findById(request.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OnboardingStatus.REJECTED);
        assertThat(tenantRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Invalid state transition: attempting to approve an already rejected request returns 400 Bad Request")
    void invalidStateTransitionRejected() throws Exception {
        TenantOnboardingRequest rejected = requestRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-2026-44556")
                .shopName("Already Closed")
                .ownerFullName("Bob Hope")
                .ownerEmail("bob@hope.example")
                .requestedPlanCode("starter")
                .status(OnboardingStatus.REJECTED)
                .build());

        mockMvc.perform(post("/api/v1/admin/onboarding-requests/" + rejected.getId() + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot approve onboarding request in status: REJECTED")));
    }

    @Test
    @DisplayName("Public registration endpoint aliases and status routes are accessible without 403 Forbidden")
    void publicRegistrationAliasesAccessibleWithoutAuth() throws Exception {
        CreateOnboardingRequestDto dto1 = CreateOnboardingRequestDto.builder()
                .shopName("Addis Hardware Supply")
                .ownerFullName("Abebe Bikila")
                .ownerEmail("abebe@addishardware.et")
                .ownerPhone("+251911223344")
                .requestedPlanCode("starter")
                .currency("ETB")
                .password("StrongPass123!")
                .build();

        // 1. Test POST /api/v1/onboarding/requests
        mockMvc.perform(post("/api/v1/onboarding/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", notNullValue()));

        CreateOnboardingRequestDto dto2 = CreateOnboardingRequestDto.builder()
                .shopName("Bole Building Mart")
                .ownerFullName("Almaz Ayana")
                .ownerEmail("almaz@bolebuilding.et")
                .ownerPhone("+251922334455")
                .requestedPlanCode("starter")
                .currency("ETB")
                .password("StrongPass123!")
                .build();

        // 2. Test POST /api/v1/register
        mockMvc.perform(post("/api/v1/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", notNullValue()));

        CreateOnboardingRequestDto dto3 = CreateOnboardingRequestDto.builder()
                .shopName("Merkato Timber Center")
                .ownerFullName("Kenenisa Bekele")
                .ownerEmail("kenenisa@merkatotimber.et")
                .ownerPhone("+251933445566")
                .requestedPlanCode("starter")
                .currency("ETB")
                .password("StrongPass123!")
                .build();

        // 3. Test POST /register
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", notNullValue()));

        // 4. Test GET /api/v1/public/subscription-plans without auth
        mockMvc.perform(get("/api/v1/public/subscription-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 5. Test GET /public/subscription-plans without auth
        mockMvc.perform(get("/public/subscription-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
