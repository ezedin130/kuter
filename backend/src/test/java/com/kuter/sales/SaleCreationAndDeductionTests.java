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
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleCreationAndDeductionTests {

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
    private User cashierA;
    private User storekeeperA;

    private String ownerToken;
    private String cashierToken;
    private String storekeeperToken;

    private UnitDefinition pieceUnit;
    private UnitDefinition boxUnit;
    private Category category;
    private Product steelNails;
    private Product cementBags;

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
                .code("alpha-hardware")
                .name("Alpha Hardware Ltd")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 2. Setup Users
        ownerA = userRepository.save(User.builder()
                .email("owner@alpha.example")
                .fullName("Omar Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        cashierA = userRepository.save(User.builder()
                .email("cashier@alpha.example")
                .fullName("Caleb Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        storekeeperA = userRepository.save(User.builder()
                .email("storekeeper@alpha.example")
                .fullName("Sam Storekeeper")
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

        TenantMembership memCashier = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memStorekeeper = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(storekeeperA.getId())
                .role(TenantRole.STOREKEEPER)
                .status(MembershipStatus.ACTIVE)
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwner)));
        cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashier)));
        storekeeperToken = jwtService.generateToken(new KuterUserPrincipal(storekeeperA, List.of(memStorekeeper)));

        // 3. Units & Category
        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BOX")
                .name("Boxes")
                .build());

        category = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Fasteners & Hardware")
                .active(true)
                .build());

        // 4. Products: Steel Nails (base: PCS, selling price: 0.50, cost: 0.30)
        steelNails = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("NAIL-3IN")
                .name("Steel Nails 3 inch")
                .baseUnitId(pieceUnit.getId())
                .categoryId(category.getId())
                .sellingPrice(new BigDecimal("0.5000"))
                .costPrice(new BigDecimal("0.3000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Secondary unit: 1 BOX = 100 PCS
        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenantA.getId())
                .productId(steelNails.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("100.0000"))
                .isSellingUnit(true)
                .isReceivingUnit(true)
                .build());

        // Stock balance: 500 PCS on hand, average cost = 0.3000
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(steelNails.getId())
                .quantityOnHand(new BigDecimal("500.0000"))
                .averageCost(new BigDecimal("0.3000"))
                .build());

        // Cement Bags: 50 Bags on hand, selling price = 25.00, cost = 18.00
        cementBags = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("CEM-50KG")
                .name("Portland Cement 50kg")
                .baseUnitId(pieceUnit.getId())
                .categoryId(category.getId())
                .sellingPrice(new BigDecimal("25.0000"))
                .costPrice(new BigDecimal("18.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(cementBags.getId())
                .quantityOnHand(new BigDecimal("50.0000"))
                .averageCost(new BigDecimal("18.0000"))
                .build());
    }

    @Test
    @DisplayName("Create sale with single product in base unit deducts stock, records movement, and returns full financials")
    void testCreateSale_singleProduct_baseUnit_success() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Ahmed Builder")
                .customerPhone("+966501234567")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("50.0000"))
                                // unitPrice null -> uses product.sellingPrice = 0.5000
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.customerName", is("Ahmed Builder")))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.subtotal", is(25.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(25.0)))
                .andExpect(jsonPath("$.data.totalCost", is(15.0))) // 50 * 0.30 = 15.00
                .andExpect(jsonPath("$.data.grossProfit", is(10.0))) // 25 - 15 = 10.00
                .andExpect(jsonPath("$.data.items[0].soldQuantity", is(50.0)))
                .andExpect(jsonPath("$.data.items[0].baseQuantity", is(50.0)))
                .andExpect(jsonPath("$.data.items[0].unitPrice", is(0.5)));

        // Verify stock deducted: 500 - 50 = 450
        StockBalance updatedBalance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(updatedBalance.getQuantityOnHand()).isEqualByComparingTo("450.0000");

        // Verify stock movement ledger
        List<StockMovement> movements = movementRepository.findAll();
        assertThat(movements).hasSize(1);
        StockMovement movement = movements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(MovementType.SALE);
        assertThat(movement.getQuantityChange()).isEqualByComparingTo("-50.0000");
        assertThat(movement.getBalanceAfter()).isEqualByComparingTo("450.0000");
        assertThat(movement.getReferenceType()).isEqualTo("SALE");
    }

    @Test
    @DisplayName("Create sale with secondary unit multiplies conversion factor and deducts correct base units")
    void testCreateSale_secondaryUnit_success() throws Exception {
        // Sell 2 Boxes of nails (1 box = 100 pcs) => 200 pcs deducted
        // Price per box = 0.50 * 100 = 50.00 => Total = 100.00
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Contractor Corp")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(boxUnit.getId())
                                .quantity(new BigDecimal("2.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(100.0)))
                .andExpect(jsonPath("$.data.totalCost", is(60.0))) // 200 pcs * 0.30 = 60.00
                .andExpect(jsonPath("$.data.grossProfit", is(40.0)))
                .andExpect(jsonPath("$.data.items[0].soldQuantity", is(2.0)))
                .andExpect(jsonPath("$.data.items[0].conversionFactor", is(100.0)))
                .andExpect(jsonPath("$.data.items[0].baseQuantity", is(200.0)));

        // Verify stock balance: 500 - 200 = 300
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("300.0000");
    }

    @Test
    @DisplayName("Create sale with multiple products deducts all stocks atomically")
    void testCreateSale_multipleProducts_success() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("General Contractor")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("100.0000"))
                                .build(),
                        CreateSaleItemRequestDto.builder()
                                .productId(cementBags.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                // Nails: 100 * 0.50 = 50. Cement: 10 * 25.00 = 250. Total = 300.
                .andExpect(jsonPath("$.data.subtotal", is(300.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(300.0)))
                .andExpect(jsonPath("$.data.items.length()", is(2)));

        // Verify nails: 500 - 100 = 400
        StockBalance nailsBal = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(nailsBal.getQuantityOnHand()).isEqualByComparingTo("400.0000");

        // Verify cement: 50 - 10 = 40
        StockBalance cementBal = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), cementBags.getId()).orElseThrow();
        assertThat(cementBal.getQuantityOnHand()).isEqualByComparingTo("40.0000");

        // 2 distinct movements
        assertThat(movementRepository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("Create sale with line discounts and header discount computes correct totals")
    void testCreateSale_withDiscounts_success() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("VIP Client")
                .discountAmount(new BigDecimal("5.0000")) // Header discount
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("100.0000")) // 100 * 0.50 = 50.00
                                .discountAmount(new BigDecimal("2.0000")) // Line discount -> 48.00
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal", is(50.0)))
                .andExpect(jsonPath("$.data.discountAmount", is(7.0))) // 2 line + 5 header = 7
                .andExpect(jsonPath("$.data.totalAmount", is(43.0))); // 50 - 7 = 43.00
    }

    @Test
    @DisplayName("Create sale with custom unit price uses authorized custom price")
    void testCreateSale_customUnitPrice_success() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Special Rate Customer")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("10.0000"))
                                .unitPrice(new BigDecimal("0.7500")) // Overrides default 0.50
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].unitPrice", is(0.75)))
                .andExpect(jsonPath("$.data.totalAmount", is(7.5)));
    }

    @Test
    @DisplayName("Create sale with insufficient stock is rejected with 400 and completely rolled back")
    void testCreateSale_insufficientStock_rollsBack() throws Exception {
        // Attempt to sell 600 Nails when only 500 on hand
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Over-demanding Customer")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("600.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Insufficient stock")));

        // Verify stock is untouched: still 500
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("500.0000");

        // Verify no sale and no movements created
        assertThat(saleRepository.findAll()).isEmpty();
        assertThat(movementRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Create sale with inactive product is rejected with 400 Bad Request")
    void testCreateSale_inactiveProduct_rejected() throws Exception {
        steelNails.setStatus(ProductStatus.INACTIVE);
        productRepository.save(steelNails);

        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("10.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Cannot sell inactive product")));
    }

    @Test
    @DisplayName("Create sale with unconfigured unit is rejected with 400 Bad Request")
    void testCreateSale_unconfiguredUnit_rejected() throws Exception {
        // Cement bags only configured with PCS, not BOX
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(cementBags.getId())
                                .unitDefinitionId(boxUnit.getId())
                                .quantity(new BigDecimal("1.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("not configured as a selling unit")));
    }

    @Test
    @DisplayName("Idempotency key replay returns existing sale without deducting stock twice")
    void testCreateSale_idempotencyReplay_noDoubleDeduction() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Network Glitch Customer")
                .idempotencyKey("sale-tx-unique-991")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("50.0000"))
                                .build()
                ))
                .build();

        // First attempt
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotencyKey", is("sale-tx-unique-991")));

        // Stock after 1st attempt: 500 - 50 = 450
        StockBalance balanceAfter1 = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(balanceAfter1.getQuantityOnHand()).isEqualByComparingTo("450.0000");

        // Second attempt with identical idempotency key
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotencyKey", is("sale-tx-unique-991")));

        // Stock remains exactly 450 (no double deduction!)
        StockBalance balanceAfter2 = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), steelNails.getId()).orElseThrow();
        assertThat(balanceAfter2.getQuantityOnHand()).isEqualByComparingTo("450.0000");

        // Only 1 sale record and 1 movement in database
        assertThat(saleRepository.findAll()).hasSize(1);
        assertThat(movementRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("Cashier role can create sales but proprietary cost and profit data are stripped")
    void testCreateSale_asCashier_succeedsWithoutCostData() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Retail Customer")
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("20.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(10.0)))
                // Cost and profit data must be omitted/null for cashier!
                .andExpect(jsonPath("$.data.totalCost").doesNotExist())
                .andExpect(jsonPath("$.data.grossProfit").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].unitCost").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].totalCost").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].grossProfit").doesNotExist());
    }

    @Test
    @DisplayName("Storekeeper role cannot create sales and is rejected with 403 Forbidden")
    void testCreateSale_asStorekeeper_rejectedWithForbidden() throws Exception {
        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(steelNails.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
