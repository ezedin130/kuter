package com.kuter.lending;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
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
import com.kuter.inventory.MovementType;
import com.kuter.inventory.StockBalance;
import com.kuter.inventory.StockBalanceRepository;
import com.kuter.inventory.StockMovement;
import com.kuter.inventory.StockMovementRepository;
import com.kuter.lending.dto.CreateBorrowingRequestDto;
import com.kuter.lending.dto.CreateLoanRequestDto;
import com.kuter.lending.dto.RecordLoanReturnDto;
import com.kuter.sales.SaleRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
class InventoryLendingAndBorrowingTests {

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
    private StockLoanRepository loanRepository;

    @Autowired
    private StockLoanReturnRepository loanReturnRepository;

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Tenant tenant;
    private User owner;
    private User storekeeper;
    private String ownerToken;
    private String storekeeperToken;

    private UnitDefinition bagUnit;
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
                .code("cement-central")
                .name("Cement Central Depot")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        owner = userRepository.save(User.builder()
                .email("owner@cementcentral.example")
                .fullName("Depot Owner")
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

        storekeeper = userRepository.save(User.builder()
                .email("store@cementcentral.example")
                .fullName("Sam Storekeeper")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership storekeeperMem = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(storekeeper.getId())
                .role(TenantRole.STOREKEEPER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(
                        ShopPermission.PRODUCTS_VIEW,
                        ShopPermission.INVENTORY_VIEW,
                        ShopPermission.INVENTORY_RECEIVE,
                        ShopPermission.INVENTORY_ADJUST,
                        ShopPermission.LENDING_VIEW,
                        ShopPermission.LENDING_REQUEST,
                        ShopPermission.LENDING_MANAGE
                ))
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(ownerMem)));
        storekeeperToken = jwtService.generateToken(new KuterUserPrincipal(storekeeper, List.of(storekeeperMem)));

        bagUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BAG")
                .name("Bags (50kg)")
                .build());

        category = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Masonry")
                .active(true)
                .build());

        product = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("CEM-OPC-50KG")
                .name("Portland Cement 50kg")
                .categoryId(category.getId())
                .baseUnitId(bagUnit.getId())
                .sellingPrice(new BigDecimal("12.5000"))
                .costPrice(new BigDecimal("9.0000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .build());

        // Initialize zero stock balance
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(product.getId())
                .quantityOnHand(BigDecimal.ZERO)
                .averageCost(BigDecimal.ZERO)
                .build());
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM stock_loan_returns");
        jdbcTemplate.execute("DELETE FROM stock_loans");
        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM product_unit_conversions");
        jdbcTemplate.execute("DELETE FROM products");
        jdbcTemplate.execute("DELETE FROM categories");
        jdbcTemplate.execute("DELETE FROM unit_definitions");
        jdbcTemplate.execute("DELETE FROM tenant_membership_permissions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM tenants");
    }

    @Test
    @DisplayName("Record borrowing from external shop: increases inventoryOnHand and generates BORROW_RECEIPT movement")
    void recordBorrowingIncreasesStock() throws Exception {
        CreateBorrowingRequestDto borrowDto = CreateBorrowingRequestDto.builder()
                .counterpartyName("Apex Builders Supply (External)")
                .counterpartyContactPerson("Mark Apex")
                .counterpartyPhone("+1-555-0199")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("50.0000"))
                .transactionDate(Instant.now())
                .notes("Emergency stock borrow due to stockout")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/borrowings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrowDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.direction", is("BORROW")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.initialQuantity", is(50.0)))
                .andExpect(jsonPath("$.data.outstandingBaseQuantity", is(50.0)))
                .andExpect(jsonPath("$.data.counterpartyName", is("Apex Builders Supply (External)")));

        // Verify inventory balance increased to 50
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("50.0000");

        // Verify stock movement created with BORROW_RECEIPT
        List<StockMovement> movements = movementRepository.findAllByTenantId(tenant.getId());
        assertThat(movements).hasSize(1);
        StockMovement mov = movements.get(0);
        assertThat(mov.getMovementType()).isEqualTo(MovementType.BORROW_RECEIPT);
        assertThat(mov.getQuantityChange()).isEqualByComparingTo("50.0000");
        assertThat(mov.getBalanceAfter()).isEqualByComparingTo("50.0000");

        // Verify ZERO sales revenue created
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Record loan to external shop: validates stock, decreases inventoryOnHand, and generates LOAN_DISPATCH movement")
    void recordLoanDecreasesStock() throws Exception {
        // Set stock on hand to 30
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        balance.setQuantityOnHand(new BigDecimal("30.0000"));
        balanceRepository.save(balance);

        // 1. Attempt to lend 40 (insufficient stock) -> 400 Bad Request
        CreateLoanRequestDto excessiveLoan = CreateLoanRequestDto.builder()
                .counterpartyName("Metro Contractors (External)")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("40.0000"))
                .transactionDate(Instant.now())
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessiveLoan)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Insufficient available stock to loan product")));

        // 2. Lend 20 -> 201 Created
        CreateLoanRequestDto validLoan = CreateLoanRequestDto.builder()
                .counterpartyName("Metro Contractors (External)")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("20.0000"))
                .transactionDate(Instant.now())
                .notes("Lending 20 bags until Friday")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLoan)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.direction", is("LOAN")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.outstandingBaseQuantity", is(20.0)));

        // Verify inventory balance decreased from 30 to 10
        StockBalance updatedBalance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        assertThat(updatedBalance.getQuantityOnHand()).isEqualByComparingTo("10.0000");

        // Verify stock movement created with LOAN_DISPATCH
        List<StockMovement> movements = movementRepository.findAllByTenantId(tenant.getId());
        assertThat(movements).hasSize(1);
        StockMovement mov = movements.get(0);
        assertThat(mov.getMovementType()).isEqualTo(MovementType.LOAN_DISPATCH);
        assertThat(mov.getQuantityChange()).isEqualByComparingTo("-20.0000");
        assertThat(mov.getBalanceAfter()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Multi-step partial returns for borrowed stock: updates remaining balance and stock ledger")
    void multiStepPartialReturnsForBorrowing() throws Exception {
        // Step 1: Borrow 40 bags
        CreateBorrowingRequestDto borrowDto = CreateBorrowingRequestDto.builder()
                .counterpartyName("Apex Builders Supply")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("40.0000"))
                .transactionDate(Instant.now())
                .build();

        String borrowJson = mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/borrowings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrowDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID loanId = UUID.fromString(objectMapper.readTree(borrowJson).get("data").get("id").asText());

        // Step 2: Return first partial quantity of 15 bags
        RecordLoanReturnDto returnPart1 = RecordLoanReturnDto.builder()
                .quantity(new BigDecimal("15.0000"))
                .unitDefinitionId(bagUnit.getId())
                .returnDate(Instant.now())
                .notes("Returned partial 15 bags")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/lending/" + loanId + "/returns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnPart1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("PARTIALLY_RETURNED")))
                .andExpect(jsonPath("$.data.returnedQuantity", is(15.0)))
                .andExpect(jsonPath("$.data.outstandingBaseQuantity", is(25.0)));

        // Inventory was 40, now 25
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("25.0000");

        // Step 3: Over-return attempt (trying to return 30 when only 25 remain) -> 400 Bad Request
        RecordLoanReturnDto overReturn = RecordLoanReturnDto.builder()
                .quantity(new BigDecimal("30.0000"))
                .unitDefinitionId(bagUnit.getId())
                .returnDate(Instant.now())
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/lending/" + loanId + "/returns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overReturn)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("exceeds outstanding obligation")));

        // Step 4: Complete remaining return of 25 bags -> status becomes RETURNED
        RecordLoanReturnDto returnPart2 = RecordLoanReturnDto.builder()
                .quantity(new BigDecimal("25.0000"))
                .unitDefinitionId(bagUnit.getId())
                .returnDate(Instant.now())
                .notes("Final settlement")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/lending/" + loanId + "/returns")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(returnPart2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("RETURNED")))
                .andExpect(jsonPath("$.data.outstandingBaseQuantity", is(0.0)));

        // Final balance is 0
        StockBalance finalBalance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        assertThat(finalBalance.getQuantityOnHand()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("Lending Obligations Summary aggregates pending borrowed and loaned amounts accurately")
    void lendingObligationsSummaryCalculation() throws Exception {
        // Initial balance 100
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenant.getId(), product.getId()).orElseThrow();
        balance.setQuantityOnHand(new BigDecimal("100.0000"));
        balanceRepository.save(balance);

        // 1. Borrow 20 bags from Shop X
        CreateBorrowingRequestDto borrow = CreateBorrowingRequestDto.builder()
                .counterpartyName("Shop X")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("20.0000"))
                .transactionDate(Instant.now())
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/borrowings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(borrow)))
                .andExpect(status().isCreated());

        // 2. Lend 35 bags to Shop Y
        CreateLoanRequestDto loan = CreateLoanRequestDto.builder()
                .counterpartyName("Shop Y")
                .productId(product.getId())
                .unitDefinitionId(bagUnit.getId())
                .quantity(new BigDecimal("35.0000"))
                .transactionDate(Instant.now())
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/inventory/loans")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loan)))
                .andExpect(status().isCreated());

        // 3. Query Obligations Summary
        mockMvc.perform(get(Constants.API_BASE_PATH + "/inventory/lending/obligations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storekeeperToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveBorrowings", is(1)))
                .andExpect(jsonPath("$.data.totalActiveLoans", is(1)))
                .andExpect(jsonPath("$.data.productObligations", hasSize(1)))
                .andExpect(jsonPath("$.data.productObligations[0].productSku", is("CEM-OPC-50KG")))
                .andExpect(jsonPath("$.data.productObligations[0].totalBorrowedBaseQuantity", is(20.0)))
                .andExpect(jsonPath("$.data.productObligations[0].totalLoanedBaseQuantity", is(35.0)))
                .andExpect(jsonPath("$.data.productObligations[0].netObligationBaseQuantity", is(-15.0)));
    }
}
