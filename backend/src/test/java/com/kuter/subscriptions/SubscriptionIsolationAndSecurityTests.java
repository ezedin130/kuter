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
import com.kuter.subscriptions.dto.AssignSubscriptionDto;
import com.kuter.subscriptions.dto.ExtendSubscriptionDto;
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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionIsolationAndSecurityTests {

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
    private String ownerAToken;
    private String ownerBToken;
    private String cashierAToken;

    private Tenant tenantA;
    private Tenant tenantB;

    private User ownerA;
    private User ownerB;
    private User cashierA;

    private SubscriptionPlan standardPlan;
    private SubscriptionPlan enterprisePlan;

    private TenantSubscription subA;
    private TenantSubscription subB;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM stock_adjustment_items");
        jdbcTemplate.execute("DELETE FROM stock_adjustments");
        jdbcTemplate.execute("DELETE FROM stock_receiving_items");
        jdbcTemplate.execute("DELETE FROM stock_receivings");
        jdbcTemplate.execute("DELETE FROM product_unit_conversions");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM unit_definitions");
        jdbcTemplate.execute("DELETE FROM subscription_history");
        subscriptionRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM subscription_plans WHERE code NOT IN ('starter', 'growth', 'enterprise')");

        // 1. Platform Admin
        User adminUser = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .fullName("System Administrator")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = jwtService.generateToken(new KuterUserPrincipal(adminUser, Collections.emptyList()));

        // 2. Tenants
        tenantA = tenantRepository.save(Tenant.builder()
                .code("yard-alpha")
                .name("Alpha Materials Yard")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        tenantB = tenantRepository.save(Tenant.builder()
                .code("yard-beta")
                .name("Beta Supply Depot")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 3. Users and Memberships
        ownerA = userRepository.save(User.builder()
                .email("owner.a@alpha.example")
                .fullName("Alice Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memOwnerA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(ownerA.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());
        ownerAToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwnerA)));

        cashierA = userRepository.save(User.builder()
                .email("cashier.a@alpha.example")
                .fullName("Charlie Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memCashierA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());
        cashierAToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashierA)));

        ownerB = userRepository.save(User.builder()
                .email("owner.b@beta.example")
                .fullName("Bob Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memOwnerB = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantB.getId())
                .userId(ownerB.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());
        ownerBToken = jwtService.generateToken(new KuterUserPrincipal(ownerB, List.of(memOwnerB)));

        // 4. Plans (Seed plans from V3)
        standardPlan = planRepository.findByCode("starter").orElseThrow();
        enterprisePlan = planRepository.findByCode("enterprise").orElseThrow();

        // 5. Active Subscriptions
        Instant now = Instant.now();
        subA = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(tenantA.getId())
                .planId(standardPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now.minus(10, ChronoUnit.DAYS))
                .currentPeriodEnd(now.plus(20, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        subB = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(tenantB.getId())
                .planId(enterprisePlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(now.minus(5, ChronoUnit.DAYS))
                .currentPeriodEnd(now.plus(25, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());
    }

    @Test
    @DisplayName("Shop owner can inspect own subscription and usage details via /api/v1/me/subscription")
    void shopOwnerCanInspectOwnSubscriptionAndUsage() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.subscriptionId", is(subA.getId().toString())))
                .andExpect(jsonPath("$.data.planCode", is("starter")))
                .andExpect(jsonPath("$.data.planName", is("Starter")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.effectiveStatus", is("ACTIVE")))
                .andExpect(jsonPath("$.data.productLimit", is(500)))
                .andExpect(jsonPath("$.data.userLimit", is(3)))
                .andExpect(jsonPath("$.data.branchLimit", is(1)))
                .andExpect(jsonPath("$.data.activeUsersCount", is(2))) // Owner A + Cashier A
                .andExpect(jsonPath("$.data.activeProductsCount", is(0)))
                .andExpect(jsonPath("$.data.daysRemaining", greaterThanOrEqualTo(19)))
                .andExpect(jsonPath("$.data.features", notNullValue()));
    }

    @Test
    @DisplayName("Cross-tenant context tampering is forbidden when shop user supplies another tenant's header")
    void crossTenantContextTamperingForbidden() throws Exception {
        // Owner A tries to pass Tenant B's ID in X-Tenant-ID header
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Tenant subscription history is strictly isolated between tenants")
    void crossTenantSubscriptionHistoryStrictlyIsolated() throws Exception {
        // Record history for Tenant A
        historyRepository.save(SubscriptionHistory.builder()
                .subscriptionId(subA.getId())
                .tenantId(tenantA.getId())
                .planId(standardPlan.getId())
                .action(SubscriptionAction.ASSIGNED)
                .newStatus(SubscriptionStatus.ACTIVE)
                .newPlanId(standardPlan.getId())
                .reason("Initial assignment for Tenant A")
                .build());

        // Record history for Tenant B
        historyRepository.save(SubscriptionHistory.builder()
                .subscriptionId(subB.getId())
                .tenantId(tenantB.getId())
                .planId(enterprisePlan.getId())
                .action(SubscriptionAction.ASSIGNED)
                .newStatus(SubscriptionStatus.ACTIVE)
                .newPlanId(enterprisePlan.getId())
                .reason("Initial assignment for Tenant B")
                .build());

        // Owner A requests own history
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tenantId", is(tenantA.getId().toString())))
                .andExpect(jsonPath("$.data[0].reason", is("Initial assignment for Tenant A")));

        // Owner B requests own history
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].tenantId", is(tenantB.getId().toString())))
                .andExpect(jsonPath("$.data[0].reason", is("Initial assignment for Tenant B")));
    }

    @Test
    @DisplayName("Non-admin tenant users are blocked with 403 Forbidden on all platform-admin endpoints")
    void nonAdminCannotAccessPlatformAdminEndpoints() throws Exception {
        // 1. List all subscriptions
        mockMvc.perform(get("/api/v1/admin/subscriptions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isForbidden());

        // 2. Summary metrics
        mockMvc.perform(get("/api/v1/admin/subscriptions/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isForbidden());

        // 3. Assign subscription
        AssignSubscriptionDto assignDto = AssignSubscriptionDto.builder()
                .tenantId(tenantA.getId())
                .planId(enterprisePlan.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignDto)))
                .andExpect(status().isForbidden());

        // 4. Extend subscription
        ExtendSubscriptionDto extendDto = ExtendSubscriptionDto.builder()
                .additionalDays(30)
                .reason("Attempted unauthorized extension")
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + subA.getId() + "/extend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extendDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Platform administrator can inspect any tenant's subscription details and summary metrics")
    void platformAdminCanInspectAnyTenantAndSummaryMetrics() throws Exception {
        // 1. Platform admin gets Tenant A subscription
        mockMvc.perform(get("/api/v1/admin/subscriptions/tenant/" + tenantA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", is(tenantA.getId().toString())))
                .andExpect(jsonPath("$.data.tenantCode", is("yard-alpha")))
                .andExpect(jsonPath("$.data.planCode", is("starter")));

        // 2. Platform admin gets Tenant B subscription
        mockMvc.perform(get("/api/v1/admin/subscriptions/tenant/" + tenantB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", is(tenantB.getId().toString())))
                .andExpect(jsonPath("$.data.tenantCode", is("yard-beta")))
                .andExpect(jsonPath("$.data.planCode", is("enterprise")));

        // 3. Platform admin gets summary metrics
        // Starter ($49) + Enterprise ($279) = $328 total monthly revenue
        mockMvc.perform(get("/api/v1/admin/subscriptions/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSubscriptions", is(2)))
                .andExpect(jsonPath("$.data.activeCount", is(2)))
                .andExpect(jsonPath("$.data.trialingCount", is(0)))
                .andExpect(jsonPath("$.data.expiredCount", is(0)))
                .andExpect(jsonPath("$.data.estimatedMonthlyRevenue", is(328.0)));
    }

    @Test
    @DisplayName("Unauthenticated requests to protected endpoints return 401 Unauthorized while public catalog returns 200 OK")
    void unauthenticatedSecurityRules() throws Exception {
        // 1. /api/v1/me/subscription requires authentication
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription"))
                .andExpect(status().isUnauthorized());

        // 2. /api/v1/admin/subscriptions requires authentication
        mockMvc.perform(get("/api/v1/admin/subscriptions"))
                .andExpect(status().isUnauthorized());

        // 3. /api/v1/public/subscription-plans is publicly accessible without credentials
        mockMvc.perform(get(Constants.API_BASE_PATH + "/public/subscription-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }
}
