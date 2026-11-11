package com.kuter.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
import com.kuter.catalog.dto.CreateUnitDto;
import com.kuter.catalog.dto.UpdateCategoryDto;
import com.kuter.catalog.dto.UpdateProductDto;
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
import com.kuter.inventory.StockAdjustmentItemRepository;
import com.kuter.inventory.StockAdjustmentRepository;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.inventory.StockReceivingItemRepository;
import com.kuter.inventory.StockReceivingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryAndProductApiTests {

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

    private UnitDefinition baseUnitPcs;
    private UnitDefinition boxUnit;

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

        // 1. Setup Tenant A and Owner A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("alpha-hardware")
                .name("Alpha Hardware & Masonry")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerA = userRepository.save(User.builder()
                .email("owner.a@alphahardware.com")
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

        // 2. Setup Cashier for Tenant A
        cashierA = userRepository.save(User.builder()
                .email("cashier.a@alphahardware.com")
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

        // 3. Setup Tenant B and Owner B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("beta-depot")
                .name("Beta Building Depot")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerB = userRepository.save(User.builder()
                .email("owner.b@betadepot.com")
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

        // 4. Base Units for Tenant A
        baseUnitPcs = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("BOX")
                .name("Box of 50")
                .build());
    }

    // =========================================================================
    // Category Tests
    // =========================================================================

    @Test
    @DisplayName("Create category successfully returns 201 Created")
    void createCategory_success() throws Exception {
        CreateCategoryDto dto = CreateCategoryDto.builder()
                .name("Plumbing & Fittings")
                .description("PVC pipes, elbows, valves, and fixtures")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Plumbing & Fittings")))
                .andExpect(jsonPath("$.data.active", is(true)))
                .andExpect(jsonPath("$.data.productCount", is(0)));
    }

    @Test
    @DisplayName("Duplicate category name within the same tenant is rejected with 400 Bad Request")
    void createCategory_duplicateNameInSameTenant_fails() throws Exception {
        categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Timber & Boards")
                .active(true)
                .build());

        CreateCategoryDto duplicateDto = CreateCategoryDto.builder()
                .name("timber & boards") // case-insensitive check
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Same category name can exist across different tenants")
    void createCategory_sameNameInDifferentTenant_succeeds() throws Exception {
        categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Electrical")
                .active(true)
                .build());

        CreateCategoryDto dtoForB = CreateCategoryDto.builder()
                .name("Electrical")
                .description("Beta electrical supplies")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoForB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Electrical")));
    }

    @Test
    @DisplayName("Cross-tenant category access is forbidden/not found")
    void crossTenantCategoryIsolation_notFound() throws Exception {
        Category catB = categoryRepository.save(Category.builder()
                .tenantId(tenantB.getId())
                .name("Tenant B Exclusive Category")
                .active(true)
                .build());

        // Owner A attempts to get Category B
        mockMvc.perform(get(Constants.API_BASE_PATH + "/categories/" + catB.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deleting category with linked products deactivates it instead of deleting")
    void deleteCategory_withProducts_deactivatesInsteadOfDeleting() throws Exception {
        Category masonry = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Masonry")
                .active(true)
                .build());

        productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("CBLK-6")
                .name("Concrete Block 6in")
                .categoryId(masonry.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("1.5000"))
                .sellingPrice(new BigDecimal("2.2000"))
                .status(ProductStatus.ACTIVE)
                .build());

        mockMvc.perform(delete(Constants.API_BASE_PATH + "/categories/" + masonry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk());

        Category updated = categoryRepository.findById(masonry.getId()).orElseThrow();
        assertThat(updated.isActive()).isFalse(); // soft deactivated
    }

    // =========================================================================
    // Unit Definition Tests
    // =========================================================================

    @Test
    @DisplayName("List units auto-seeds standard units if tenant has none")
    void listUnits_autoSeedsStandardUnits() throws Exception {
        // Tenant B has no units yet
        mockMvc.perform(get(Constants.API_BASE_PATH + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(5))));

        assertThat(unitRepository.findAllByTenantId(tenantB.getId())).isNotEmpty();
    }

    @Test
    @DisplayName("Create custom unit succeeds and duplicate code within tenant fails")
    void createUnit_crudAndDuplicateCheck() throws Exception {
        CreateUnitDto unitDto = CreateUnitDto.builder()
                .code("ROLL")
                .name("Roll of wire/sheet")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unitDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code", is("ROLL")));

        // Try duplicate
        mockMvc.perform(post(Constants.API_BASE_PATH + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unitDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    // =========================================================================
    // Product Tests
    // =========================================================================

    @Test
    @DisplayName("Create product with initial stock creates stock balance and initial movement")
    void createProduct_withInitialStock_createsBalanceAndMovement() throws Exception {
        Category cementCat = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Cement & Binders")
                .active(true)
                .build());

        CreateProductDto dto = CreateProductDto.builder()
                .sku("CEM-425N")
                .name("Portland Cement 42.5N 50kg")
                .description("General purpose structural cement")
                .categoryId(cementCat.getId())
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("9.5000"))
                .sellingPrice(new BigDecimal("12.0000"))
                .reorderThreshold(new BigDecimal("50.0000"))
                .initialQuantity(new BigDecimal("100.0000"))
                .barcode("600123456789")
                .imageUrl("https://cdn.kuter.example/cement-bag.png")
                .imageStorageKey("products/alpha/cem-425n.png")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.sku", is("CEM-425N")))
                .andExpect(jsonPath("$.data.quantityOnHand", is(100.0)))
                .andExpect(jsonPath("$.data.categoryName", is("Cement & Binders")))
                .andExpect(jsonPath("$.data.baseUnitCode", is("PCS")));

        // Verify balance and movement in DB
        Product product = productRepository.findByTenantIdAndSku(tenantA.getId(), "CEM-425N").orElseThrow();
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), product.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(balance.getAverageCost()).isEqualByComparingTo(new BigDecimal("9.5000"));

        List<StockMovement> movements = movementRepository.findAllByTenantId(tenantA.getId());
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getMovementType()).isEqualTo(MovementType.RECEIPT);
        assertThat(movements.get(0).getQuantityChange()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    @DisplayName("Image URL validation rejects unsafe schemes and allows valid URLs and paths")
    void createProduct_imageUrlValidation() throws Exception {
        CreateProductDto badDto = CreateProductDto.builder()
                .sku("BAD-IMG-1")
                .name("Unsafe Image Item")
                .baseUnitId(baseUnitPcs.getId())
                .imageUrl("javascript:alert('XSS')")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Image URL must use a secure protocol")));

        CreateProductDto validDto = CreateProductDto.builder()
                .sku("GOOD-IMG-1")
                .name("Safe Image Item")
                .baseUnitId(baseUnitPcs.getId())
                .imageUrl("/assets/products/item1.png")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Duplicate SKU within same tenant is rejected but allowed in another tenant")
    void createProduct_skuUniquenessPerTenant() throws Exception {
        productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("STL-ROD-12")
                .name("Steel Rebar 12mm")
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("5.0000"))
                .sellingPrice(new BigDecimal("7.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        CreateProductDto duplicateInA = CreateProductDto.builder()
                .sku("stl-rod-12") // case-insensitive check
                .name("Duplicate Rebar")
                .baseUnitId(baseUnitPcs.getId())
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateInA)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // Same SKU in Tenant B is allowed
        UnitDefinition unitB = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantB.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        CreateProductDto sameInB = CreateProductDto.builder()
                .sku("STL-ROD-12")
                .name("Tenant B Steel Rebar")
                .baseUnitId(unitB.getId())
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameInB)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku", is("STL-ROD-12")));
    }

    @Test
    @DisplayName("Cross-tenant product isolation: Tenant B cannot access Tenant A's product")
    void crossTenantProductIsolation() throws Exception {
        Product prodA = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("SEC-PROD-A")
                .name("Secret Product A")
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("15.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Tenant B owner attempts GET
        mockMvc.perform(get(Constants.API_BASE_PATH + "/products/" + prodA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString()))
                .andExpect(status().isNotFound());

        // Tenant B owner attempts PUT
        UpdateProductDto updateDto = UpdateProductDto.builder()
                .name("Hacked Product")
                .build();

        mockMvc.perform(put(Constants.API_BASE_PATH + "/products/" + prodA.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerBToken)
                        .header(Constants.TENANT_ID_HEADER, tenantB.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deleting product with movement history soft-deactivates instead of deleting")
    void deleteProduct_withMovements_softDeactivates() throws Exception {
        Product prod = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("DEL-TEST-1")
                .name("Product With Ledger History")
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("10.0000"))
                .sellingPrice(new BigDecimal("15.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(prod.getId())
                .quantityOnHand(new BigDecimal("20.0000"))
                .averageCost(new BigDecimal("10.0000"))
                .build());

        movementRepository.save(StockMovement.builder()
                .tenantId(tenantA.getId())
                .productId(prod.getId())
                .movementType(MovementType.RECEIPT)
                .quantityChange(new BigDecimal("20.0000"))
                .balanceAfter(new BigDecimal("20.0000"))
                .unitCost(new BigDecimal("10.0000"))
                .actorId(ownerA.getId())
                .build());

        mockMvc.perform(delete(Constants.API_BASE_PATH + "/products/" + prod.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk());

        // Verify product still exists in DB but is INACTIVE
        Product updated = productRepository.findById(prod.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    // =========================================================================
    // Product Unit Conversion Tests
    // =========================================================================

    @Test
    @DisplayName("Product unit conversion CRUD and validation rules")
    void productUnitConversions_crudAndValidation() throws Exception {
        Product prod = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .sku("NAIL-2IN")
                .name("Wire Nails 2 Inch")
                .baseUnitId(baseUnitPcs.getId())
                .costPrice(new BigDecimal("0.0500"))
                .sellingPrice(new BigDecimal("0.1000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // 1. Attempt to add base unit as conversion unit -> rejected 400
        CreateProductUnitConversionDto baseConversion = CreateProductUnitConversionDto.builder()
                .unitId(baseUnitPcs.getId())
                .conversionFactor(new BigDecimal("1.0000"))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products/" + prod.getId() + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(baseConversion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot add conversion rule for the product's base unit")));

        // 2. Add valid conversion: 1 BOX = 50 PCS
        CreateProductUnitConversionDto boxConversion = CreateProductUnitConversionDto.builder()
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("50.0000"))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products/" + prod.getId() + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(boxConversion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.unitCode", is("BOX")))
                .andExpect(jsonPath("$.data.conversionFactor", is(50.0)));

        // 3. List conversions for product
        mockMvc.perform(get(Constants.API_BASE_PATH + "/products/" + prod.getId() + "/units")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].baseUnit", is(true)))
                .andExpect(jsonPath("$.data[0].unitCode", is("PCS")))
                .andExpect(jsonPath("$.data[1].baseUnit", is(false)))
                .andExpect(jsonPath("$.data[1].unitCode", is("BOX")));

        // 4. Delete conversion
        mockMvc.perform(delete(Constants.API_BASE_PATH + "/products/" + prod.getId() + "/units/" + boxUnit.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString()))
                .andExpect(status().isOk());

        assertThat(conversionRepository.findAllByTenantIdAndProductId(tenantA.getId(), prod.getId())).isEmpty();
    }

    // =========================================================================
    // Role Authorization Tests
    // =========================================================================

    @Test
    @DisplayName("Cashier role cannot create products or categories (403 Forbidden)")
    void roleSecurity_cashierForbiddenFromCatalogModification() throws Exception {
        CreateCategoryDto catDto = CreateCategoryDto.builder()
                .name("Forbidden Cat")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catDto)))
                .andExpect(status().isForbidden());

        CreateProductDto prodDto = CreateProductDto.builder()
                .sku("CASH-PROD")
                .name("Cashier Product")
                .baseUnitId(baseUnitPcs.getId())
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prodDto)))
                .andExpect(status().isForbidden());
    }
}
