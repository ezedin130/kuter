package com.kuter;

import com.kuter.health.HealthResponse;
import com.kuter.health.HealthService;
import com.kuter.health.SystemStatus;
import com.kuter.health.SystemStatusRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/kuter_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=1122",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.clean-disabled=true"
})
class PostgreSqlIntegrationTests {

    @Autowired
    private SystemStatusRepository systemStatusRepository;

    @Autowired
    private HealthService healthService;

    @Autowired
    private com.kuter.tenancy.TenantRepository tenantRepository;

    @Autowired
    private com.kuter.subscriptions.SubscriptionPlanRepository planRepository;

    @Autowired
    private com.kuter.catalog.UnitDefinitionRepository unitRepository;

    @Autowired
    private com.kuter.catalog.ProductRepository productRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify PostgreSQL database connection, Flyway migration, and SystemStatus entity query")
    void verifyPostgresConnectivityAndMigration() {
        Optional<SystemStatus> status = systemStatusRepository.findByStatusName("DATABASE_INITIALIZED");

        assertThat(status).isPresent();
        assertThat(status.get().getStatusValue()).isEqualTo("SUCCESS");

        HealthResponse health = healthService.getHealthStatus();
        assertThat(health.status()).isEqualTo("UP");
        assertThat(health.database()).isEqualTo("CONNECTED");
    }

    @Test
    @DisplayName("Verify V11 migration successfully dropped audit_logs table from PostgreSQL")
    void verifyV11MigrationAuditLogsTableDroppedOnPostgres() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'audit_logs'",
                Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify full core domain persistence on PostgreSQL with UUIDs and audit timestamps")
    void verifyPostgresCoreDomainPersistence() {
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        String planCode = "pro-" + uniqueSuffix;
        String tenantCode = "pg-shop-" + uniqueSuffix;
        String sku = "STL-16MM-" + uniqueSuffix;

        // 1. Subscription Plan
        com.kuter.subscriptions.SubscriptionPlan plan = planRepository.save(
                com.kuter.subscriptions.SubscriptionPlan.builder()
                        .code(planCode)
                        .name("Professional Monthly")
                        .description("Up to 5000 products, 10 users")
                        .price(new java.math.BigDecimal("49.9900"))
                        .billingInterval(com.kuter.subscriptions.BillingInterval.MONTHLY)
                        .productLimit(5000)
                        .userLimit(10)
                        .isPublic(true)
                        .isActive(true)
                        .build()
        );
        assertThat(plan.getId()).isNotNull();

        // 2. Tenant
        com.kuter.tenancy.Tenant tenant = tenantRepository.save(
                com.kuter.tenancy.Tenant.builder()
                        .code(tenantCode)
                        .name("PostgreSQL Building Yard")
                        .businessRegistrationNumber("RC-883719")
                        .status(com.kuter.tenancy.TenantStatus.ACTIVE)
                        .currency("NGN")
                        .timezone("Africa/Lagos")
                        .build()
        );
        assertThat(tenant.getId()).isNotNull();
        assertThat(tenant.getCreatedAt()).isNotNull();
        assertThat(tenant.getUpdatedAt()).isNotNull();

        // 3. Unit Definition
        com.kuter.catalog.UnitDefinition unit = unitRepository.save(
                com.kuter.catalog.UnitDefinition.builder()
                        .tenantId(tenant.getId())
                        .code("BUNDLE")
                        .name("Steel Rebar Bundle")
                        .build()
        );
        assertThat(unit.getId()).isNotNull();

        // 4. Product with scale 4 monetary fields
        com.kuter.catalog.Product product = productRepository.save(
                com.kuter.catalog.Product.builder()
                        .tenantId(tenant.getId())
                        .sku(sku)
                        .name("16mm TMT Steel Rebar Bundle")
                        .baseUnitId(unit.getId())
                        .costPrice(new java.math.BigDecimal("350.0000"))
                        .sellingPrice(new java.math.BigDecimal("420.0000"))
                        .reorderThreshold(new java.math.BigDecimal("15.0000"))
                        .status(com.kuter.catalog.ProductStatus.ACTIVE)
                        .brand("Tiger TMT")
                        .build()
        );
        assertThat(product.getId()).isNotNull();
        assertThat(product.getCreatedAt()).isNotNull();

        Optional<com.kuter.catalog.Product> fetched = productRepository.findByTenantIdAndSku(
                tenant.getId(), sku
        );
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getName()).isEqualTo("16mm TMT Steel Rebar Bundle");
        assertThat(fetched.get().getBaseUnitId()).isEqualTo(unit.getId());
    }

    @Autowired
    private com.kuter.lending.StockLoanRepository stockLoanRepository;

    @Test
    @DisplayName("Verify searchLoans specification works seamlessly on PostgreSQL with null parameters")
    void verifyPostgresStockLoanSpecificationSearchWithNullParameters() {
        String uniqueSuffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        com.kuter.tenancy.Tenant tenant = tenantRepository.save(
                com.kuter.tenancy.Tenant.builder()
                        .code("pg-loan-" + uniqueSuffix)
                        .name("PG Loan Depot")
                        .currency("ETB")
                        .timezone("UTC")
                        .status(com.kuter.tenancy.TenantStatus.ACTIVE)
                        .build()
        );

        com.kuter.catalog.UnitDefinition unit = unitRepository.save(
                com.kuter.catalog.UnitDefinition.builder()
                        .tenantId(tenant.getId())
                        .code("BAG")
                        .name("Bag")
                        .build()
        );

        com.kuter.catalog.Product prod = productRepository.save(
                com.kuter.catalog.Product.builder()
                        .tenantId(tenant.getId())
                        .sku("CEM-" + uniqueSuffix)
                        .name("Cement 50kg")
                        .baseUnitId(unit.getId())
                        .costPrice(new java.math.BigDecimal("10.0000"))
                        .sellingPrice(new java.math.BigDecimal("15.0000"))
                        .reorderThreshold(new java.math.BigDecimal("5.0000"))
                        .status(com.kuter.catalog.ProductStatus.ACTIVE)
                        .build()
        );

        com.kuter.lending.StockLoan loan = stockLoanRepository.save(
                com.kuter.lending.StockLoan.builder()
                        .tenantId(tenant.getId())
                        .loanReference("BRW-PG-" + uniqueSuffix)
                        .direction(com.kuter.lending.LoanDirection.BORROW)
                        .status(com.kuter.lending.LoanStatus.ACTIVE)
                        .counterpartyName("External PG Counterparty")
                        .productId(prod.getId())
                        .productSku(prod.getSku())
                        .productName(prod.getName())
                        .unitId(unit.getId())
                        .unitCode(unit.getCode())
                        .conversionFactor(java.math.BigDecimal.ONE)
                        .initialQuantity(new java.math.BigDecimal("20.0000"))
                        .initialBaseQuantity(new java.math.BigDecimal("20.0000"))
                        .returnedQuantity(java.math.BigDecimal.ZERO)
                        .returnedBaseQuantity(java.math.BigDecimal.ZERO)
                        .outstandingBaseQuantity(new java.math.BigDecimal("20.0000"))
                        .unitCostSnapshot(new java.math.BigDecimal("10.0000"))
                        .transactionDate(java.time.Instant.now())
                        .build()
        );

        // Execute searchLoans with null status and null search parameter against real PostgreSQL!
        org.springframework.data.domain.Page<com.kuter.lending.StockLoan> page =
                stockLoanRepository.searchLoans(tenant.getId(), com.kuter.lending.LoanDirection.BORROW, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "transactionDate"
                        )));

        assertThat(page.getTotalElements()).isEqualTo(1L);
        assertThat(page.getContent().get(0).getLoanReference()).isEqualTo("BRW-PG-" + uniqueSuffix);
    }
}
