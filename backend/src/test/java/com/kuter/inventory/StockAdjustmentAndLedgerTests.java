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
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockAdjustmentItemDto;
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
class StockAdjustmentAndLedgerTests {

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

    private Tenant tenant;
    private User owner;
    private User storekeeper;
    private User cashier;

    private String ownerToken;
    private String storekeeperToken;
    private String cashierToken;

    private UnitDefinition bagUnit;
    private Category drywallCat;
    private Product gypsumPlaster;

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

        // 1. Setup Tenant
        tenant = tenantRepository.save(Tenant.builder()
                .code("city-drywall")
                .name("City Drywall & Plaster Supplies")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 2. Setup Users
        owner = userRepository.save(User.builder()
                .email("dan.owner@citydrywall.example")
                .fullName("Dan Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());
        TenantMembership memOwner = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());
        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(memOwner)));

        storekeeper = userRepository.save(User.builder()
                .email("sam.keeper@citydrywall.example")
                .fullName("Sam Keeper")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());
        TenantMembership memKeeper = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(storekeeper.getId())
                .role(TenantRole.STOREKEEPER)
                .status(MembershipStatus.ACTIVE)
                .build());
        storekeeperToken = jwtService.generateToken(new KuterUserPrincipal(storekeeper, List.of(memKeeper)));

        cashier = userRepository.save(User.builder()
                .email("cora.cashier@citydrywall.example")
                .fullName("Cora Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());
        TenantMembership memCashier = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(cashier.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());
        cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashier, List.of(memCashier)));

        // 3. Setup Units and Product
        bagUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BAG")
                .name("25kg Bag")
                .build());

        drywallCat = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Drywall & Plaster")
                .active(true)
                .build());

        gypsumPlaster = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("GYP-PLAS-25")
                .name("Finishing Gypsum Plaster 25kg")
                .categoryId(drywallCat.getId())
                .baseUnitId(bagUnit.getId())
                .costPrice(new BigDecimal("8.0000"))
                .sellingPrice(new BigDecimal("11.5000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .status(ProductStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Upward adjustment increases stock balance and creates ADJUSTMENT movement")
    void adjustStock_upwardAdjustment_success() throws Exception {
        // Initial balance: 50 bags @ $8.0000
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        CreateStockAdjustmentDto adjDto = CreateStockAdjustmentDto.builder()
                .reasonCode("FOUND")
                .notes("Found extra unopened pallet during cycle count")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .quantityChange(new BigDecimal("15.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.reasonCode", is("FOUND")))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(15.0)))
                .andExpect(jsonPath("$.data.items[0].newQuantity", is(65.0)));

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("65.0000"));
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("8.0000"));

        List<StockMovement> movements = movementRepository.findAllByTenantId(tenant.getId());
        assertThat(movements).hasSize(1);
        StockMovement m = movements.get(0);
        assertThat(m.getMovementType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(m.getQuantityChange()).isEqualByComparingTo(new BigDecimal("15.0000"));
        assertThat(m.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("65.0000"));
    }

    @Test
    @DisplayName("Downward adjustment decreases stock while strictly preserving weighted-average cost")
    void adjustStock_downwardAdjustment_preservesWeightedAverageCost() throws Exception {
        // Initial balance: 100 bags @ $8.0000 (total valuation $800.00)
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        CreateStockAdjustmentDto damageDto = CreateStockAdjustmentDto.builder()
                .reasonCode("DAMAGED")
                .notes("Water leakage ruined 10 bags")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .quantityChange(new BigDecimal("-10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(damageDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(-10.0)))
                .andExpect(jsonPath("$.data.items[0].newQuantity", is(90.0)));

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("90.0000"));
        // WAC MUST NOT CHANGE on reduction
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("8.0000"));
    }

    @Test
    @DisplayName("Negative stock prevention: adjustment reducing balance below zero is rejected with 400")
    void adjustStock_negativeStockPrevention_failsWithBadRequest() throws Exception {
        // Balance has only 5 bags
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .quantityOnHand(new BigDecimal("5.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        // Attempting to remove 8 bags
        CreateStockAdjustmentDto excessiveDto = CreateStockAdjustmentDto.builder()
                .reasonCode("CORRECTION")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .quantityChange(new BigDecimal("-8.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("result in negative stock")));

        // Stock balance must remain untouched at 5
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("5.0000"));
        assertThat(movementRepository.findAllByTenantId(tenant.getId())).isEmpty();
    }

    @Test
    @DisplayName("Physical count reconciliation using targetQuantity computes delta automatically")
    void adjustStock_physicalAuditWithTargetQuantity() throws Exception {
        // Current system on-hand is 72 bags
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .quantityOnHand(new BigDecimal("72.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        // Physical inventory count counted 68 bags (shortage of 4)
        CreateStockAdjustmentDto auditDto = CreateStockAdjustmentDto.builder()
                .reasonCode("STOCK_COUNT")
                .notes("Month-end physical count reconciliation")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .targetQuantity(new BigDecimal("68.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auditDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].quantityChange", is(-4.0)))
                .andExpect(jsonPath("$.data.items[0].newQuantity", is(68.0)));

        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("68.0000"));
    }

    @Test
    @DisplayName("Idempotency key prevents duplicate adjustment recording")
    void adjustStock_idempotencyReplayProtection() throws Exception {
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("8.0000"))
                .build());

        CreateStockAdjustmentDto idempotentAdj = CreateStockAdjustmentDto.builder()
                .reasonCode("CORRECTION")
                .idempotencyKey("KEY-ADJ-IDEM-777")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .quantityChange(new BigDecimal("5.0000"))
                                .build()
                ))
                .build();

        // 1st request -> Created
        String firstAdjNum = objectMapper.readTree(
                mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                                .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(idempotentAdj)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.data.adjustmentNumber").isNotEmpty())
                        .andReturn().getResponse().getContentAsString()
        ).get("data").get("adjustmentNumber").asText();

        StockBalance balanceAfter1 = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balanceAfter1.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("55.0000"));

        // 2nd request with same idempotency key -> Returns existing adjustment, does NOT add another 5!
        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idempotentAdj)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.adjustmentNumber", is(firstAdjNum)));

        StockBalance balanceAfter2 = balanceRepository.findByTenantIdAndProductId(tenant.getId(), gypsumPlaster.getId()).orElseThrow();
        assertThat(balanceAfter2.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("55.0000"));
        assertThat(movementRepository.findAllByTenantId(tenant.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Stock movements ledger can be filtered by product and movement type")
    void listMovements_filteredPagination() throws Exception {
        // Seed two movements
        movementRepository.save(StockMovement.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .movementType(MovementType.RECEIPT)
                .quantityChange(new BigDecimal("50.0000"))
                .balanceAfter(new BigDecimal("50.0000"))
                .unitCost(new BigDecimal("8.0000"))
                .actorId(owner.getId())
                .build());

        movementRepository.save(StockMovement.builder()
                .tenantId(tenant.getId())
                .productId(gypsumPlaster.getId())
                .movementType(MovementType.ADJUSTMENT)
                .quantityChange(new BigDecimal("-5.0000"))
                .balanceAfter(new BigDecimal("45.0000"))
                .unitCost(new BigDecimal("8.0000"))
                .actorId(owner.getId())
                .build());

        // Query by product and type=ADJUSTMENT
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .param("productId", gypsumPlaster.getId().toString())
                        .param("movementType", "ADJUSTMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].movementType", is("ADJUSTMENT")))
                .andExpect(jsonPath("$.data.content[0].quantityChange", is(-5.0)));
    }

    @Test
    @DisplayName("Cashier is forbidden from adjusting stock (403 Forbidden)")
    void adjustStock_cashierForbidden() throws Exception {
        CreateStockAdjustmentDto dto = CreateStockAdjustmentDto.builder()
                .reasonCode("CORRECTION")
                .items(List.of(
                        CreateStockAdjustmentItemDto.builder()
                                .productId(gypsumPlaster.getId())
                                .quantityChange(new BigDecimal("1.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/adjustments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
