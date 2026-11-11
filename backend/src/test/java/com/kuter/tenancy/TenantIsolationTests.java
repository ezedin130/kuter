package com.kuter.tenancy;

import com.kuter.catalog.Product;
import com.kuter.catalog.ProductRepository;
import com.kuter.catalog.ProductStatus;
import com.kuter.catalog.UnitDefinition;
import com.kuter.catalog.UnitDefinitionRepository;
import com.kuter.common.Constants;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private UnitDefinitionRepository unitDefinitionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private com.kuter.catalog.ProductUnitConversionRepository conversionRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Tenant tenantAlpha;
    private Tenant tenantBeta;
    private User userAlpha;
    private TenantMembership membershipAlpha;
    private User platformAdmin;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM sale_items");
        jdbcTemplate.execute("DELETE FROM sales");
        jdbcTemplate.execute("DELETE FROM stock_movements");
        jdbcTemplate.execute("DELETE FROM stock_balances");
        jdbcTemplate.execute("DELETE FROM stock_adjustment_items");
        jdbcTemplate.execute("DELETE FROM stock_adjustments");
        jdbcTemplate.execute("DELETE FROM stock_receiving_items");
        jdbcTemplate.execute("DELETE FROM stock_receivings");
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        unitDefinitionRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Create two separate tenants
        tenantAlpha = tenantRepository.save(Tenant.builder()
                .code("alpha-hardware")
                .name("Alpha Hardware Supplies")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        tenantBeta = tenantRepository.save(Tenant.builder()
                .code("beta-timber")
                .name("Beta Timber & Roofing")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        // 2. Create User Alpha - member ONLY of Tenant Alpha
        userAlpha = userRepository.save(User.builder()
                .email("owner@alpha.com")
                .passwordHash("hashed_secret")
                .fullName("Alpha Shop Owner")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(false)
                .build());

        membershipAlpha = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantAlpha.getId())
                .userId(userAlpha.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        // 3. Create Platform Admin - no tenant memberships by default
        platformAdmin = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .passwordHash("hashed_admin_secret")
                .fullName("Platform Super Admin")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(true)
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Cross-tenant spoofing rejected: User in Tenant Alpha cannot supply Tenant Beta ID")
    void crossTenantSpoofingReturnsForbidden() throws Exception {
        // Authenticate as userAlpha
        KuterUserPrincipal principal = new KuterUserPrincipal(userAlpha, List.of(membershipAlpha));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // Attempt to access with X-Tenant-ID pointing to Tenant Beta
        mockMvc.perform(get("/api/v1/health")
                        .with(authentication(auth))
                        .header(Constants.TENANT_ID_HEADER, tenantBeta.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.error", is("Forbidden")))
                .andExpect(jsonPath("$.message", containsString("Access denied: you do not hold an active membership")));

        // Verify ThreadLocal is clean after request
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Platform admin without membership cannot access tenant context without delegation")
    void platformAdminCannotSpoofTenantContext() throws Exception {
        KuterUserPrincipal principal = new KuterUserPrincipal(platformAdmin, Collections.emptyList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        mockMvc.perform(get("/api/v1/health")
                        .with(authentication(auth))
                        .header(Constants.TENANT_ID_HEADER, tenantAlpha.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.message", containsString("Access denied: you do not hold an active membership")));
    }

    @Test
    @DisplayName("Legitimate tenant member supplying their tenant ID successfully populates TenantContext")
    void legitimateTenantContextAllowed() throws Exception {
        KuterUserPrincipal principal = new KuterUserPrincipal(userAlpha, List.of(membershipAlpha));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        mockMvc.perform(get("/api/v1/health")
                        .with(authentication(auth))
                        .header(Constants.TENANT_ID_HEADER, tenantAlpha.getId().toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify ThreadLocal cleared after request completion
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    @DisplayName("Tenant-scoped uniqueness: same SKU can exist in different tenants, but duplicates fail within the same tenant")
    void tenantScopedUniqueness() {
        // Base unit for tenant Alpha
        UnitDefinition bagAlpha = unitDefinitionRepository.save(UnitDefinition.builder()
                .tenantId(tenantAlpha.getId())
                .code("BAG")
                .name("50kg Bag")
                .build());

        // Base unit for tenant Beta
        UnitDefinition bagBeta = unitDefinitionRepository.save(UnitDefinition.builder()
                .tenantId(tenantBeta.getId())
                .code("BAG")
                .name("50kg Bag")
                .build());

        // 1. Create Product with SKU "CEM-425" in Tenant Alpha
        Product prodAlpha = productRepository.save(Product.builder()
                .tenantId(tenantAlpha.getId())
                .sku("CEM-425")
                .name("Portland Cement 42.5R (Alpha)")
                .baseUnitId(bagAlpha.getId())
                .costPrice(new BigDecimal("8.5000"))
                .sellingPrice(new BigDecimal("10.0000"))
                .reorderThreshold(new BigDecimal("50.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // 2. Create Product with EXACT SAME SKU "CEM-425" in Tenant Beta - MUST SUCCEED
        Product prodBeta = productRepository.save(Product.builder()
                .tenantId(tenantBeta.getId())
                .sku("CEM-425")
                .name("Portland Cement 42.5R (Beta)")
                .baseUnitId(bagBeta.getId())
                .costPrice(new BigDecimal("9.0000"))
                .sellingPrice(new BigDecimal("11.5000"))
                .reorderThreshold(new BigDecimal("100.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        assertThat(prodAlpha.getId()).isNotNull();
        assertThat(prodBeta.getId()).isNotNull();
        assertThat(prodAlpha.getId()).isNotEqualTo(prodBeta.getId());

        // 3. Attempt to insert duplicate SKU "CEM-425" within Tenant Alpha - MUST FAIL
        Product duplicateAlpha = Product.builder()
                .tenantId(tenantAlpha.getId())
                .sku("CEM-425")
                .name("Duplicate Portland Cement")
                .baseUnitId(bagAlpha.getId())
                .costPrice(new BigDecimal("8.0000"))
                .sellingPrice(new BigDecimal("10.0000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .status(ProductStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> productRepository.saveAndFlush(duplicateAlpha))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Repository query isolation: querying entity across tenant boundary returns empty")
    void repositoryQueryIsolation() {
        UnitDefinition bagAlpha = unitDefinitionRepository.save(UnitDefinition.builder()
                .tenantId(tenantAlpha.getId())
                .code("BAG-A")
                .name("Bag A")
                .build());

        Product prodAlpha = productRepository.save(Product.builder()
                .tenantId(tenantAlpha.getId())
                .sku("BAR-12MM")
                .name("Rebar 12mm High Yield")
                .baseUnitId(bagAlpha.getId())
                .costPrice(new BigDecimal("5.0000"))
                .sellingPrice(new BigDecimal("7.0000"))
                .reorderThreshold(new BigDecimal("20.0000"))
                .status(ProductStatus.ACTIVE)
                .build());

        // Correct tenant ID retrieves product
        Optional<Product> foundInAlpha = productRepository.findByTenantIdAndId(tenantAlpha.getId(), prodAlpha.getId());
        assertThat(foundInAlpha).isPresent();
        assertThat(foundInAlpha.get().getSku()).isEqualTo("BAR-12MM");

        // Cross-tenant query with tenant Beta ID returns EMPTY
        Optional<Product> foundInBeta = productRepository.findByTenantIdAndId(tenantBeta.getId(), prodAlpha.getId());
        assertThat(foundInBeta).isEmpty();

        // SKU query scoped by tenant
        assertThat(productRepository.findByTenantIdAndSku(tenantAlpha.getId(), "BAR-12MM")).isPresent();
        assertThat(productRepository.findByTenantIdAndSku(tenantBeta.getId(), "BAR-12MM")).isEmpty();
    }
}
