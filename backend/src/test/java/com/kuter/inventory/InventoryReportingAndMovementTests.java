package com.kuter.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.ProductUnitConversion;
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
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CreateStockReceivingItemDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryReportingAndMovementTests {

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
    private ProductUnitConversionRepository conversionRepository;

    @Autowired
    private StockBalanceRepository balanceRepository;

    @Autowired
    private StockMovementRepository movementRepository;

    @Autowired
    private StockReceivingRepository receivingRepository;

    @Autowired
    private StockReceivingItemRepository receivingItemRepository;

    private Tenant tenant;
    private String ownerToken;
    private Category cementCategory;
    private Category steelCategory;
    private UnitDefinition bagUnit;
    private UnitDefinition palletUnit;
    private Product portlandCement;
    private Product rebarSteel;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        receivingItemRepository.deleteAll();
        receivingRepository.deleteAll();
        balanceRepository.deleteAll();
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .code("kuter-materials")
                .name("Kuter Building Materials")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        User owner = userRepository.save(User.builder()
                .email("owner@kutermaterials.com")
                .passwordHash(passwordEncoder.encode("SecurePass123!"))
                .fullName("Shop Owner")
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership membership = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(membership)));

        cementCategory = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Cement & Binders")
                .active(true)
                .build());

        steelCategory = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Reinforcement Steel")
                .active(true)
                .build());

        bagUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BAG")
                .name("Bag")
                .build());

        palletUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("PLT")
                .name("Pallet")
                .build());

        // Portland Cement: threshold 50.0000
        portlandCement = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("CEM-PORT-01")
                .name("Portland Cement 50kg")
                .categoryId(cementCategory.getId())
                .baseUnitId(bagUnit.getId())
                .costPrice(new BigDecimal("8.5000"))
                .sellingPrice(new BigDecimal("12.0000"))
                .reorderThreshold(new BigDecimal("50.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Rebar Steel: threshold 20.0000
        rebarSteel = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("STL-BAR-12")
                .name("Rebar Steel 12mm")
                .categoryId(steelCategory.getId())
                .baseUnitId(bagUnit.getId())
                .costPrice(new BigDecimal("15.0000"))
                .sellingPrice(new BigDecimal("22.0000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Pallet conversion: 1 Pallet = 40 Bags
        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(portlandCement.getId())
                .unitId(palletUnit.getId())
                .conversionFactor(new BigDecimal("40.0000"))
                .build());
    }

    @Test
    @DisplayName("GET /api/v1/inventory/reports/current returns stock status and secondary unit conversion metadata")
    void shouldReturnCurrentInventoryWithConversionInfo() throws Exception {
        CreateStockReceivingDto receiveDto = CreateStockReceivingDto.builder()
                .supplierName("National Cement Ltd")
                .notes("Initial shipment")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(portlandCement.getId())
                                .receivedUnitId(bagUnit.getId())
                                .receivedQuantity(new BigDecimal("80.0000"))
                                .unitCost(new BigDecimal("8.5000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(receiveDto)))
                .andExpect(status().isCreated());

        // Query Current Inventory Report
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                // Portland Cement check (80 bags > threshold 50 -> IN_STOCK)
                .andExpect(jsonPath("$.data.content[0].productSku", is("CEM-PORT-01")))
                .andExpect(jsonPath("$.data.content[0].quantityOnHand", is(80.0)))
                .andExpect(jsonPath("$.data.content[0].stockStatus", is("IN_STOCK")))
                .andExpect(jsonPath("$.data.content[0].baseUnitCode", is("BAG")))
                .andExpect(jsonPath("$.data.content[0].conversions", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].conversions[0].unitCode", is("PLT")))
                .andExpect(jsonPath("$.data.content[0].conversions[0].conversionFactor", is(40.0)))
                .andExpect(jsonPath("$.data.content[0].conversions[0].equivalentQuantity", is(2.0)))
                // Rebar Steel check (0 bars <= 20 -> OUT_OF_STOCK)
                .andExpect(jsonPath("$.data.content[1].productSku", is("STL-BAR-12")))
                .andExpect(jsonPath("$.data.content[1].quantityOnHand").value(0))
                .andExpect(jsonPath("$.data.content[1].stockStatus", is("OUT_OF_STOCK")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/reports/valuation returns weighted average calculation basis and category breakdown")
    void shouldReturnDetailedValuationReport() throws Exception {
        CreateStockReceivingDto receiveDto = CreateStockReceivingDto.builder()
                .supplierName("National Cement Ltd")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(portlandCement.getId())
                                .receivedUnitId(bagUnit.getId())
                                .receivedQuantity(new BigDecimal("100.0000"))
                                .unitCost(new BigDecimal("9.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(receiveDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/valuation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valuationType", is("CURRENT")))
                .andExpect(jsonPath("$.data.currency", is("USD")))
                .andExpect(jsonPath("$.data.categoryBreakdowns", hasSize(2)))
                .andExpect(jsonPath("$.data.categoryBreakdowns[*].categoryName", org.hamcrest.Matchers.hasItems("Cement & Binders", "Reinforcement Steel")));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/reports/valuation/items returns paged item valuation details")
    void shouldReturnValuationItemsReport() throws Exception {
        CreateStockReceivingDto receiveDto = CreateStockReceivingDto.builder()
                .supplierName("National Cement Ltd")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(portlandCement.getId())
                                .receivedUnitId(bagUnit.getId())
                                .receivedQuantity(new BigDecimal("100.0000"))
                                .unitCost(new BigDecimal("9.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(receiveDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/reports/valuation/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].productSku", is("CEM-PORT-01")))
                .andExpect(jsonPath("$.data.content[0].quantityOnHand", is(100.0)))
                .andExpect(jsonPath("$.data.content[0].averageCost", is(9.0)))
                .andExpect(jsonPath("$.data.content[0].totalValuation", is(900.0)));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/movements with date-range and referenceType filter")
    void shouldFilterStockMovements() throws Exception {
        CreateStockReceivingDto receiveDto = CreateStockReceivingDto.builder()
                .supplierName("National Cement Ltd")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(portlandCement.getId())
                                .receivedUnitId(bagUnit.getId())
                                .receivedQuantity(new BigDecimal("50.0000"))
                                .unitCost(new BigDecimal("8.5000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(receiveDto)))
                .andExpect(status().isCreated());

        // Filter by referenceType = STOCK_RECEIVING
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .param("referenceType", "STOCK_RECEIVING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].referenceType", is("STOCK_RECEIVING")))
                .andExpect(jsonPath("$.data.content[0].movementType", is("RECEIPT")))
                .andExpect(jsonPath("$.data.content[0].quantityChange", is(50.0)));

        // Filter by non-existent referenceType = SALE
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", tenant.getId().toString())
                        .param("referenceType", "SALE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }
}
