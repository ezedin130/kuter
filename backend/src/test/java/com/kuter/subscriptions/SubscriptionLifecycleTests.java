package com.kuter.subscriptions;

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
import com.kuter.subscriptions.dto.ActivateSubscriptionDto;
import com.kuter.subscriptions.dto.AssignSubscriptionDto;
import com.kuter.subscriptions.dto.CancelSubscriptionDto;
import com.kuter.subscriptions.dto.ChangePlanDto;
import com.kuter.subscriptions.dto.ExtendSubscriptionDto;
import com.kuter.subscriptions.dto.ReactivateSubscriptionDto;
import com.kuter.subscriptions.dto.StartTrialDto;
import com.kuter.subscriptions.dto.SuspendSubscriptionDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
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
class SubscriptionLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private TenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionHistoryRepository historyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String shopOwnerToken;
    private Tenant testTenant;
    private SubscriptionPlan starterPlan;
    private SubscriptionPlan growthPlan;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM subscription_history");
        jdbcTemplate.execute("DELETE FROM tenant_subscriptions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");

        // Retain seed plans
        starterPlan = planRepository.findByCode("starter").orElseThrow();
        growthPlan = planRepository.findByCode("growth").orElseThrow();

        // 1. Setup Platform Admin
        User admin = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .fullName("Platform Administrator")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = jwtService.generateToken(new KuterUserPrincipal(admin, List.of()));

        // 2. Setup Shop Tenant & Owner
        testTenant = tenantRepository.save(Tenant.builder()
                .code("delta-blocks")
                .name("Delta Blocks & Pavers")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        User shopOwner = userRepository.save(User.builder()
                .email("owner@deltablocks.com")
                .fullName("Dave Delta")
                .passwordHash(passwordEncoder.encode("OwnerPass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership membership = membershipRepository.save(TenantMembership.builder()
                .tenantId(testTenant.getId())
                .userId(shopOwner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());
        shopOwnerToken = jwtService.generateToken(new KuterUserPrincipal(shopOwner, List.of(membership)));
    }

    @Test
    @DisplayName("Admin assigns active subscription to tenant")
    void testAssignSubscription_Active() throws Exception {
        AssignSubscriptionDto dto = AssignSubscriptionDto.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .periodDays(30)
                .notes("New merchant onboarding")
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", is(testTenant.getId().toString())))
                .andExpect(jsonPath("$.data.planCode", is("starter")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.currentPeriodEnd", notNullValue()));

        List<TenantSubscription> subs = subscriptionRepository.findAllByTenantId(testTenant.getId());
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subs.get(0).isCurrent()).isTrue();

        List<SubscriptionHistory> history = historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(subs.get(0).getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(SubscriptionAction.ASSIGNED);
    }

    @Test
    @DisplayName("Admin assigns trialing subscription with custom trial days")
    void testAssignSubscription_Trialing() throws Exception {
        AssignSubscriptionDto dto = AssignSubscriptionDto.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.TRIALING)
                .trialDays(14)
                .notes("14-day growth trial")
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status", is("TRIALING")))
                .andExpect(jsonPath("$.data.trialStart", notNullValue()))
                .andExpect(jsonPath("$.data.trialEnd", notNullValue()));

        TenantSubscription sub = subscriptionRepository.findByTenantIdAndIsCurrentTrue(testTenant.getId()).orElseThrow();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        assertThat(sub.getTrialStart()).isNotNull();
        assertThat(sub.getTrialEnd()).isNotNull();
    }

    @Test
    @DisplayName("Admin can activate a trialing or expired subscription")
    void testActivateSubscription_Success() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.TRIALING)
                .currentPeriodStart(Instant.now().minus(14, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now().plus(1, ChronoUnit.DAYS))
                .trialStart(Instant.now().minus(14, ChronoUnit.DAYS))
                .trialEnd(Instant.now().plus(1, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        ActivateSubscriptionDto dto = new ActivateSubscriptionDto(30, "Trial converted to full subscription");

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        TenantSubscription updated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(updated.getTrialStart()).isNull();
    }

    @Test
    @DisplayName("Admin can switch a tenant's subscription plan")
    void testChangePlan_Success() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        ChangePlanDto dto = new ChangePlanDto(growthPlan.getId(), true, "Upgraded to Growth tier");

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/change-plan")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode", is("growth")));

        TenantSubscription updated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getPlanId()).isEqualTo(growthPlan.getId());

        List<SubscriptionHistory> history = historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(sub.getId());
        assertThat(history).anyMatch(h -> h.getAction() == SubscriptionAction.PLAN_CHANGED);
    }

    @Test
    @DisplayName("Admin can extend subscription period with mandatory reason")
    void testExtendSubscription_Success() throws Exception {
        Instant now = Instant.now();
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now)
                .currentPeriodEnd(now.plus(10, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        ExtendSubscriptionDto dto = new ExtendSubscriptionDto(15, null, "Courtesy extension granted by manager");

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/extend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        TenantSubscription updated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(updated.getCurrentPeriodEnd()).isAfter(now.plus(20, ChronoUnit.DAYS));

        List<SubscriptionHistory> history = historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(sub.getId());
        assertThat(history).anyMatch(h -> h.getAction() == SubscriptionAction.EXTENDED && h.getReason().contains("Courtesy extension"));
    }

    @Test
    @DisplayName("Extension without reason rejected with 400 Bad Request")
    void testExtendSubscription_MissingReason_ReturnsBadRequest() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(10, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        ExtendSubscriptionDto dto = new ExtendSubscriptionDto(15, null, ""); // blank reason

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/extend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin can suspend and reactivate subscription with reason")
    void testSuspendAndReactivate_Success() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        // 1. Suspend
        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuspendSubscriptionDto("Compliance review pending"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")))
                .andExpect(jsonPath("$.data.suspensionReason", is("Compliance review pending")));

        TenantSubscription suspended = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(suspended.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);

        // 2. Reactivate
        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/reactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ReactivateSubscriptionDto("Review cleared"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.suspensionReason", nullValue()));

        TenantSubscription reactivated = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(reactivated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Admin can cancel subscription immediately")
    void testCancelSubscription_Immediate_Success() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        CancelSubscriptionDto dto = new CancelSubscriptionDto("Merchant closing business", true);

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("CANCELLED")))
                .andExpect(jsonPath("$.data.cancelledAt", notNullValue()));

        TenantSubscription cancelled = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("Cannot activate a cancelled subscription - 400 Bad Request")
    void testInvalidTransition_ActivateCancelled_ReturnsBadRequest() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.CANCELLED)
                .currentPeriodStart(Instant.now().minus(30, ChronoUnit.DAYS))
                .currentPeriodEnd(Instant.now())
                .cancelledAt(Instant.now())
                .cancellationReason("Terminated")
                .isCurrent(true)
                .build());

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivateSubscriptionDto(30, "Reactivate"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot activate a cancelled subscription")));
    }

    @Test
    @DisplayName("Shop owner cannot perform administrative subscription actions - 403 Forbidden")
    void testLifecycleMutations_NonAdmin_Forbidden() throws Exception {
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(starterPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SuspendSubscriptionDto("Self suspend"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Subscription history tracks the full administrative lifecycle audit trail")
    void testSubscriptionHistory_AuditTrail() throws Exception {
        // 1. Assign
        AssignSubscriptionDto assignDto = new AssignSubscriptionDto(testTenant.getId(), starterPlan.getId(), SubscriptionStatus.ACTIVE, 30, null, "Initial assignment");
        mockMvc.perform(post("/api/v1/admin/subscriptions/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignDto)))
                .andExpect(status().isCreated());

        TenantSubscription sub = subscriptionRepository.findByTenantIdAndIsCurrentTrue(testTenant.getId()).orElseThrow();

        // 2. Extend
        ExtendSubscriptionDto extendDto = new ExtendSubscriptionDto(10, null, "First extension");
        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/extend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extendDto)))
                .andExpect(status().isOk());

        // 3. Suspend
        SuspendSubscriptionDto suspendDto = new SuspendSubscriptionDto("Payment investigation");
        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/suspend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendDto)))
                .andExpect(status().isOk());

        // 4. Query history
        mockMvc.perform(get("/api/v1/admin/subscriptions/" + sub.getId() + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].action", is("SUSPENDED")))
                .andExpect(jsonPath("$.data[1].action", is("EXTENDED")))
                .andExpect(jsonPath("$.data[2].action", is("ASSIGNED")));
    }
}
