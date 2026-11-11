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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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
class StockReceivingAndCostingTests {

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
    private User storekeeperA;
    private User cashierA;

    private String ownerAToken;
    private String storekeeperAToken;
    private String cashierAToken;

    private UnitDefinition pieceUnit;
    private UnitDefinition boxUnit;
    private Category buildingMaterials;
    private Product steelRebar;

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
                .code("yard-builders")
                .name("Yard Builders Supply")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 2. Owner A
        ownerA = userRepository.save(User.builder()
                .email("owner@yardbuilders.example")
                .fullName("Owen Owner")
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
        ownerAToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwner)));

        // 3. Storekeeper A
        storekeeperA = userRepository.save(User.builder()
                .email("keeper@yardbuilders.example")
                .fullName("Stan Storekeeper")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());
        TenantMembership memKeeper = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(storekeeperA.getId())
                .role(TenantRole.STOREKEEPER)
                .status(MembershipStatus.ACTIVE)
                .build());
        storekeeperAToken = jwtService.generateToken(new KuterUserPrincipal(storekeeperA, List.of(memKeeper)));

        // 4. Cashier A
        cashierA = userRepository.save(User.builder()
                .email("cashier@yardbuilders.example")
                .fullName("Casey Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());
        TenantMembership memCashier = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());
        cashierAToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashier)));

        // 5. Tenant B setup
        tenantB = tenantRepository.save(Tenant.builder()
                .code("foreign-yard")
                .name("Foreign Yard Supply")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 6. Base Units and Category
        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BOX")
                .name("Box of 50")
                .build());

        buildingMaterials = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Structural Steel")
                .active(true)
                .build());

        steelRebar = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("REBAR-16MM")
                .name("High Yield Steel Rebar 16mm")
                .categoryId(buildingMaterials.getId())
                .baseUnitId(pieceUnit.getId())
                .costPrice(BigDecimal.ZERO)
                .sellingPrice(new BigDecimal("15.0000"))
                .reorderThreshold(new BigDecimal("100.0000"))
                .status(ProductStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Initial stock receiving into empty stock sets on-hand and average cost directly")
    void receiveStock_initialReceiptIntoZeroStock() throws Exception {
        CreateStockReceivingDto receiptDto = CreateStockReceivingDto.builder()
                .supplierName("Apex Steel Mills Ltd")
                .deliveryNoteRef("DN-2026-001")
                .notes("Initial container unloading")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(steelRebar.getId())
                                .receivedUnitId(pieceUnit.getId())
                                .receivedQuantity(new BigDecimal("100.0000"))
                                .unitCost(new BigDecimal("10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.supplierName", is("Apex Steel Mills Ltd")))
                .andExpect(jsonPath("$.data.totalCost", is(1000.0)))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].baseQuantity", is(100.0)))
                .andExpect(jsonPath("$.data.items[0].totalCost", is(1000.0)));

        // Verify balance in DB
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelRebar.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("10.0000"));

        // Verify ledger movement
        List<StockMovement> movements = movementRepository.findAllByTenantId(tenantA.getId());
        assertThat(movements).hasSize(1);
        StockMovement m = movements.get(0);
        assertThat(m.getMovementType()).isEqualTo(MovementType.RECEIPT);
        assertThat(m.getQuantityChange()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(m.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(m.getUnitCost()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    @DisplayName("Moving weighted-average cost correctly updates on subsequent receipt")
    void receiveStock_weightedAverageCostCalculation() throws Exception {
        // Step 1: Pre-populate balance with 100 pcs @ $10.0000 (total $1,000)
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(steelRebar.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("10.0000"))
                .build());

        // Step 2: Receive 50 pcs @ $13.0000 (total $650)
        // Expected: (1000 + 650) / 150 = 1650 / 150 = 11.0000
        CreateStockReceivingDto secondReceipt = CreateStockReceivingDto.builder()
                .supplierName("Apex Steel Mills Ltd")
                .deliveryNoteRef("DN-2026-002")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(steelRebar.getId())
                                .receivedUnitId(pieceUnit.getId())
                                .receivedQuantity(new BigDecimal("50.0000"))
                                .unitCost(new BigDecimal("13.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReceipt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalCost", is(650.0)));

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelRebar.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("150.0000"));
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("11.0000"));

        // Product costPrice should also be updated to the latest WAC
        Product updatedProduct = productRepository.findById(steelRebar.getId()).orElseThrow();
        assertThat(updatedProduct.getCostPrice()).isEqualByComparingTo(new BigDecimal("11.0000"));
    }

    @Test
    @DisplayName("Stock receiving with unit conversion correctly converts to base quantity and base cost")
    void receiveStock_withUnitConversion() throws Exception {
        // 1 BOX = 50 PCS
        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenantA.getId())
                .productId(steelRebar.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("50.0000"))
                .build());

        // Initial balance: 50 pcs @ $4.0000 ($200)
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(steelRebar.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("4.0000"))
                .build());

        // Receive 2 boxes @ $250.00 per box = $500 total cost
        // Base quantity received = 2 * 50 = 100 pcs
        // Base cost per pc = $500 / 100 = $5.0000
        // New total pcs = 50 + 100 = 150 pcs
        // New total value = $200 + $500 = $700
        // New WAC = $700 / 150 = 4.6667
        CreateStockReceivingDto boxReceipt = CreateStockReceivingDto.builder()
                .supplierName("Apex Steel Distributors")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(steelRebar.getId())
                                .receivedUnitId(boxUnit.getId())
                                .receivedQuantity(new BigDecimal("2.0000"))
                                .unitCost(new BigDecimal("250.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boxReceipt)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].receivedQuantity", is(2.0)))
                .andExpect(jsonPath("$.data.items[0].conversionFactor", is(50.0)))
                .andExpect(jsonPath("$.data.items[0].baseQuantity", is(100.0)))
                .andExpect(jsonPath("$.data.items[0].totalCost", is(500.0)));

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelRebar.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("150.0000"));
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("4.6667"));
    }

    @Test
    @DisplayName("Idempotency key prevents duplicate stock receiving and duplicate balance inflation")
    void receiveStock_idempotencyReplayProtection() throws Exception {
        CreateStockReceivingDto receiptWithIdempotency = CreateStockReceivingDto.builder()
                .supplierName("Bulk Steel Corp")
                .idempotencyKey("KEY-RCV-UNIQUE-999")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(steelRebar.getId())
                                .receivedUnitId(pieceUnit.getId())
                                .receivedQuantity(new BigDecimal("40.0000"))
                                .unitCost(new BigDecimal("12.0000"))
                                .build()
                ))
                .build();

        // 1st request -> Created
        String firstRcvNum = objectMapper.readTree(
                mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperAToken)
                                .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(receiptWithIdempotency)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.receivingNumber").isNotEmpty())
                        .andReturn().getResponse().getContentAsString()
        ).get("data").get("receivingNumber").asText();

        StockBalance balanceFirst = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelRebar.getId()).orElseThrow();
        assertThat(balanceFirst.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("40.0000"));
        assertThat(movementRepository.findAllByTenantId(tenantA.getId())).hasSize(1);

        // 2nd request with EXACT SAME idempotencyKey -> Returns existing receipt, DOES NOT double stock!
        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptWithIdempotency)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.receivingNumber", is(firstRcvNum)));

        StockBalance balanceSecond = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelRebar.getId()).orElseThrow();
        // Quantity must still be 40.0000, NOT 80.0000
        assertThat(balanceSecond.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("40.0000"));
        assertThat(movementRepository.findAllByTenantId(tenantA.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Cross-tenant receiving is rejected when product belongs to another tenant")
    void receiveStock_crossTenantProductRejected() throws Exception {
        UnitDefinition unitB = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantB.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        Product productB = productRepository.save(Product.builder()
                .tenantId(tenantB.getId())
                .sku("PROD-B")
                .name("Foreign Product")
                .baseUnitId(unitB.getId())
                .costPrice(new BigDecimal("5.0000"))
                .sellingPrice(new BigDecimal("10.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        CreateStockReceivingDto crossTenantDto = CreateStockReceivingDto.builder()
                .supplierName("Intruder Supplier")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(productB.getId()) // Product belonging to Tenant B!
                                .receivedUnitId(pieceUnit.getId())
                                .receivedQuantity(new BigDecimal("10.0000"))
                                .unitCost(new BigDecimal("5.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossTenantDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cashier is forbidden from receiving stock (403 Forbidden)")
    void receiveStock_cashierForbidden() throws Exception {
        CreateStockReceivingDto dto = CreateStockReceivingDto.builder()
                .supplierName("Any Supplier")
                .items(List.of(
                        CreateStockReceivingItemDto.builder()
                                .productId(steelRebar.getId())
                                .receivedUnitId(pieceUnit.getId())
                                .receivedQuantity(new BigDecimal("10.0000"))
                                .unitCost(new BigDecimal("5.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/receipts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
