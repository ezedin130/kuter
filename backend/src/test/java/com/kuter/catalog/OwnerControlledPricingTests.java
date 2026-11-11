package com.kuter.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.dto.CreateCategoryDto;
import com.kuter.catalog.dto.CreateProductDto;
import com.kuter.catalog.dto.CreateProductUnitConversionDto;
import com.kuter.catalog.dto.CreateUnitDto;
import com.kuter.catalog.dto.UpdateSellingPriceDto;
import com.kuter.common.Constants;
import com.kuter.config.JwtService;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.ShopPermission;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.inventory.InventoryService;
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CreateStockReceivingItemDto;
import com.kuter.sales.dto.CreateSaleRequestDto;
import com.kuter.sales.dto.CreateSaleItemRequestDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantContext;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerControlledPricingTests {

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
    private InventoryService inventoryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant tenant;
    private User owner;
    private User cashier;
    private String ownerToken;
    private String cashierToken;

    private UnitDefinition pcsUnit;
    private UnitDefinition boxUnit;
    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM stock_loan_returns");
        jdbcTemplate.execute("DELETE FROM stock_loans");
        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM stock_adjustment_items");
        jdbcTemplate.execute("DELETE FROM stock_adjustments");
        jdbcTemplate.execute("DELETE FROM stock_receiving_items");
        jdbcTemplate.execute("DELETE FROM stock_receivings");
        jdbcTemplate.execute("DELETE FROM product_unit_conversions");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM unit_definitions");
        jdbcTemplate.execute("DELETE FROM tenant_onboarding_requests");
        jdbcTemplate.execute("DELETE FROM tenant_subscriptions");
        jdbcTemplate.execute("DELETE FROM tenant_membership_permissions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");

        tenant = tenantRepository.save(Tenant.builder()
                .code("delta-paints")
                .name("Delta Paints & Adhesives")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        owner = userRepository.save(User.builder()
                .email("owner@deltapaints.example")
                .fullName("Dana Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership ownerMem = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(ShopPermission.values()))
                .build());

        cashier = userRepository.save(User.builder()
                .email("cashier@deltapaints.example")
                .fullName("Chris Cashier")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership cashierMem = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(cashier.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(ShopPermission.PRODUCTS_VIEW, ShopPermission.SALES_CREATE, ShopPermission.SALES_VIEW))
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(ownerMem)));
        cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashier, List.of(cashierMem)));

        pcsUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        boxUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BOX")
                .name("Box of 10")
                .build());

        category = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Adhesives")
                .active(true)
                .build());

        product = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("ADH-500ML")
                .name("SuperBond Adhesive 500ml")
                .categoryId(category.getId())
                .baseUnitId(pcsUnit.getId())
                .sellingPrice(new BigDecimal("25.0000"))
                .costPrice(new BigDecimal("15.0000"))
                .reorderThreshold(new BigDecimal("10.0000"))
                .build());

        conversionRepository.save(ProductUnitConversion.builder()
                .tenantId(tenant.getId())
                .productId(product.getId())
                .unitId(boxUnit.getId())
                .conversionFactor(new BigDecimal("10.0000"))
                .build());

        // Receive initial stock of 100 PCS at $15 cost
        TenantContext.setTenantId(tenant.getId());
        try {
            KuterUserPrincipal ownerPrincipal = new KuterUserPrincipal(owner, List.of(ownerMem));
            inventoryService.receiveStock(CreateStockReceivingDto.builder()
                    .supplierName("GlueCo")
                    .receivedAt(Instant.now())
                    .items(List.of(CreateStockReceivingItemDto.builder()
                            .productId(product.getId())
                            .receivedUnitId(pcsUnit.getId())
                            .receivedQuantity(new BigDecimal("100.0000"))
                            .unitCost(new BigDecimal("15.0000"))
                            .build()))
                    .build(), ownerPrincipal, null);
        } finally {
            TenantContext.clear();
        }
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM stock_loan_returns");
        jdbcTemplate.execute("DELETE FROM stock_loans");
        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM stock_receiving_items");
        jdbcTemplate.execute("DELETE FROM stock_receivings");
        jdbcTemplate.execute("DELETE FROM product_unit_conversions");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM unit_definitions");
        jdbcTemplate.execute("DELETE FROM tenant_membership_permissions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");
        TenantContext.clear();
    }

    @Test
    @DisplayName("Owner can update selling price; staff without PRICES_MANAGE is denied (403 Forbidden)")
    void ownerCanUpdateSellingPriceAndStaffDenied() throws Exception {
        UpdateSellingPriceDto priceUpdate = UpdateSellingPriceDto.builder()
                .sellingPrice(new BigDecimal("29.9900"))
                .build();

        // 1. Staff attempt -> 403 Forbidden
        mockMvc.perform(patch(Constants.API_BASE_PATH + "/products/" + product.getId() + "/selling-price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceUpdate)))
                .andExpect(status().isForbidden());

        // 2. Owner attempt -> 200 OK
        mockMvc.perform(patch(Constants.API_BASE_PATH + "/products/" + product.getId() + "/selling-price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sellingPrice", is(29.99)));

        // Verify product in database has new selling price but costPrice is untouched ($15.0000)
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getSellingPrice()).isEqualByComparingTo("29.9900");
        assertThat(updated.getCostPrice()).isEqualByComparingTo("15.0000");
    }

    @Test
    @DisplayName("POS Checkout: Tampered unit price deviation is rejected with 400 Bad Request")
    void posCheckoutRejectsPriceTampering() throws Exception {
        // Cashier attempts to sell product (selling price is $25.00) at unauthorized $10.00
        CreateSaleRequestDto tamperedSale = CreateSaleRequestDto.builder()
                .customerName("Discount Seeker")
                .items(List.of(CreateSaleItemRequestDto.builder()
                        .productId(product.getId())
                        .unitDefinitionId(pcsUnit.getId())
                        .quantity(new BigDecimal("2.0000"))
                        .unitPrice(new BigDecimal("10.0000")) // Tampered!
                        .build()))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperedSale)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match the authorized owner price")));
    }

    @Test
    @DisplayName("POS Checkout: Valid price matching owner setting succeeds and deducts inventory")
    void posCheckoutWithAuthorizedPriceSucceeds() throws Exception {
        CreateSaleRequestDto validSale = CreateSaleRequestDto.builder()
                .customerName("Walk-in Contractor")
                .items(List.of(CreateSaleItemRequestDto.builder()
                        .productId(product.getId())
                        .unitDefinitionId(pcsUnit.getId())
                        .quantity(new BigDecimal("5.0000"))
                        .unitPrice(new BigDecimal("25.0000")) // Matches owner price
                        .build()))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validSale)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(125.0)));
    }

    @Test
    @DisplayName("POS Checkout: Multi-unit conversion price validation (Box of 10 @ $250.00)")
    void posCheckoutValidatesConvertedUnitPrice() throws Exception {
        // 1 Box = 10 Pcs, base selling price = $25.00 -> Box price must be $250.00
        // If cashier sends tampered box price ($200.00) -> rejected
        CreateSaleRequestDto tamperedBoxSale = CreateSaleRequestDto.builder()
                .customerName("Wholesale Buyer")
                .items(List.of(CreateSaleItemRequestDto.builder()
                        .productId(product.getId())
                        .unitDefinitionId(boxUnit.getId())
                        .quantity(new BigDecimal("1.0000"))
                        .unitPrice(new BigDecimal("200.0000")) // Tampered!
                        .build()))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperedBoxSale)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("does not match the authorized owner price")));

        // Accurate box price ($250.00) -> succeeds
        CreateSaleRequestDto validBoxSale = CreateSaleRequestDto.builder()
                .customerName("Wholesale Buyer")
                .items(List.of(CreateSaleItemRequestDto.builder()
                        .productId(product.getId())
                        .unitDefinitionId(boxUnit.getId())
                        .quantity(new BigDecimal("2.0000"))
                        .unitPrice(new BigDecimal("250.0000"))
                        .build()))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBoxSale)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalAmount", is(500.0)));
    }
}
