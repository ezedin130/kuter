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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleConcurrencyTests {

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
    private SaleRepository saleRepository;

    @Autowired
    private SaleItemRepository saleItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Tenant tenantA;
    private User ownerA;
    private String ownerToken;

    private UnitDefinition pieceUnit;
    private Category category;
    private Product productX;
    private Product productY;

    @BeforeEach
    void setUp() {
        saleItemRepository.deleteAll();
        saleRepository.deleteAll();
        movementRepository.deleteAll();
        balanceRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Setup Tenant
        tenantA = tenantRepository.save(Tenant.builder()
                .code("concurrent-hardware")
                .name("Concurrent Hardware Ltd")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        // 2. Setup Owner
        ownerA = userRepository.save(User.builder()
                .email("owner@concurrent.example")
                .fullName("Connie Concurrent")
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

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwner)));

        // 3. Units & Category
        pieceUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenantA.getId())
                .code("PCS")
                .name("Pieces")
                .build());

        category = categoryRepository.save(Category.builder()
                .tenantId(tenantA.getId())
                .name("Metals & Wire")
                .description("Metal bars and structural shapes")
                .active(true)
                .build());

        // 4. Products
        productX = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .categoryId(category.getId())
                .baseUnitId(pieceUnit.getId())
                .sku("BAR-COPPER-10")
                .name("Copper Bar 10mm")
                .sellingPrice(new BigDecimal("25.0000"))
                .costPrice(new BigDecimal("15.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        productY = productRepository.save(Product.builder()
                .tenantId(tenantA.getId())
                .categoryId(category.getId())
                .baseUnitId(pieceUnit.getId())
                .sku("BAR-BRASS-10")
                .name("Brass Bar 10mm")
                .sellingPrice(new BigDecimal("20.0000"))
                .costPrice(new BigDecimal("12.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Initial inventory:
        // Product X: 100 PCS on hand
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(productX.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("15.0000"))
                .build());

        // Product Y: 100 PCS on hand
        balanceRepository.save(StockBalance.builder()
                .tenantId(tenantA.getId())
                .productId(productY.getId())
                .quantityOnHand(new BigDecimal("100.0000"))
                .averageCost(new BigDecimal("12.0000"))
                .build());
    }

    @Test
    @DisplayName("Concurrent sales competing for limited stock: exactly 3 succeed, 2 fail, stock balance = 10")
    void concurrentSales_limitedStock_pessimisticLockGuaranteesIntegrity() throws Exception {
        int threadCount = 5;
        BigDecimal requestQuantity = new BigDecimal("30.0000"); // 5 * 30 = 150 attempted, only 100 available
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Ensure all threads start simultaneously

                    CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                            .customerName("Concurrent Buyer " + index)
                            .items(List.of(
                                    CreateSaleItemRequestDto.builder()
                                            .productId(productX.getId())
                                            .unitDefinitionId(pieceUnit.getId())
                                            .quantity(requestQuantity)
                                            .build()
                            ))
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/v1/sales")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                                    .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 400) {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(3);
        assertThat(failureCount.get()).isEqualTo(2);

        // Verify stock balance in DB: 100 - (3 * 30) = 10
        StockBalance balance = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), productX.getId()).orElseThrow();
        assertThat(balance.getQuantityOnHand()).isEqualByComparingTo("10.0000");
    }

    @Test
    @DisplayName("Concurrent multi-product sales in opposing order execute without deadlock")
    void concurrentSales_opposingOrder_noDeadlock() throws Exception {
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Thread 0 and 2: Buy [Product X, Product Y]
        // Thread 1 and 3: Buy [Product Y, Product X]
        for (int i = 0; i < threadCount; i++) {
            final boolean reverseOrder = (i % 2 != 0);
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    List<CreateSaleItemRequestDto> items = new ArrayList<>();
                    if (reverseOrder) {
                        items.add(CreateSaleItemRequestDto.builder()
                                .productId(productY.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .build());
                        items.add(CreateSaleItemRequestDto.builder()
                                .productId(productX.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .build());
                    } else {
                        items.add(CreateSaleItemRequestDto.builder()
                                .productId(productX.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .build());
                        items.add(CreateSaleItemRequestDto.builder()
                                .productId(productY.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("5.0000"))
                                .build());
                    }

                    CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                            .customerName("Dual Product Buyer " + index)
                            .items(items)
                            .build();

                    MvcResult result = mockMvc.perform(post("/api/v1/sales")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                                    .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(exceptions).isEmpty();
        assertThat(successCount.get()).isEqualTo(4);

        // 4 sales * 5 PCS = 20 PCS deducted for each product (100 - 20 = 80)
        StockBalance balanceX = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), productX.getId()).orElseThrow();
        StockBalance balanceY = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), productY.getId()).orElseThrow();
        assertThat(balanceX.getQuantityOnHand()).isEqualByComparingTo("80.0000");
        assertThat(balanceY.getQuantityOnHand()).isEqualByComparingTo("80.0000");
    }

    @Test
    @DisplayName("Concurrent duplicate idempotency keys result in single sale and single deduction")
    void concurrentSales_duplicateIdempotencyKey_singleDeduction() throws Exception {
        int threadCount = 2;
        String sharedKey = "tx-shared-concurrent-" + UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        CreateSaleRequestDto request = CreateSaleRequestDto.builder()
                .customerName("Idempotency Buyer")
                .idempotencyKey(sharedKey)
                .items(List.of(
                        CreateSaleItemRequestDto.builder()
                                .productId(productX.getId())
                                .unitDefinitionId(pieceUnit.getId())
                                .quantity(new BigDecimal("15.0000"))
                                .build()
                ))
                .build();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    MvcResult result = mockMvc.perform(post("/api/v1/sales")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                                    .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                            .andReturn();

                    int statusCode = result.getResponse().getStatus();
                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one created or replayed; total responses accounted for
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);
        assertThat(successCount.get() + conflictCount.get()).isEqualTo(2);

        // Crucial: Stock deducted ONLY ONCE for 15.0000 (100 - 15 = 85)
        StockBalance balanceX = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), productX.getId()).orElseThrow();
        assertThat(balanceX.getQuantityOnHand()).isEqualByComparingTo("85.0000");

        // Subsequent call with the same idempotency key replays 201 Created without re-deducting stock
        mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.idempotencyKey", is(sharedKey)));

        StockBalance balanceAfterReplay = balanceRepository.findByTenantIdAndProductId(tenantA.getId(), productX.getId()).orElseThrow();
        assertThat(balanceAfterReplay.getQuantityOnHand()).isEqualByComparingTo("85.0000");
    }
}
