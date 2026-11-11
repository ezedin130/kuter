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
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
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
import java.util.List;

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
class SalesReportingAndDailyBreakdownTests {

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
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitDefinitionRepository unitRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Autowired
    private com.kuter.inventory.StockMovementRepository movementRepository;

    @Autowired
    private com.kuter.catalog.ProductUnitConversionRepository conversionRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    private Tenant tenant;
    private String ownerToken;
    private String cashierToken;
    private Product productA;
    private Product productB;
    private UnitDefinition pieceUnit;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        balanceRepository.deleteAll();
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        unitRepository.deleteAll();
        categoryRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .code("apex-hardware")
                .name("Apex Hardware Supplies")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        User owner = userRepository.save(User.builder()
                .email("owner@apex.example")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .fullName("Apex Owner")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        User cashier = userRepository.save(User.builder()
                .email("cashier@apex.example")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .fullName("Apex Cashier")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memOwner = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memCashier = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(cashier.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(memOwner)));
        cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashier, List.of(memCashier)));

        Category category = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("General Tools")
                .active(true)
                .build());

        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("PCS")
                .name("Piece")
                .build());

        productA = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("HAMMER-01")
                .name("Claw Hammer 16oz")
                .categoryId(category.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("20.0000"))
                .reorderThreshold(new BigDecimal("5.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        productB = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("SAW-02")
                .name("Handsaw 20in")
                .categoryId(category.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(new BigDecimal("15.0000"))
                .sellingPrice(new BigDecimal("30.0000"))
                .reorderThreshold(new BigDecimal("5.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Stock balances: 50 hammers @ $10, 50 saws @ $15
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(productA.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("10.0000"))
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(productB.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("15.0000"))
                .build());

        // Create Sale 1: 2 Hammers (Total: $40.00)
        CreateSaleRequestDto sale1 = CreateSaleRequestDto.builder()
                .customerName("John Doe")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(productA.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("2.0000"))
                                .unitPrice(new BigDecimal("20.0000"))
                                .build()
                ))
                .build();

        // Create Sale 2: 1 Saw (Total: $30.00)
        CreateSaleRequestDto sale2 = CreateSaleRequestDto.builder()
                .customerName("Jane Smith")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(productB.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("1.0000"))
                                .unitPrice(new BigDecimal("30.0000"))
                                .build()
                ))
                .build();

        try {
            mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .header("X-Tenant-ID", tenant.getId().toString())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(sale1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .header("X-Tenant-ID", tenant.getId().toString())
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(sale2)))
                    .andExpect(status().isCreated());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Filter GET /api/v1/sales by productId")
    void shouldFilterSalesByProductId() throws Exception {
        // Query sales containing Product A (Hammer) -> should return 1 sale
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .param("productId", productA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].customerName", is("John Doe")));

        // Query sales containing Product B (Saw) -> should return 1 sale
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .param("productId", productB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].customerName", is("Jane Smith")));
    }

    @Test
    @DisplayName("GET /api/v1/sales/reports/daily returns daily aggregated breakdown")
    void shouldReturnDailySalesBreakdown() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/reports/daily")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].salesCount", is(2)))
                .andExpect(jsonPath("$.data[0].salesAmount").value(70.0));
    }

    @Test
    @DisplayName("Owner can see profit and COGS in summary report, while Cashier sees null")
    void shouldEnforceCostMarginRedactionForCashier() throws Exception {
        // Owner view: can see COGS and Gross Profit
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSalesCount", is(2)))
                .andExpect(jsonPath("$.data.totalSalesAmount").value(70.0))
                .andExpect(jsonPath("$.data.totalCostOfGoodsSold").value(35.0))       // 2*10 + 1*15 = 35
                .andExpect(jsonPath("$.data.grossProfit").value(35.0))               // 70 - 35 = 35
                .andExpect(jsonPath("$.data.grossMarginPercentage").value(50.0));

        // Cashier view: totalCostOfGoodsSold and grossProfit are omitted (redacted)
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSalesCount", is(2)))
                .andExpect(jsonPath("$.data.totalSalesAmount").value(70.0))
                .andExpect(jsonPath("$.data.totalCostOfGoodsSold").doesNotExist())
                .andExpect(jsonPath("$.data.grossProfit").doesNotExist())
                .andExpect(jsonPath("$.data.grossMarginPercentage").doesNotExist());
    }
}
