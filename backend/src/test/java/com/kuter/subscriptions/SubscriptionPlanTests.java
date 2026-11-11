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
import com.kuter.subscriptions.dto.CreateSubscriptionPlanDto;
import com.kuter.subscriptions.dto.UpdatePlanStatusDto;
import com.kuter.subscriptions.dto.UpdateSubscriptionPlanDto;
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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionPlanTests {

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

    @BeforeEach
    void setUp() {
        // Clean child tables first
        jdbcTemplate.execute("DELETE FROM subscription_history");
        jdbcTemplate.execute("DELETE FROM tenant_subscriptions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");

        // Retain seed plans, delete extra plans created by tests
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code NOT IN ('starter', 'growth', 'enterprise')");

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
                .code("alpha-materials")
                .name("Alpha Materials Yard")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        User shopOwner = userRepository.save(User.builder()
                .email("owner@alphamaterials.com")
                .fullName("Alice Owner")
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
    @DisplayName("Admin can create a new subscription plan with limits and pricing")
    void testCreatePlan_Success() throws Exception {
        CreateSubscriptionPlanDto dto = CreateSubscriptionPlanDto.builder()
                .code("pro-distributor")
                .name("Pro Distributor")
                .description("Designed for high volume building merchants")
                .price(new BigDecimal("199.0000"))
                .currency("USD")
                .billingInterval(BillingInterval.MONTHLY)
                .productLimit(5000)
                .userLimit(25)
                .branchLimit(5)
                .trialDays(21)
                .isPublic(true)
                .isActive(true)
                .featuresSummary("5,000 products, 25 seats, dual UOM, priority dispatch")
                .build();

        mockMvc.perform(post("/api/v1/subscription-plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("pro-distributor")))
                .andExpect(jsonPath("$.data.name", is("Pro Distributor")))
                .andExpect(jsonPath("$.data.price", is(199.0)))
                .andExpect(jsonPath("$.data.productLimit", is(5000)))
                .andExpect(jsonPath("$.data.userLimit", is(25)))
                .andExpect(jsonPath("$.data.trialDays", is(21)));

        assertThat(planRepository.findByCode("pro-distributor")).isPresent();
    }

    @Test
    @DisplayName("Rejects duplicate plan code with 409 Conflict")
    void testCreatePlan_DuplicateCode_ReturnsConflict() throws Exception {
        CreateSubscriptionPlanDto dto = CreateSubscriptionPlanDto.builder()
                .code("starter") // Seed code already exists
                .name("Starter Duplicate")
                .price(new BigDecimal("50.0000"))
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscription-plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)));
    }

    @Test
    @DisplayName("Rejects plan creation with negative price or blank name with 400 Bad Request")
    void testCreatePlan_ValidationFailure_ReturnsBadRequest() throws Exception {
        CreateSubscriptionPlanDto dto = CreateSubscriptionPlanDto.builder()
                .code("invalid-plan")
                .name("") // blank
                .price(new BigDecimal("-10.0000")) // negative
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscription-plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    @DisplayName("Shop owner cannot create plans - 403 Forbidden")
    void testCreatePlan_NonAdmin_Forbidden() throws Exception {
        CreateSubscriptionPlanDto dto = CreateSubscriptionPlanDto.builder()
                .code("unauthorized-plan")
                .name("Unauthorized Plan")
                .price(new BigDecimal("99.0000"))
                .billingInterval(BillingInterval.MONTHLY)
                .build();

        mockMvc.perform(post("/api/v1/subscription-plans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can update plan metadata and limits")
    void testUpdatePlan_Success() throws Exception {
        SubscriptionPlan plan = planRepository.findByCode("starter").orElseThrow();

        UpdateSubscriptionPlanDto updateDto = UpdateSubscriptionPlanDto.builder()
                .name("Starter Plus")
                .description("Updated starter description")
                .price(new BigDecimal("59.0000"))
                .currency("USD")
                .billingInterval(BillingInterval.MONTHLY)
                .productLimit(750)
                .userLimit(5)
                .branchLimit(1)
                .trialDays(14)
                .isPublic(true)
                .isActive(true)
                .featuresSummary("750 products, 5 seats")
                .build();

        mockMvc.perform(put("/api/v1/subscription-plans/" + plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Starter Plus")))
                .andExpect(jsonPath("$.data.price", is(59.0)))
                .andExpect(jsonPath("$.data.productLimit", is(750)))
                .andExpect(jsonPath("$.data.userLimit", is(5)));

        SubscriptionPlan updated = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Starter Plus");
        assertThat(updated.getProductLimit()).isEqualTo(750);
    }

    @Test
    @DisplayName("Admin can activate and deactivate plans")
    void testUpdatePlanStatus_Success() throws Exception {
        SubscriptionPlan plan = planRepository.findByCode("growth").orElseThrow();

        // Deactivate
        mockMvc.perform(patch("/api/v1/subscription-plans/" + plan.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePlanStatusDto(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(false)));

        SubscriptionPlan deactivated = planRepository.findById(plan.getId()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();

        // Reactivate
        mockMvc.perform(patch("/api/v1/subscription-plans/" + plan.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePlanStatusDto(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active", is(true)));
    }

    @Test
    @DisplayName("Cannot delete plan referenced by active or historical subscriptions")
    void testDeletePlan_ReferencedBySubscription_ReturnsConflict() throws Exception {
        SubscriptionPlan plan = planRepository.findByCode("starter").orElseThrow();

        // Create subscription referencing plan
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(plan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(Instant.now())
                .currentPeriodEnd(Instant.now().plus(30, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        mockMvc.perform(delete("/api/v1/subscription-plans/" + plan.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("referenced by existing tenant subscriptions")));
    }

    @Test
    @DisplayName("Admin can delete an unreferenced plan")
    void testDeletePlan_Unreferenced_Success() throws Exception {
        SubscriptionPlan unreferenced = planRepository.save(SubscriptionPlan.builder()
                .code("temp-tier")
                .name("Temporary Tier")
                .price(BigDecimal.TEN)
                .billingInterval(BillingInterval.MONTHLY)
                .isActive(true)
                .isPublic(false)
                .build());

        mockMvc.perform(delete("/api/v1/subscription-plans/" + unreferenced.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertThat(planRepository.findById(unreferenced.getId())).isEmpty();
    }

    @Test
    @DisplayName("Public plans endpoint returns only active and public plans without authentication")
    void testPublicPlans_PermitAll_ReturnsOnlyPublicActivePlans() throws Exception {
        // Create an internal/private plan
        planRepository.save(SubscriptionPlan.builder()
                .code("internal-partner")
                .name("Internal Partner Plan")
                .price(BigDecimal.ZERO)
                .billingInterval(BillingInterval.YEARLY)
                .isPublic(false) // PRIVATE
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/public/subscription-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[?(@.code == 'starter')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'growth')]").exists())
                .andExpect(jsonPath("$.data[?(@.code == 'internal-partner')]").doesNotExist());
    }
}
