package com.kuter.security;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockAdjustmentItemDto;
import com.kuter.sales.SaleItemRepository;
import com.kuter.sales.SaleRepository;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossTenantSecurityComprehensiveTests {

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

    private Tenant tenantA;
    private Tenant tenantB;
    private String tokenA;
    private String tokenB;
    private Product productA;
    private UUID saleAId;

    @BeforeEach
    void setUp() throws Exception {
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

        // Tenant A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("tenant-alpha")
                .name("Tenant Alpha")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        // Tenant B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("tenant-beta")
                .name("Tenant Beta")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        // Users
        User userA = userRepository.save(User.builder()
                .email("owner@alpha.example")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .fullName("Owner Alpha")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        User userB = userRepository.save(User.builder()
                .email("owner@beta.example")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .fullName("Owner Beta")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(userA.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memB = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantB.getId())
                .userId(userB.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        tokenA = jwtService.generateToken(new KuterUserPrincipal(userA, List.of(memA)));
        tokenB = jwtService.generateToken(new KuterUserPrincipal(userB, List.of(memB)));

        Category catA = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Alpha Cement")
                .active(true)
                .build());

        UnitDefinition unitA = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BAG")
                .name("Bag")
                .build());

        productA = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("ALPHA-CEM-01")
                .name("Alpha Cement 50kg")
                .categoryId(catA.getId())
                .baseUnitId(unitA.getId())
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("15.0000"))
                .reorderThreshold(new BigDecimal("10.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(productA.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("10.0000"))
                .build());

        // Create Sale for Tenant A
        CreateSaleRequestDto createSale = CreateSaleRequestDto.builder()
                .customerName("Customer X")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(productA.getId())
                                .unitDefinitionId(unitA.getId())
                                .quantity(new BigDecimal("2.0000"))
                                .unitPrice(new BigDecimal("15.0000"))
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(createSale)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        saleAId = UUID.fromString(responseNode.path("data").path("id").asText());
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A product by ID (returns 404)")
    void shouldPreventCrossTenantProductAccess() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/products/" + productA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A stock balance by product ID (returns 404)")
    void shouldPreventCrossTenantStockBalanceAccess() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/balances/" + productA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant B cannot adjust stock for Tenant A product (returns 404)")
    void shouldPreventCrossTenantStockAdjustment() throws Exception {
        CreateStockAdjustmentDto adjustDto = CreateStockAdjustmentDto.builder()
                .reasonCode("DAMAGED")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(productA.getId())
                                .quantityChange(new BigDecimal("-10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(adjustDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A sale by ID (returns 404)")
    void shouldPreventCrossTenantSaleAccess() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/" + saleAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant B cannot access Tenant A sale receipt by ID (returns 404)")
    void shouldPreventCrossTenantReceiptAccess() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/sales/" + saleAId + "/receipt")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Tenant B cannot void Tenant A sale (returns 404)")
    void shouldPreventCrossTenantSaleVoiding() throws Exception {
        VoidSaleRequestDto voidDto = VoidSaleRequestDto.builder()
                .reason("Fraudulent order")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales/" + saleAId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB)
                        .header("X-Tenant-ID", tenantB.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(voidDto)))
                .andExpect(status().isNotFound());
    }
}
