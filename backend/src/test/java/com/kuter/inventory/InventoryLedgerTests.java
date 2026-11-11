package com.kuter.inventory;

import com.kuter.catalog.Category;
import com.kuter.catalog.CategoryRepository;
import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InventoryLedgerTests {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

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
    private StockReceivingRepository receivingRepository;

    @Autowired
    private StockReceivingItemRepository receivingItemRepository;

    @Autowired
    private StockAdjustmentRepository adjustmentRepository;

    private Tenant tenant;
    private User yardManager;
    private Product cementProduct;
    private UnitDefinition bagUnit;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        receivingItemRepository.deleteAll();
        receivingRepository.deleteAll();
        balanceRepository.deleteAll();
        adjustmentRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .code("kuter-depot")
                .name("Kuter Central Building Depot")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        yardManager = userRepository.save(User.builder()
                .email("yard@kuter.com")
                .passwordHash("hashed_pw")
                .fullName("John Yard Master")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(false)
                .build());

        Category cementCat = categoryRepository.save(Category.builder()
                .tenantId(tenant.getId())
                .name("Cement & Binding")
                .build());

        bagUnit = unitRepository.save(UnitDefinition.builder()
                .tenantId(tenant.getId())
                .code("BAG")
                .name("50kg Bag")
                .build());

        cementProduct = productRepository.save(Product.builder()
                .tenantId(tenant.getId())
                .sku("CEM-50KG")
                .name("Dangote 3X 50kg Cement")
                .categoryId(cementCat.getId())
                .baseUnitId(bagUnit.getId())
                .costPrice(new BigDecimal("7.5000"))
                .sellingPrice(new BigDecimal("9.2000"))
                .reorderThreshold(new BigDecimal("100.0000"))
                .status(ProductStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("StockBalance tracks on-hand quantity and moving average cost with 4-decimal precision")
    void stockBalanceTracking() {
        StockBalance balance = balanceRepository.save(StockBalance.builder()
                .tenantId(tenant.getId())
                .productId(cementProduct.getId())
                .quantityOnHand(new BigDecimal("1000.0000"))
                .averageCost(new BigDecimal("7.5000"))
                .lastCountedAt(Instant.now())
                .build());

        assertThat(balance.getId()).isNotNull();
        assertThat(balance.getCreatedAt()).isNotNull();
        assertThat(balance.getUpdatedAt()).isNotNull();

        Optional<StockBalance> retrieved = balanceRepository.findByTenantIdAndProductId(
                tenant.getId(), cementProduct.getId()
        );
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getQuantityOnHand()).isEqualByComparingTo("1000.0000");
        assertThat(retrieved.get().getAverageCost()).isEqualByComparingTo("7.5000");
    }

    @Test
    @DisplayName("StockMovement append-only ledger entries record full chronology of yard movements")
    void stockMovementAuditTrail() throws Exception {
        UUID receivingRef = UUID.randomUUID();
        UUID saleRef = UUID.randomUUID();
        UUID adjRef = UUID.randomUUID();

        // 1. Initial receiving: +1000 bags
        movementRepository.save(StockMovement.builder()
                .tenantId(tenant.getId())
                .productId(cementProduct.getId())
                .movementType(MovementType.RECEIPT)
                .referenceId(receivingRef)
                .referenceType("STOCK_RECEIVING")
                .quantityChange(new BigDecimal("1000.0000"))
                .balanceAfter(new BigDecimal("1000.0000"))
                .unitCost(new BigDecimal("7.5000"))
                .actorId(yardManager.getId())
                .notes("GRN-2026-001 Delivery from factory")
                .build());

        Thread.sleep(15);

        // 2. Counter sale: -50 bags
        movementRepository.save(StockMovement.builder()
                .tenantId(tenant.getId())
                .productId(cementProduct.getId())
                .movementType(MovementType.SALE)
                .referenceId(saleRef)
                .referenceType("SALE")
                .quantityChange(new BigDecimal("-50.0000"))
                .balanceAfter(new BigDecimal("950.0000"))
                .unitCost(new BigDecimal("7.5000"))
                .actorId(yardManager.getId())
                .notes("INV-2026-101 Walk-in customer")
                .build());

        Thread.sleep(15);

        // 3. Spillage/breakage adjustment: -5 bags
        movementRepository.save(StockMovement.builder()
                .tenantId(tenant.getId())
                .productId(cementProduct.getId())
                .movementType(MovementType.ADJUSTMENT)
                .referenceId(adjRef)
                .referenceType("STOCK_ADJUSTMENT")
                .quantityChange(new BigDecimal("-5.0000"))
                .balanceAfter(new BigDecimal("945.0000"))
                .unitCost(new BigDecimal("7.5000"))
                .actorId(yardManager.getId())
                .notes("ADJ-2026-001 Forklift puncture damaged 5 bags")
                .build());

        List<StockMovement> ledger = movementRepository.findByTenantIdAndProductIdOrderByCreatedAtDesc(
                tenant.getId(), cementProduct.getId()
        );

        assertThat(ledger).hasSize(3);
        // Latest movement should have balance_after = 945.0000
        assertThat(ledger.get(0).getBalanceAfter()).isEqualByComparingTo("945.0000");
        assertThat(ledger.get(0).getMovementType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(ledger.get(0).getQuantityChange()).isEqualByComparingTo("-5.0000");

        // Earliest movement should be the initial receiving
        assertThat(ledger.get(2).getMovementType()).isEqualTo(MovementType.RECEIPT);
        assertThat(ledger.get(2).getQuantityChange()).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("StockReceiving document and items persist with supplier and unit reference")
    void stockReceivingDocumentPersistence() {
        StockReceiving receiving = receivingRepository.save(StockReceiving.builder()
                .tenantId(tenant.getId())
                .receivingNumber("GRN-2026-0042")
                .supplierName("National Cement Mills Ltd")
                .deliveryNoteRef("DN-88392")
                .status("COMPLETED")
                .totalCost(new BigDecimal("15000.0000"))
                .receivedBy(yardManager.getId())
                .receivedAt(Instant.now())
                .notes("Arrived in good condition via trailer TR-04")
                .build());

        StockReceivingItem item = receivingItemRepository.save(StockReceivingItem.builder()
                .tenantId(tenant.getId())
                .receivingId(receiving.getId())
                .productId(cementProduct.getId())
                .receivedUnitId(bagUnit.getId())
                .receivedQuantity(new BigDecimal("2000.0000"))
                .conversionFactor(BigDecimal.ONE)
                .baseQuantity(new BigDecimal("2000.0000"))
                .unitCost(new BigDecimal("7.5000"))
                .totalCost(new BigDecimal("15000.0000"))
                .build());

        assertThat(receiving.getId()).isNotNull();
        assertThat(item.getId()).isNotNull();

        Optional<StockReceiving> fetched = receivingRepository.findByTenantIdAndReceivingNumber(
                tenant.getId(), "GRN-2026-0042"
        );
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getSupplierName()).isEqualTo("National Cement Mills Ltd");

        List<StockReceivingItem> items = receivingItemRepository.findAllByTenantIdAndReceivingId(
                tenant.getId(), receiving.getId()
        );
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getBaseQuantity()).isEqualByComparingTo("2000.0000");
    }
}
