package com.kuter.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.common.Constants;
import com.kuter.config.JwtService;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.onboarding.OnboardingStatus;
import com.kuter.onboarding.TenantOnboardingRequest;
import com.kuter.onboarding.TenantOnboardingRequestRepository;
import com.kuter.subscriptions.BillingInterval;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionPlanRepository;
import com.kuter.subscriptions.SubscriptionStatus;
import com.kuter.subscriptions.TenantSubscription;
import com.kuter.subscriptions.TenantSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformOverviewAndStatusTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private TenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private TenantOnboardingRequestRepository onboardingRepository;

    private Tenant activeTenant;
    private SubscriptionPlan proPlan;
    private String adminToken;
    private String ownerToken;

    @BeforeEach
    void setUp() {
        subscriptionRepository.deleteAll();
        planRepository.deleteAll();
        onboardingRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Admin User
        User adminUser = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .fullName("Platform Administrator")
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());

        adminToken = jwtService.generateToken(new KuterUserPrincipal(adminUser, List.of()));

        // Create 2 Tenants: 1 ACTIVE, 1 SUSPENDED
        activeTenant = tenantRepository.save(Tenant.builder()
                .code("active-shop")
                .name("Active Shop Materials")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        Tenant suspendedTenant = tenantRepository.save(Tenant.builder()
                .code("suspended-shop")
                .name("Suspended Shop Materials")
                .status(TenantStatus.SUSPENDED)
                .currency("USD")
                .timezone("UTC")
                .build());

        // Shop Owner for activeTenant
        User owner = userRepository.save(User.builder()
                .email("owner@activeshop.example")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .fullName("Active Shop Owner")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memOwner = membershipRepository.save(TenantMembership.builder()
                .tenantId(activeTenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(memOwner)));

        // Pro Plan: $100.00 / month
        proPlan = planRepository.save(SubscriptionPlan.builder()
                .code("pro")
                .name("Pro Business")
                .price(new BigDecimal("100.0000"))
                .billingInterval(BillingInterval.MONTHLY)
                .currency("USD")
                .trialDays(14)
                .productLimit(1000)
                .userLimit(10)
                .branchLimit(3)
                .build());

        // Active subscription for activeTenant
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(activeTenant.getId())
                .planId(proPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now().minus(5, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now().plus(25, ChronoUnit.DAYS))
                .build());

        // 1 Pending onboarding request
        onboardingRepository.save(TenantOnboardingRequest.builder()
                .referenceCode("REQ-20261029-001")
                .shopName("New Inbound Shop")
                .ownerFullName("Alice Applicant")
                .ownerEmail("alice@newinbound.example")
                .requestedPlanCode("pro")
                .status(OnboardingStatus.PENDING)
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/admin/metrics/overview returns aggregate platform metrics")
    void shouldReturnPlatformOverviewMetrics() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/metrics/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalTenants", is(2)))
                .andExpect(jsonPath("$.data.activeTenants", is(1)))
                .andExpect(jsonPath("$.data.suspendedTenants", is(1)))
                .andExpect(jsonPath("$.data.pendingOnboardingRequests", is(1)))
                .andExpect(jsonPath("$.data.activeSubscriptions", is(1)))
                .andExpect(jsonPath("$.data.estimatedMonthlyRevenue", is(100.0)));
    }

    @Test
    @DisplayName("Platform Admin can query GET /api/v1/admin/tenants/{id}/status")
    void shouldReturnTenantStatusDetailForAdmin() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/tenants/" + activeTenant.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", is(activeTenant.getId().toString())))
                .andExpect(jsonPath("$.data.operationalStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.planCode", is("pro")))
                .andExpect(jsonPath("$.data.lifecycleStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.effectiveStatus", is("ACTIVE")));
    }

    @Test
    @DisplayName("Shop Owner can query GET /api/v1/tenants/me/status")
    void shouldReturnMyTenantStatusDetail() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/tenants/me/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", activeTenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", is(activeTenant.getId().toString())))
                .andExpect(jsonPath("$.data.operationalStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.planCode", is("pro")))
                .andExpect(jsonPath("$.data.lifecycleStatus", is("ACTIVE")));
    }

    @Test
    @DisplayName("Non-admin is forbidden from platform overview metrics")
    void shouldForbidShopUserFromAdminMetrics() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/metrics/overview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }
}
