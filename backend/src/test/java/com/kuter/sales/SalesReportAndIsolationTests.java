package com.kuter.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
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
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.SaleResponseDto;
import com.kuter.sales.dto.VoidSaleRequestDto;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesReportAndIsolationTests {

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
    private StockBalanceRepository balanceRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // Tenant A
    private Tenant tenantA;
    private User ownerA;
    private User cashierA;
    private String ownerAToken;
    private String cashierAToken;
    private UnitDefinition unitA;
    private Product cementA;
    private Product rebarA;

    // Tenant B
    private Tenant tenantB;
    private User ownerB;
    private String ownerBToken;

    @BeforeEach
    void setUp() {
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        movementRepository.deleteAll();
        balanceRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Setup Tenant A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("tenant-alpha")
                .name("Alpha Building Materials")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerA = userRepository.save(User.builder()
                .email("owner@alpha.example")
                .fullName("Arthur Alpha")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        cashierA = userRepository.save(User.builder()
                .email("cashier@alpha.example")
                .fullName("Clara Cashier")
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

        TenantMembership memCashierA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerAToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwnerA)));
        cashierAToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashierA)));

        // 2. Setup Tenant B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("tenant-beta")
                .name("Beta Masonry Supply")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerB = userRepository.save(User.builder()
                .email("owner@beta.example")
                .fullName("Boris Beta")
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

        // 3. Setup Catalog & Inventory for Tenant A
        unitA = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BAG")
                .name("Bag")
                .build());

        Category catA = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Masonry Supplies")
                .description("Concrete and masonry")
                .active(true)
                .build());

        // Cement: Price = $12.00, Cost = $8.00
        cementA = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .categoryId(catA.getId())
                .baseUnitId(unitA.getId())
                .sku("CEM-50KG")
                .name("Portland Cement 50kg")
                .sellingPrice(new BigDecimal("12.0000"))
                .costPrice(new BigDecimal("8.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(cementA.getId())
                .quantityOnHand(new BigDecimal("500.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        // Rebar: Price = $20.00, Cost = $14.00
        rebarA = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .categoryId(catA.getId())
                .baseUnitId(unitA.getId())
                .sku("REB-12MM")
                .name("Steel Rebar 12mm")
                .sellingPrice(new BigDecimal("20.0000"))
                .costPrice(new BigDecimal("14.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(rebarA.getId())
                .quantityOnHand(new BigDecimal("200.0000"))
                .averageCost(new BigDecimal("14.0000"))
                .build());
    }

    private SaleResponseDto createSaleForTenantA(String customer, Product product, int quantity, BigDecimal discount) throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName(customer)
                .discountAmount(discount)
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(product.getId())
                                .unitDefinitionId(unitA.getId())
                                .quantity(new BigDecimal(quantity).setScale(4, RoundingMode.HALF_UP))
                                .unitPrice(product.getSellingPrice())
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").traverse(objectMapper).readValueAs(SaleResponseDto.class);
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B cannot access or modify Tenant A's sales")
    void crossTenantIsolation_strictRejection() throws Exception {
        SaleResponseDto saleA = createSaleForTenantA("Tenant A Client", cementA, 10, BigDecimal.ZERO);

        // 1. Tenant B cannot retrieve Tenant A sale by ID -> 404
        mockMvc.perform(get("/api/v1/sales/" + saleA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isNotFound());

        // 2. Tenant B cannot retrieve Tenant A printable receipt -> 404
        mockMvc.perform(get("/api/v1/sales/" + saleA.getId() + "/receipt")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isNotFound());

        // 3. Tenant B cannot void Tenant A sale -> 404
        VoidSaleRequestDto voidDto = VoidSaleRequestDto.builder().reason("Unauthorized cross-tenant void").build();
        mockMvc.perform(post("/api/v1/sales/" + saleA.getId() + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidDto)))
                .andExpect(status().isNotFound());

        // 4. Tenant B sale list does not include Tenant A sales
        mockMvc.perform(get("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("Printable receipt returns operational details and explicit disclaimer without sensitive cost fields")
    void getSaleReceipt_operationalReceiptDetails() throws Exception {
        SaleResponseDto saleA = createSaleForTenantA("Commercial Contractor", cementA, 5, new BigDecimal("5.0000"));

        mockMvc.perform(get("/api/v1/sales/" + saleA.getId() + "/receipt")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.shopName", is("Alpha Building Materials")))
                .andExpect(jsonPath("$.data.customerName", is("Commercial Contractor")))
                .andExpect(jsonPath("$.data.invoiceNumber", is(saleA.getInvoiceNumber())))
                .andExpect(jsonPath("$.data.disclaimer", is("Operational Sales Record - Not a Proof of Payment")))
                .andExpect(jsonPath("$.data.subtotal", is(60.0))) // 5 * 12.00 = 60.00
                .andExpect(jsonPath("$.data.discountAmount", is(5.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(55.0))) // 60 - 5 = 55.00
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName", is("Portland Cement 50kg")))
                .andExpect(jsonPath("$.data.items[0].unitCode", is("BAG")))
                .andExpect(jsonPath("$.data.items[0].quantity", is(5.0)))
                .andExpect(jsonPath("$.data.items[0].unitPrice", is(12.0)))
                .andExpect(jsonPath("$.data.items[0].totalPrice", is(60.0)));
    }

    @Test
    @DisplayName("Owner gets sales summary report with gross profit and margin calculations")
    void getSalesSummaryReport_asOwner_includesFinancialMetrics() throws Exception {
        // Sale 1: Cement 10 bags @ $12 = $120, cost = $80, profit = $40
        SaleResponseDto s1 = createSaleForTenantA("Customer 1", cementA, 10, BigDecimal.ZERO);

        // Sale 2: Rebar 5 pieces @ $20 = $100, cost = $70, discount = $10, total = $90, profit = 90 - 70 = 20
        SaleResponseDto s2 = createSaleForTenantA("Customer 2", rebarA, 5, new BigDecimal("10.0000"));

        // Sale 3: Cement 5 bags @ $12 = $60, then voided
        SaleResponseDto s3 = createSaleForTenantA("Customer 3", cementA, 5, BigDecimal.ZERO);
        VoidSaleRequestDto voidDto = VoidSaleRequestDto.builder().reason("Returned immediately").build();
        mockMvc.perform(post("/api/v1/sales/" + s3.getId() + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidDto)))
                .andExpect(status().isOk());

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/sales/reports/summary")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSalesCount", is(2)))
                .andExpect(jsonPath("$.data.totalSalesAmount", is(210.0))) // 120 + 90
                .andExpect(jsonPath("$.data.totalCostOfGoodsSold", is(150.0))) // 80 + 70
                .andExpect(jsonPath("$.data.grossProfit", is(60.0))) // 210 - 150
                .andExpect(jsonPath("$.data.voidedSalesCount", is(1)))
                .andExpect(jsonPath("$.data.voidedSalesAmount", is(60.0)))
                .andExpect(jsonPath("$.data.dailyBreakdowns", hasSize(1)))
                .andExpect(jsonPath("$.data.dailyBreakdowns[0].salesAmount", is(210.0)))
                .andExpect(jsonPath("$.data.dailyBreakdowns[0].costOfGoodsSold", is(150.0)))
                .andExpect(jsonPath("$.data.dailyBreakdowns[0].grossProfit", is(60.0)));
    }

    @Test
    @DisplayName("Cashier viewing sales summary report has proprietary cost and profit metrics stripped")
    void getSalesSummaryReport_asCashier_costAndProfitOmitted() throws Exception {
        createSaleForTenantA("Customer 1", cementA, 10, BigDecimal.ZERO);

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/sales/reports/summary")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSalesCount", is(1)))
                .andExpect(jsonPath("$.data.totalSalesAmount", is(120.0)))
                .andExpect(jsonPath("$.data.totalCostOfGoodsSold").doesNotExist())
                .andExpect(jsonPath("$.data.grossProfit").doesNotExist())
                .andExpect(jsonPath("$.data.grossMarginPercentage").doesNotExist())
                .andExpect(jsonPath("$.data.dailyBreakdowns[0].costOfGoodsSold").doesNotExist())
                .andExpect(jsonPath("$.data.dailyBreakdowns[0].grossProfit").doesNotExist());
    }

    @Test
    @DisplayName("Product sales ranking report calculates volume, revenue and gross margins")
    void getProductSalesReport_ranksAndCalculatesAccurately() throws Exception {
        // Product 1: Cement 20 bags @ $12 = $240, Cost = $160, Profit = $80
        createSaleForTenantA("Cement Buyer", cementA, 20, BigDecimal.ZERO);

        // Product 2: Rebar 5 pieces @ $20 = $100, Cost = $70, Profit = $30
        createSaleForTenantA("Rebar Buyer", rebarA, 5, BigDecimal.ZERO);

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/sales/reports/products")
                        .param("startDate", today.toString())
                        .param("endDate", today.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.products", hasSize(2)))
                // Top product by revenue: Cement ($240)
                .andExpect(jsonPath("$.data.products[0].productName", is("Portland Cement 50kg")))
                .andExpect(jsonPath("$.data.products[0].totalQuantitySold", is(20.0)))
                .andExpect(jsonPath("$.data.products[0].totalRevenue", is(240.0)))
                .andExpect(jsonPath("$.data.products[0].totalCost", is(160.0)))
                .andExpect(jsonPath("$.data.products[0].grossProfit", is(80.0)))
                // Second product: Rebar ($100)
                .andExpect(jsonPath("$.data.products[1].productName", is("Steel Rebar 12mm")))
                .andExpect(jsonPath("$.data.products[1].totalQuantitySold", is(5.0)))
                .andExpect(jsonPath("$.data.products[1].totalRevenue", is(100.0)))
                .andExpect(jsonPath("$.data.products[1].totalCost", is(70.0)))
                .andExpect(jsonPath("$.data.products[1].grossProfit", is(30.0)));
    }
}
