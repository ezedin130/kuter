package com.kuter.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
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
import com.kuter.identity.dto.LoginRequest;
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockAdjustmentItemDto;
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CreateStockReceivingItemDto;
import com.kuter.onboarding.dto.ApproveOnboardingDto;
import com.kuter.onboarding.dto.CreateOnboardingRequestDto;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.VoidSaleRequestDto;
import com.kuter.subscriptions.BillingInterval;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionPlanRepository;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import com.kuter.catalog.ProductUnitConversionRepository;
import com.kuter.inventory.StockAdjustmentItemRepository;
import com.kuter.inventory.StockAdjustmentRepository;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.inventory.StockReceivingItemRepository;
import com.kuter.inventory.StockReceivingRepository;
import com.kuter.onboarding.TenantOnboardingRequestRepository;
import com.kuter.sales.SaleItemRepository;
import com.kuter.sales.SaleRepository;
import com.kuter.subscriptions.TenantSubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase Eleven Full-System End-to-End Integration Tests.
 * Validates cross-application workflows across the complete lifecycle:
 * Public Website -> Platform Admin Review & Approval -> Shop Setup ->
 * Dual-UOM Catalog -> Inbound Receiving -> Stock Adjustments -> Counter POS ->
 * Operational Printable Receipt -> Atomic Voiding -> Immutable Movement Ledger ->
 * Cashier Redaction -> Cross-Tenant Security Boundaries.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FullSystemEndToEndIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private UnitDefinitionRepository unitRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductUnitConversionRepository conversionRepository;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private StockReceivingRepository receivingRepository;

    @Autowired
    private StockReceivingItemRepository receivingItemRepository;

    @Autowired
    private StockAdjustmentRepository adjustmentRepository;

    @Autowired
    private StockAdjustmentItemRepository adjustmentItemRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private TenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private TenantOnboardingRequestRepository onboardingRepository;

    private User platformAdminUser;
    private String platformAdminToken;

    private void cleanupDatabase() {
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        movementRepository.deleteAll();
        receivingItemRepository.deleteAll();
        receivingRepository.deleteAll();
        adjustmentItemRepository.deleteAll();
        adjustmentRepository.deleteAll();
        balanceRepository.deleteAll();
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        subscriptionRepository.deleteAll();
        onboardingRepository.deleteAll();
        membershipRepository.deleteAll();
        tenantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        cleanupDatabase();

        // Ensure Starter Plan exists
        planRepository.findByCode("STARTER").orElseGet(() ->
                planRepository.save(SubscriptionPlan.builder()
                        .name("Starter Plan")
                        .code("STARTER")
                        .description("Starter plan for small hardware yards")
                        .price(new BigDecimal("49.00"))
                        .currency("USD")
                        .billingInterval(BillingInterval.MONTHLY)
                        .productLimit(200)
                        .userLimit(3)
                        .branchLimit(1)
                        .trialDays(14)
                        .isActive(true)
                        .isPublic(true)
                        .build()));

        // Ensure Platform Admin exists
        platformAdminUser = userRepository.findByEmail("admin.e2e@kuterplatform.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("admin.e2e@kuterplatform.com")
                        .passwordHash(passwordEncoder.encode("AdminPass123!"))
                        .fullName("E2E Platform SuperAdmin")
                        .status(UserStatus.ACTIVE)
                        .isPlatformAdmin(true)
                        .build()));

        KuterUserPrincipal adminPrincipal = new KuterUserPrincipal(platformAdminUser, Collections.emptyList());
        platformAdminToken = jwtService.generateToken(adminPrincipal);
    }

    @AfterEach
    void tearDown() {
        cleanupDatabase();
    }

    @Test
    @DisplayName("Complete E2E Business Workflow: Registration -> Approval -> Catalog -> Dual-UOM -> Receiving -> Sale -> Void -> Ledger -> Security")
    void shouldExecuteFullSystemEndToEndWorkflow() throws Exception {

        // =========================================================================
        // Step 1: Public visitor queries public subscription plans
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/public/subscription-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));

        // =========================================================================
        // Step 2: Visitor submits shop onboarding registration request
        // =========================================================================
        String shopName = "Sahara Building Materials " + UUID.randomUUID().toString().substring(0, 6);
        String ownerEmail = "owner." + UUID.randomUUID().toString().substring(0, 6) + "@saharaye.example";

        CreateOnboardingRequestDto registerDto = CreateOnboardingRequestDto.builder()
                .shopName(shopName)
                .preferredShopCode("sahara-mats-" + UUID.randomUUID().toString().substring(0, 4))
                .ownerFullName("Hassan Al-Sahara")
                .ownerEmail(ownerEmail)
                .ownerPhone("+1 555-098-7654")
                .currency("USD")
                .businessAddress("Yard 4, Industrial Zone, South Sector")
                .requestedPlanCode("starter")
                .applicantNotes("Large rebar and cement supplier")
                .build();

        MvcResult regResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/onboarding-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", notNullValue()))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andReturn();

        JsonNode regNode = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String referenceCode = regNode.path("data").path("referenceCode").asText();

        // =========================================================================
        // Step 3: Visitor tracks application status using reference code
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/onboarding-requests/status/" + referenceCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.referenceCode", is(referenceCode)))
                .andExpect(jsonPath("$.data.status", is("PENDING")))
                .andExpect(jsonPath("$.data.shopName", is(shopName)));

        // =========================================================================
        // Step 4 & 5: Platform Admin reviews list and finds request by reference code
        // =========================================================================
        MvcResult adminListResult = mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/onboarding-requests")
                        .param("status", "PENDING")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        JsonNode listNode = objectMapper.readTree(adminListResult.getResponse().getContentAsString());
        UUID onboardingId = null;
        for (JsonNode item : listNode.path("data").path("content")) {
            if (referenceCode.equals(item.path("referenceCode").asText())) {
                onboardingId = UUID.fromString(item.path("id").asText());
                break;
            }
        }
        assertThat(onboardingId).isNotNull();

        ApproveOnboardingDto approveDto = ApproveOnboardingDto.builder()
                .customShopCode("sahara-mats-approved-" + UUID.randomUUID().toString().substring(0, 4))
                .reviewerNotes("Verified trade license. Fully approved.")
                .build();

        MvcResult approveResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/admin/onboarding-requests/" + onboardingId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.tenantId", notNullValue()))
                .andReturn();

        JsonNode approveNode = objectMapper.readTree(approveResult.getResponse().getContentAsString());
        UUID tenantId = UUID.fromString(approveNode.path("data").path("tenantId").asText());

        // =========================================================================
        // Step 6 & 7: Shop Owner authenticates and acquires JWT with active tenant
        // Update password for test login
        // =========================================================================
        User ownerUser = userRepository.findByEmail(ownerEmail).orElseThrow();
        ownerUser.setPasswordHash(passwordEncoder.encode("SaharaOwner@1234"));
        userRepository.save(ownerUser);

        LoginRequest loginReq = new LoginRequest(ownerEmail, "SaharaOwner@1234");
        MvcResult loginResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.memberships", hasSize(1)))
                .andExpect(jsonPath("$.data.memberships[0].role", is("OWNER")))
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String ownerToken = loginNode.path("data").path("accessToken").asText();

        // Verify /auth/me returns identity & memberships
        mockMvc.perform(get(Constants.API_BASE_PATH + "/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is(ownerEmail)))
                .andExpect(jsonPath("$.data.memberships[0].tenantId", is(tenantId.toString())));

        // =========================================================================
        // Step 8: Verify Tenant Subscription Self-Inspection (/me/subscription)
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/me/subscription")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.planCode", is("starter")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.productLimit", is(500)));

        // =========================================================================
        // Step 9: Verify Shop Settings & Roster (/tenants/me & /tenants/me/members)
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/tenants/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is(shopName)));

        mockMvc.perform(get(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].role", is("OWNER")))
                .andExpect(jsonPath("$.data[0].email", is(ownerEmail)));

        // =========================================================================
        // Step 10: Fetch/Seed Tenant Units (/units)
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        UnitDefinition pcsUnit = unitRepository.findByTenantIdAndCode(tenantId, "PCS").orElseThrow();
        UnitDefinition bndlUnit = unitRepository.findByTenantIdAndCode(tenantId, "BNDL").orElseThrow();

        // =========================================================================
        // Step 11: Create Category and Product in the Catalog
        // =========================================================================
        CreateCategoryDto catDto = CreateCategoryDto.builder()
                .name("Structural Reinforcement Steel")
                .description("Deformed rebar and mild steel rods")
                .build();

        MvcResult catResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        UUID categoryId = UUID.fromString(objectMapper.readTree(catResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        CreateProductDto prodDto = CreateProductDto.builder()
                .sku("REBAR-12MM")
                .name("High-Yield Deformed Steel Rebar 12mm x 12m")
                .categoryId(categoryId)
                .baseUnitId(pcsUnit.getId())
                .sellingPrice(new BigDecimal("8.5000"))
                .costPrice(new BigDecimal("5.0000"))
                .reorderThreshold(new BigDecimal("50.0000"))
                .build();

        MvcResult prodResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is("REBAR-12MM")))
                .andReturn();

        UUID productId = UUID.fromString(objectMapper.readTree(prodResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // =========================================================================
        // Step 12: Configure Dual-UOM Conversion (1 Bundle = 50 Pieces)
        // =========================================================================
        CreateProductUnitConversionDto convDto = CreateProductUnitConversionDto.builder()
                .unitId(bndlUnit.getId())
                .conversionFactor(new BigDecimal("50.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products/" + productId + "/conversions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(convDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.toBaseFactor", is(50.0)));

        // =========================================================================
        // Step 13 & 14: Inbound Stock Receiving in Secondary Units (4 Bundles = 200 Pieces)
        // =========================================================================
        CreateStockReceivingDto receiptDto = CreateStockReceivingDto.builder()
                .supplierName("National Steel Mills Ltd")
                .deliveryNoteRef("DN-44012")
                .notes("Standard 12m length delivery received at East Gate")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(productId)
                                .receivedUnitId(bndlUnit.getId())
                                .receivedQuantity(new BigDecimal("4.0000")) // 4 bundles = 200 pcs
                                .unitCost(new BigDecimal("250.0000")) // $250/bundle = $5.00/pc
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify current balance is exactly 200.0000 pieces with moving WAC = $5.0000
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is(200.0)))
                .andExpect(jsonPath("$.data.movingAverageCost", is(5.0)));

        // =========================================================================
        // Step 15: Accountable Stock Adjustment (-10 pcs for breakage/bent steel)
        // =========================================================================
        CreateStockAdjustmentDto adjDto = CreateStockAdjustmentDto.builder()
                .reasonCode("BREAKAGE")
                .notes("Bent bar during offloading crane operation")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(productId)
                                .quantityChange(new BigDecimal("-10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(-10.0)))
                .andExpect(jsonPath("$.data.items[0].balanceAfter", is(190.0)));

        // =========================================================================
        // Step 16: Counter POS Checkout with Idempotency Protection
        // Sells: 2 Bundles (100 pcs) + 5 individual Pieces (5 pcs) = 105 pcs
        // Balance reduces: 190 - 105 = 85 pieces
        // =========================================================================
        String idempotencyKey = "pos-tx-e2e-" + UUID.randomUUID();

        CreateSaleRequestDto saleDto = CreateSaleRequestDto.builder()
                .customerName("Apex Contracting LLC")
                .customerPhone("+1 555-444-3322")
                .idempotencyKey(idempotencyKey)
                .items(List.of(
                        // Line 1: 2 Bundles @ $425 = $850 (100 pcs deducted)
                        CreateSaleItemRequestDto.builder()
                                .productId(productId)
                                .unitDefinitionId(bndlUnit.getId())
                                .quantity(new BigDecimal("2.0000"))
                                .unitPrice(new BigDecimal("425.0000"))
                                .discountAmount(BigDecimal.ZERO)
                                .build(),
                        // Line 2: 5 Pieces @ $8.50 = $42.50 (5 pcs deducted)
                        CreateSaleItemRequestDto.builder()
                                .productId(productId)
                                .unitDefinitionId(pcsUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .unitPrice(new BigDecimal("8.5000"))
                                .discountAmount(BigDecimal.ZERO)
                                .build()
                ))
                .notes("Site delivery batch 1")
                .build();

        MvcResult saleResult = mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalAmount", is(892.50)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andReturn();

        UUID saleId = UUID.fromString(objectMapper.readTree(saleResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Replaying the exact same idempotency key must return 200 OK without re-deducting stock
        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saleDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id", is(saleId.toString())));

        // Balance should be exactly 85 pieces (190 - 105)
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is(85.0)));

        // =========================================================================
        // Step 17: Printable Operational Receipt with Non-Payment Disclaimer
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/" + saleId + "/receipt")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.operationalDisclaimer", containsString("Not a Proof of Payment")))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.totalAmount", is(892.50)));

        // =========================================================================
        // Step 18: Atomic Void Workflow (Restores stock to 190.0000 pcs)
        // =========================================================================
        VoidSaleRequestDto voidDto = VoidSaleRequestDto.builder()
                .reason("Customer truck cancelled delivery due to bad weather")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("VOIDED")));

        // Stock must be restored to 190.0000 pcs
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentBalance", is(190.0)));

        // =========================================================================
        // Step 19: Immutable Stock Movement Ledger Queries
        // =========================================================================
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", notNullValue()))
                // 1 receipt (+200), 1 adjustment (-10), 1 sale (-105), 1 void reversal (+105) = 4 movements
                .andExpect(jsonPath("$.data.totalElements", is(4)));

        // =========================================================================
        // Step 20: Cashier RBAC & Financial Redaction
        // Create Cashier user in this tenant
        // =========================================================================
        User cashierUser = userRepository.save(User.builder()
                .email("cashier." + UUID.randomUUID().toString().substring(0, 6) + "@saharaye.example")
                .passwordHash(passwordEncoder.encode("CashierPass123!"))
                .fullName("Bilal Cashier")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(false)
                .build());

        TenantMembership cashierMembership = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantId)
                .userId(cashierUser.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        KuterUserPrincipal cashierPrincipal = new KuterUserPrincipal(cashierUser, List.of(cashierMembership));
        String cashierToken = jwtService.generateToken(cashierPrincipal);

        // Cashier attempts to VOID sale -> MUST be rejected with HTTP 403 Forbidden
        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidDto)))
                .andExpect(status().isForbidden());

        // Cashier queries sales summary -> Sensitive COGS and Gross Profit MUST be REDACTED (omitted from payload)
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSalesAmount", notNullValue()))
                .andExpect(jsonPath("$.data.totalCostOfGoodsSold").doesNotExist())
                .andExpect(jsonPath("$.data.grossProfit").doesNotExist())
                .andExpect(jsonPath("$.data.grossMarginPercentage").doesNotExist());

        // =========================================================================
        // Step 21: Cross-Tenant Security Boundary (IDOR / DOR Protection)
        // Another tenant cannot access Sahara's products, balance, sales, or members
        // =========================================================================
        Tenant foreignTenant = tenantRepository.save(Tenant.builder()
                .name("Foreign Competitor Hardware")
                .code("foreign-" + UUID.randomUUID().toString().substring(0, 6))
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        User foreignUser = userRepository.save(User.builder()
                .email("foreign.owner." + UUID.randomUUID().toString().substring(0, 6) + "@foreign.example")
                .passwordHash(passwordEncoder.encode("Foreign123!"))
                .fullName("Foreign Competitor")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(false)
                .build());

        TenantMembership foreignMembership = membershipRepository.save(TenantMembership.builder()
                .tenantId(foreignTenant.getId())
                .userId(foreignUser.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        KuterUserPrincipal foreignPrincipal = new KuterUserPrincipal(foreignUser, List.of(foreignMembership));
        String foreignToken = jwtService.generateToken(foreignPrincipal);

        // Foreign owner attempts to access Sahara's product directly -> 404 Not Found (scoped out)
        mockMvc.perform(get(Constants.API_BASE_PATH + "/products/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken)
                        .header("X-Tenant-ID", foreignTenant.getId().toString()))
                .andExpect(status().isNotFound());

        // Foreign owner attempts to inspect Sahara's stock balance -> 404 Not Found
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken)
                        .header("X-Tenant-ID", foreignTenant.getId().toString()))
                .andExpect(status().isNotFound());

        // Foreign owner attempts to spoof X-Tenant-ID with Sahara's tenantId -> 403 Forbidden!
        mockMvc.perform(get(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isForbidden());
    }
}
