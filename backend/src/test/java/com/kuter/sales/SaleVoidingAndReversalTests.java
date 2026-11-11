package com.kuter.sales;

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
import com.kuter.inventory.MovementType;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleVoidingAndReversalTests {

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
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Tenant tenantA;
    private User ownerA;
    private User managerA;
    private User cashierA;

    private String ownerToken;
    private String managerToken;
    private String cashierToken;

    private UnitDefinition pieceUnit;
    private UnitDefinition boxUnit;
    private Category category;
    private Product paintBucket;

    @BeforeEach
    void setUp() {
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        movementRepository.deleteAll();
        balanceRepository.deleteAll();
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Setup Tenant
        tenantA = tenantRepository.save(Tenant.builder()
                .code("delta-hardware")
                .name("Delta Hardware Ltd")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 2. Setup Users
        ownerA = userRepository.save(User.builder()
                .email("owner@delta.example")
                .fullName("David Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        managerA = userRepository.save(User.builder()
                .email("manager@delta.example")
                .fullName("Mary Manager")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        cashierA = userRepository.save(User.builder()
                .email("cashier@delta.example")
                .fullName("Chris Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership memOwner = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(ownerA.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memManager = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(managerA.getId())
                .role(TenantRole.MANAGER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memCashier = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwner)));
        managerToken = jwtService.generateToken(new KuterUserPrincipal(managerA, List.of(memManager)));
        cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashier)));

        // 3. Units & Category
        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BOX")
                .name("Box of 4")
                .build());

        category = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Paints & Finishes")
                .description("Wall paints and coatings")
                .active(true)
                .build());

        // 4. Product
        paintBucket = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .categoryId(category.getId())
                .baseUnitId(pieceUnit.getId())
                .sku("PNT-WHITE-5L")
                .name("Gloss White Paint 5L")
                .sellingPrice(new BigDecimal("45.0000"))
                .costPrice(new BigDecimal("30.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Box conversion: 1 BOX = 4 PCS
        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenantA.getId())
                .productId(paintBucket.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("4.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build());

        // Initial inventory: 100 PCS on hand, average cost $30.0000
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(paintBucket.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("30.0000"))
                .build());
    }

    private UUID createSampleSale(int quantityPcs) throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Contractor Bob")
                .notes("Standard counter pickup")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(paintBucket.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal(quantityPcs).setScale(4, RoundingMode.HALF_UP))
                                .unitPrice(new BigDecimal("45.0000"))
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        SaleResponseDto created = objectMapper.readTree(responseBody).get("data").traverse(objectMapper).readValueAs(SaleResponseDto.class);
        return created.getId();
    }

    @Test
    @DisplayName("Owner can void completed sale: restores stock and logs compensating movement")
    void voidSale_asOwner_success() throws Exception {
        // Sale of 20 pieces: stock drops from 100 to 80
        UUID saleId = createSampleSale(20);

        StockBalance midBalance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), paintBucket.getId()).orElseThrow();
        assertThat(midBalance.getQuantityOnHand()).isEqualByComparingTo("80.0000");

        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("Customer changed mind and returned goods immediately")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("VOIDED")))
                .andExpect(jsonPath("$.data.voidReason", is("Customer changed mind and returned goods immediately")))
                .andExpect(jsonPath("$.data.voidedAt", notNullValue()));

        // Verify sale record updated in DB
        Sale voidedSale = saleRepository.findById(saleId).orElseThrow();
        assertThat(voidedSale.getStatus()).isEqualTo(SaleStatus.VOIDED);
        assertThat(voidedSale.getVoidReason()).isEqualTo("Customer changed mind and returned goods immediately");
        assertThat(voidedSale.getVoidedBy()).isEqualTo(ownerA.getId());

        // Verify stock balance restored to 100
        StockBalance restoredBalance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), paintBucket.getId()).orElseThrow();
        assertThat(restoredBalance.getQuantityOnHand()).isEqualByComparingTo("100.0000");

        // Verify compensating stock movement logged with RETURN type and SALE_VOID reference
        List<StockMovement> movements = movementRepository.findByTenantIdAndProductIdOrderByCreatedAtDesc(tenantA.getId(), paintBucket.getId());
        StockMovement latestMovement = movements.get(0);
        assertThat(latestMovement.getMovementType()).isEqualTo(MovementType.RETURN);
        assertThat(latestMovement.getReferenceType()).isEqualTo("SALE_VOID");
        assertThat(latestMovement.getReferenceId()).isEqualTo(saleId);
        assertThat(latestMovement.getQuantityChange()).isEqualByComparingTo("20.0000");
        assertThat(latestMovement.getBalanceAfter()).isEqualByComparingTo("100.0000");
    }

    @Test
    @DisplayName("Manager can void completed sale successfully")
    void voidSale_asManager_success() throws Exception {
        UUID saleId = createSampleSale(10);

        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("Manager authorized order cancellation")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + managerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("VOIDED")));

        Sale voidedSale = saleRepository.findById(saleId).orElseThrow();
        assertThat(voidedSale.getStatus()).isEqualTo(SaleStatus.VOIDED);
        assertThat(voidedSale.getVoidedBy()).isEqualTo(managerA.getId());
    }

    @Test
    @DisplayName("Voiding secondary unit sale (BOX) correctly restores base units (PCS)")
    void voidSale_secondaryUnit_restoresBaseUnits() throws Exception {
        // Create sale of 2 BOXes (2 * 4 = 8 PCS)
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Painter Tim")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(paintBucket.getId())
                                .unitDefinitionId(boxUnit.getId())
                                .quantity(new BigDecimal("2.0000"))
                                .unitPrice(new BigDecimal("170.0000"))
                                .build()
                ))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        SaleResponseDto created = objectMapper.readTree(responseBody).get("data").traverse(objectMapper).readValueAs(SaleResponseDto.class);
        UUID saleId = created.getId();

        // Stock dropped by 8 PCS: 100 - 8 = 92
        StockBalance midBalance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), paintBucket.getId()).orElseThrow();
        assertThat(midBalance.getQuantityOnHand()).isEqualByComparingTo("92.0000");

        // Void the sale
        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("Wrong unit selected by cashier")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("VOIDED")));

        // Stock restored back to 100 PCS
        StockBalance restoredBalance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), paintBucket.getId()).orElseThrow();
        assertThat(restoredBalance.getQuantityOnHand()).isEqualByComparingTo("100.0000");
    }

    @Test
    @DisplayName("Cashier cannot void sale - returns 403 Forbidden")
    void voidSale_asCashier_forbidden() throws Exception {
        UUID saleId = createSampleSale(5);

        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("Cashier attempt to void")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isForbidden());

        // Ensure sale status was NOT changed
        Sale sale = saleRepository.findById(saleId).orElseThrow();
        assertThat(sale.getStatus()).isEqualTo(SaleStatus.COMPLETED);
    }

    @Test
    @DisplayName("Voiding already voided sale returns 400 Bad Request")
    void voidSale_alreadyVoided_badRequest() throws Exception {
        UUID saleId = createSampleSale(5);

        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("First void authorization")
                .build();

        // First void succeeds
        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isOk());

        // Second void attempt fails
        VoidSaleRequestDto secondRequest = VoidSaleRequestDto.builder()
                .reason("Duplicate void request")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("Voiding with blank or missing reason returns 400 Bad Request")
    void voidSale_blankReason_badRequest() throws Exception {
        UUID saleId = createSampleSale(5);

        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("   ")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + saleId + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")));
    }

    @Test
    @DisplayName("Voiding non-existent sale returns 404 Not Found")
    void voidSale_notFound() throws Exception {
        VoidSaleRequestDto voidRequest = VoidSaleRequestDto.builder()
                .reason("Void unknown sale")
                .build();

        mockMvc.perform(post("/api/v1/sales/" + UUID.randomUUID() + "/void")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voidRequest)))
                .andExpect(status().isNotFound());
    }
}
