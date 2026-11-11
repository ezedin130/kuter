package com.kuter.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.ProductUnitConversionRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryReportingAndIsolationTests {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitDefinitionRepository unitRepository;

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Tenant tenantA;
    private Tenant tenantB;

    private User ownerA;
    private User ownerB;
    private User cashierA;

    private String ownerAToken;
    private String ownerBToken;
    private String cashierAToken;

    private UnitDefinition unitA;
    private UnitDefinition unitB;

    private Category catA1;
    private Category catA2;
    private Category catB;

    private Product prodLowStock;
    private Product prodHealthyStock;
    private Product prodTenantB;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        receivingItemRepository.deleteAll();
        receivingRepository.deleteAll();
        adjustmentItemRepository.deleteAll();
        adjustmentRepository.deleteAll();
        balanceRepository.deleteAll();
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Setup Tenant A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("alpha-yard")
                .name("Alpha Yard")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerA = userRepository.save(User.builder()
                .email("owner.a@alphayard.example")
                .fullName("Alice Alpha")
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
                .email("cashier.a@alphayard.example")
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

        // 2. Setup Tenant B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("beta-hardware")
                .name("Beta Hardware")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerB = userRepository.save(User.builder()
                .email("owner.b@betahardware.example")
                .fullName("Bob Beta")
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

        // 3. Units
        unitA = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        unitB = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantB.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        // 4. Categories
        catA1 = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Masonry")
                .active(true)
                .build());

        catA2 = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Timber")
                .active(true)
                .build());

        catB = categoryRepository.save(Category.builder()
                .tenantId(tenantB.getId())
                .name("Beta Exclusive Category")
                .active(true)
                .build());

        // 5. Products and Balances in Tenant A
        // Product 1: Low stock (onHand = 5 <= reorderThreshold = 20), cost $10
        prodLowStock = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("CBLK-4IN")
                .name("Concrete Block 4 Inch")
                .categoryId(catA1.getId())
                .baseUnitId(unitA.getId())
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("14.0000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(prodLowStock.getId())
                .quantityOnHand(new BigDecimal("5.0000"))
                .averageCost(new BigDecimal("10.0000"))
                .build());

        // Product 2: Healthy stock (onHand = 100 > reorderThreshold = 10), cost $25
        prodHealthyStock = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("TIM-2X4")
                .name("Treated Pine 2x4 12ft")
                .categoryId(catA2.getId())
                .baseUnitId(unitA.getId())
                .costPrice(new BigDecimal("25.0000"))
                .sellingPrice(new BigDecimal("35.0000"))
                .reorderThreshold(new BigDecimal("10.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(prodHealthyStock.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("25.0000"))
                .build());

        // 6. Product and Balance in Tenant B (should be completely isolated)
        prodTenantB = productRepository.save(Product.builder()
                .tenantId(tenantB.getId())
                .sku("BETA-ITEM")
                .name("Beta Only Item")
                .categoryId(catB.getId())
                .baseUnitId(unitB.getId())
                .costPrice(new BigDecimal("100.0000"))
                .sellingPrice(new BigDecimal("150.0000"))
                .reorderThreshold(new BigDecimal("5.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantB.getId())
                .productId(prodTenantB.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("100.0000"))
                .build());
    }

    @Test
    @DisplayName("List balances with lowStockOnly filter returns only depleted items")
    void listBalances_lowStockFilter() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .param("lowStockOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("CBLK-4IN")))
                .andExpect(jsonPath("$.data.content[0].lowStock", is(true)));
    }

    @Test
    @DisplayName("Low stock report endpoint returns low stock products")
    void getLowStockReport_success() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/low-stock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("CBLK-4IN")));
    }

    @Test
    @DisplayName("Inventory valuation report accurately sums valuation and category breakdowns")
    void getValuationReport_accuracy() throws Exception {
        // Tenant A total valuation:
        // CBLK-4IN: 5 * $10.0000 = $50.0000
        // TIM-2X4: 100 * $25.0000 = $2500.0000
        // Total = $2550.0000 across 105 units and 2 items
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/valuation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSkus", is(2)))
                .andExpect(jsonPath("$.data.inStockSkus", is(2)))
                .andExpect(jsonPath("$.data.totalValuation", is(2550.0)))
                .andExpect(jsonPath("$.data.categoryBreakdowns", hasSize(2)));
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant A cannot access Tenant B's inventory balances or reports")
    void crossTenantInventoryIsolation() throws Exception {
        // 1. Balance list for Tenant A must have 2 items, never Tenant B's item
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // 2. Tenant A trying to get Tenant B's product balance directly returns 404
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + prodTenantB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isNotFound());

        // 3. Tenant B querying valuation gets only Tenant B's data ($5,000 valuation)
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/valuation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSkus", is(1)))
                .andExpect(jsonPath("$.data.totalValuation", is(5000.0)));
    }

    @Test
    @DisplayName("Cross-tenant isolation on stock receiving detail: Tenant A cannot view Tenant B's receipt")
    void crossTenantReceivingDetailIsolation() throws Exception {
        StockReceiving receiptB = receivingRepository.save(StockReceiving.builder()
                .tenantId(tenantB.getId())
                .receivingNumber("RCV-B-0001")
                .supplierName("Secret Beta Supplier")
                .totalCost(new BigDecimal("5000.0000"))
                .receivedAt(Instant.now())
                .receivedBy(ownerB.getId())
                .status("COMPLETED")
                .build());

        // Tenant A attempts to view Tenant B's receipt
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/receipts/" + receiptB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isNotFound());
    }
}
