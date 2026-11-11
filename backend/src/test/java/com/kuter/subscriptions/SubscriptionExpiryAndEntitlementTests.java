package com.kuter.subscriptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.catalog.dto.CreateProductDto;
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
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.subscriptions.dto.ChangePlanDto;
import com.kuter.subscriptions.dto.ExtendSubscriptionDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.AfterEach;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionExpiryAndEntitlementTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntitlementService entitlementService;

    @Autowired
    private SubscriptionService subscriptionService;

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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UnitDefinitionRepository unitRepository;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String shopOwnerToken;
    private Tenant testTenant;
    private User shopOwner;
    private SubscriptionPlan microPlan;
    private SubscriptionPlan growthPlan;
    private Category testCategory;
    private UnitDefinition baseUnitPcs;
    private Instant initialTime;

    @BeforeEach
    void setUp() {
        initialTime = Instant.parse("2026-11-01T12:00:00Z");
        Clock fixedClock = Clock.fixed(initialTime, ZoneOffset.UTC);
        entitlementService.setClock(fixedClock);
        subscriptionService.setClock(fixedClock);

        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM stock_adjustment_items");
        jdbcTemplate.execute("DELETE FROM stock_adjustments");
        jdbcTemplate.execute("DELETE FROM stock_receiving_items");
        jdbcTemplate.execute("DELETE FROM stock_receivings");
        jdbcTemplate.execute("DELETE FROM product_unit_conversions");
        jdbcTemplate.execute("DELETE FROM subscription_history");
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
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

        // 2. Test Tenant & Owner
        testTenant = tenantRepository.save(Tenant.builder()
                .code("expiry-depot")
                .name("Expiry Depot Materials")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        shopOwner = userRepository.save(User.builder()
                .email("owner@expirydepot.example")
                .fullName("Owen Depot")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership ownerMembership = membershipRepository.save(TenantMembership.builder()
                .tenantId(testTenant.getId())
                .userId(shopOwner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        shopOwnerToken = jwtService.generateToken(new KuterUserPrincipal(shopOwner, List.of(ownerMembership)));

        // 3. Plans
        // Micro plan: 2 products, 2 users
        microPlan = planRepository.save(SubscriptionPlan.builder()
                .code("micro")
                .name("Micro Plan")
                .description("Micro plan for strict limit testing")
                .price(new BigDecimal("29.0000"))
                .currency("USD")
                .billingInterval(BillingInterval.MONTHLY)
                .productLimit(2)
                .userLimit(2)
                .branchLimit(1)
                .featuresSummary("inventory,catalog")
                .trialDays(14)
                .isActive(true)
                .isPublic(true)
                .build());

        // Growth plan: seed plan from V3
        growthPlan = planRepository.findByCode("growth").orElseThrow();

        // 4. Catalog fixtures
        testCategory = categoryRepository.save(Category.builder()
                .tenantId(testTenant.getId())
                .name("Hardware Supplies")
                .active(true)
                .build());

        baseUnitPcs = unitRepository.save(UnitDefinition.builder()
                .tenantId(testTenant.getId())
                .code("PCS")
                .name("Pieces")
                .build());
    }

    @AfterEach
    void tearDown() {
        Clock utcClock = Clock.systemUTC();
        entitlementService.setClock(utcClock);
        subscriptionService.setClock(utcClock);
    }

    @Test
    @DisplayName("Effective status evaluates dynamically to EXPIRED when clock advances past period end")
    void effectiveStatusEvaluatesDynamicallyWithInjectedClock() {
        // Given an active subscription ending at initialTime + 5 days
        Instant periodStart = initialTime.minus(25, ChronoUnit.DAYS);
        Instant periodEnd = initialTime.plus(5, ChronoUnit.DAYS);

        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(microPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(periodStart)
                .currentPeriodEnd(periodEnd)
                .isCurrent(true)
                .build());

        // At initial time: effective status is ACTIVE
        assertThat(entitlementService.getEffectiveStatus(testTenant.getId())).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(entitlementService.isSubscriptionActive(testTenant.getId())).isTrue();

        // When clock is advanced 6 days (past period end)
        Clock futureClock = Clock.fixed(initialTime.plus(6, ChronoUnit.DAYS), ZoneOffset.UTC);
        entitlementService.setClock(futureClock);

        // Then effective status is EXPIRED even though DB status is still ACTIVE
        assertThat(entitlementService.getEffectiveStatus(testTenant.getId())).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(entitlementService.isSubscriptionActive(testTenant.getId())).isFalse();

        // Database record remains unchanged
        TenantSubscription refreshed = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("Product creation is blocked with 403 Forbidden when subscription is expired")
    void productCreationBlockedWhenSubscriptionIsExpired() throws Exception {
        // Given an expired subscription
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(60, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.minus(1, ChronoUnit.DAYS)) // expired 1 day ago
                .isCurrent(true)
                .build());

        CreateProductDto productDto = CreateProductDto.builder()
                .sku("WIRE-100M")
                .name("Galvanized Binding Wire 100m")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("5.0000"))
                .sellingPrice(new BigDecimal("8.5000"))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Subscription for this shop has expired")));
    }

    @Test
    @DisplayName("Sale creation is blocked with 403 Forbidden when subscription is expired")
    void saleCreationBlockedWhenSubscriptionIsExpired() throws Exception {
        // Given an expired subscription
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(60, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.minus(1, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        // Create product directly in repository to test sale enforcement
        Product prod = productRepository.save(Product.builder()
                .tenantId(testTenant.getId())
                .sku("PROD-TEST")
                .name("Direct Product")
                .baseUnitId(baseUnitPcs.getId())
                .categoryId(testCategory.getId())
                .sellingPrice(new BigDecimal("10.0000"))
                .costPrice(new BigDecimal("6.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(testTenant.getId())
                .productId(prod.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("6.0000"))
                .build());

        CreateSaleRequestDto saleDto = CreateSaleRequestDto.builder()
                .customerName("Expired Test Customer")
                .items(List.of(CreateSaleItemRequestDto.builder()
                        .productId(prod.getId())
                        .unitDefinitionId(baseUnitPcs.getId())
                        .quantity(new BigDecimal("2.0000"))
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Subscription for this shop has expired")));
    }

    @Test
    @DisplayName("Staff user addition is blocked with 403 Forbidden when subscription is expired")
    void memberAdditionBlockedWhenSubscriptionIsExpired() throws Exception {
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(60, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.minus(1, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        AddMemberDto addMemberDto = AddMemberDto.builder()
                .email("cashier@expirydepot.example")
                .fullName("Charlie Cashier")
                .role(TenantRole.CASHIER)
                .build();

        mockMvc.perform(post("/api/v1/tenants/" + testTenant.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Subscription for this shop has expired")));
    }

    @Test
    @DisplayName("Platform admin extending subscription restores tenant access immediately")
    void adminExtendingSubscriptionRestoresTenantAccess() throws Exception {
        // Given an expired subscription
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.EXPIRED)
                .currentPeriodStart(initialTime.minus(60, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.minus(2, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        CreateProductDto productDto = CreateProductDto.builder()
                .sku("ROPE-50M")
                .name("Nylon Rope 50m")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("12.0000"))
                .sellingPrice(new BigDecimal("18.0000"))
                .build();

        // 1. Initially product creation fails with 403
        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isForbidden());

        // 2. Admin extends the subscription by 30 days
        ExtendSubscriptionDto extendDto = ExtendSubscriptionDto.builder()
                .additionalDays(30)
                .reason("Customer paid via bank transfer; renewing for 30 days")
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/extend")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extendDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")));

        // 3. Product creation now succeeds with 201 Created
        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku", is("ROPE-50M")));
    }

    @Test
    @DisplayName("Product limit enforcement rejects creating more products than plan allows with 409 Conflict")
    void productLimitEnforcedWithConflict409() throws Exception {
        // Micro plan has productLimit = 2
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(microPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(1, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.plus(29, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        CreateProductDto p1 = CreateProductDto.builder()
                .sku("ITEM-1")
                .name("Item One")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("1.0000"))
                .sellingPrice(new BigDecimal("2.0000"))
                .build();

        CreateProductDto p2 = CreateProductDto.builder()
                .sku("ITEM-2")
                .name("Item Two")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("2.0000"))
                .sellingPrice(new BigDecimal("4.0000"))
                .build();

        CreateProductDto p3 = CreateProductDto.builder()
                .sku("ITEM-3")
                .name("Item Three (Should Fail)")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("3.0000"))
                .sellingPrice(new BigDecimal("6.0000"))
                .build();

        // 1st product: success
        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p1)))
                .andExpect(status().isCreated());

        // 2nd product: success
        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p2)))
                .andExpect(status().isCreated());

        // 3rd product: rejected with 409 Conflict
        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Catalog product limit of 2 reached")));
    }

    @Test
    @DisplayName("User seat limit enforcement rejects adding staff beyond plan limit with 409 Conflict")
    void userSeatLimitEnforcedWithConflict409() throws Exception {
        // Micro plan has userLimit = 2. testTenant already has 1 user (Owen Depot - OWNER).
        subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(microPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(1, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.plus(29, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        AddMemberDto m2 = AddMemberDto.builder()
                .email("user2@expirydepot.example")
                .fullName("Staff Two")
                .role(TenantRole.CASHIER)
                .build();

        AddMemberDto m3 = AddMemberDto.builder()
                .email("user3@expirydepot.example")
                .fullName("Staff Three (Should Fail)")
                .role(TenantRole.CASHIER)
                .build();

        // 2nd user: success
        mockMvc.perform(post("/api/v1/tenants/" + testTenant.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m2)))
                .andExpect(status().isCreated());

        // 3rd user: fails with 409 Conflict
        mockMvc.perform(post("/api/v1/tenants/" + testTenant.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m3)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Staff user seat limit of 2 reached")));
    }

    @Test
    @DisplayName("Plan downgrade preserves existing data without deletion and blocks creating products above new limit")
    void planDowngradePreservesExistingDataAndBlocksNewCreations() throws Exception {
        // Start on Growth plan (limit = 10)
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(growthPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(1, ChronoUnit.DAYS))
                .currentPeriodEnd(initialTime.plus(29, ChronoUnit.DAYS))
                .isCurrent(true)
                .build());

        // Create 3 products directly
        for (int i = 1; i <= 3; i++) {
            productRepository.save(Product.builder()
                    .tenantId(testTenant.getId())
                    .sku("SKU-DOWNDEMO-" + i)
                    .name("Downgrade Demo Product " + i)
                    .categoryId(testCategory.getId())
                    .baseUnitId(baseUnitPcs.getId())
                    .costPrice(BigDecimal.ONE)
                    .sellingPrice(BigDecimal.TEN)
                    .status(ProductStatus.ACTIVE)
                    .build());
        }

        assertThat(productRepository.countByTenantIdAndStatus(testTenant.getId(), ProductStatus.ACTIVE)).isEqualTo(3);

        // Platform admin downgrades tenant to Micro plan (limit = 2)
        ChangePlanDto changeDto = ChangePlanDto.builder()
                .newPlanId(microPlan.getId())
                .notes("Customer requested downgrade to Micro")
                .build();

        mockMvc.perform(post("/api/v1/admin/subscriptions/" + sub.getId() + "/change-plan")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planCode", is("micro")));

        // 1. Existing 3 products are STILL ACTIVE and preserved (no deletion on downgrade)
        assertThat(productRepository.countByTenantIdAndStatus(testTenant.getId(), ProductStatus.ACTIVE)).isEqualTo(3);

        mockMvc.perform(get(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(3)));

        // 2. Creating a 4th product is rejected because active count (3) >= limit (2)
        CreateProductDto p4 = CreateProductDto.builder()
                .sku("SKU-DOWNDEMO-4")
                .name("Downgrade Demo Product 4")
                .categoryId(testCategory.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(BigDecimal.ONE)
                .sellingPrice(BigDecimal.TEN)
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + shopOwnerToken)
                        .header(Constants.TENANT_ID_HEADER, testTenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(p4)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("Catalog product limit of 2 reached")));
    }

    @Test
    @DisplayName("Process expirations batch job updates database status and writes history audit")
    void processExpirationsJobUpdatesDatabaseStatusAndAuditHistory() {
        // Subscription currently stored as ACTIVE with currentPeriodEnd in the past
        Instant pastEnd = initialTime.minus(2, ChronoUnit.HOURS);
        TenantSubscription sub = subscriptionRepository.save(TenantSubscription.builder()
                .tenantId(testTenant.getId())
                .planId(microPlan.getId())
                .status(SubscriptionStatus.ACTIVE)
                .currentPeriodStart(initialTime.minus(30, ChronoUnit.DAYS))
                .currentPeriodEnd(pastEnd)
                .isCurrent(true)
                .build());

        // Execute background batch expiration processor
        subscriptionService.processExpirations();

        // Database status is now EXPIRED
        TenantSubscription refreshed = subscriptionRepository.findById(sub.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);

        // Immutable history ledger has EXPIRED record
        List<SubscriptionHistory> history = historyRepository.findBySubscriptionIdOrderByCreatedAtDesc(sub.getId());
        assertThat(history).isNotEmpty();
        assertThat(history.get(0).getAction()).isEqualTo(SubscriptionAction.EXPIRED);
        assertThat(history.get(0).getNewStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(history.get(0).getReason()).contains("Automatic expiration");
    }
}
