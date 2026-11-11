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
    private com.kuter.audit.AuditLogRepository auditLogRepository;

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
    @DisplayName("Verify V7 migration elements (audit log outcome and indexes) on PostgreSQL")
    void verifyV7MigrationAuditOutcomeAndIndexesOnPostgres() {
        com.kuter.audit.AuditLog log = com.kuter.audit.AuditLog.builder()
                .actorEmail("system@kuterplatform.com")
                .action("PG_TEST_ACTION")
                .entityType("SYSTEM")
                .entityId("SYSTEM-01")
                .outcome("SUCCESS")
                .details("{\"test\":\"postgres-v7\"}")
                .ipAddress("127.0.0.1")
                .build();

        com.kuter.audit.AuditLog saved = auditLogRepository.save(log);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOutcome()).isEqualTo("SUCCESS");

        Optional<com.kuter.audit.AuditLog> fetched = auditLogRepository.findById(saved.getId());
        assertThat(fetched).isPresent();
        assertThat(fetched.get().getAction()).isEqualTo("PG_TEST_ACTION");
        assertThat(fetched.get().getOutcome()).isEqualTo("SUCCESS");
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
}
