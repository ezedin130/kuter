package com.kuter.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogQueryAndSecurityTests {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    private Tenant tenantA;
    private Tenant tenantB;
    private String tenantAOwnerToken;
    private String tenantACashierToken;
    private String tenantBOwnerToken;
    private String platformAdminToken;
    private User ownerA;
    private User adminUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Tenant A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("shop-a")
                .name("Shop A Materials")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        // Tenant B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("shop-b")
                .name("Shop B Hardware")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        // Users
        ownerA = userRepository.save(User.builder()
                .email("owner.a@shopa.example")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Owner A")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        User cashierA = userRepository.save(User.builder()
                .email("cashier.a@shopa.example")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Cashier A")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        User ownerB = userRepository.save(User.builder()
                .email("owner.b@shopb.example")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .fullName("Owner B")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .fullName("Platform Administrator")
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());

        // Memberships
        TenantMembership memOwnerA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(ownerA.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memCashierA = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantA.getId())
                .userId(cashierA.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        TenantMembership memOwnerB = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantB.getId())
                .userId(ownerB.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        tenantAOwnerToken = jwtService.generateToken(new KuterUserPrincipal(ownerA, List.of(memOwnerA)));
        tenantACashierToken = jwtService.generateToken(new KuterUserPrincipal(cashierA, List.of(memCashierA)));
        tenantBOwnerToken = jwtService.generateToken(new KuterUserPrincipal(ownerB, List.of(memOwnerB)));
        platformAdminToken = jwtService.generateToken(new KuterUserPrincipal(adminUser, List.of()));

        // Create Seed Audit Logs
        // Log 1: Tenant A Product Created (SUCCESS)
        auditService.logEvent(
                tenantA.getId(),
                ownerA.getId(),
                ownerA.getEmail(),
                "PRODUCT_CREATED",
                "PRODUCT",
                UUID.randomUUID().toString(),
                "{\"name\":\"Portland Cement\"}",
                "127.0.0.1",
                "SUCCESS"
        );

        // Log 2: Tenant A Price Updated (SUCCESS)
        auditService.logEvent(
                tenantA.getId(),
                ownerA.getId(),
                ownerA.getEmail(),
                "PRODUCT_UPDATED",
                "PRODUCT",
                UUID.randomUUID().toString(),
                "{\"sellingPrice\":\"15.00\"}",
                "127.0.0.1",
                "SUCCESS"
        );

        // Log 3: Tenant B Category Created (SUCCESS)
        auditService.logEvent(
                tenantB.getId(),
                ownerB.getId(),
                ownerB.getEmail(),
                "CATEGORY_CREATED",
                "CATEGORY",
                UUID.randomUUID().toString(),
                "{\"name\":\"Timber\"}",
                "127.0.0.1",
                "SUCCESS"
        );

        // Log 4: Platform-wide Plan Created (SUCCESS, tenantId = null)
        auditService.logEvent(
                null,
                adminUser.getId(),
                adminUser.getEmail(),
                "PLAN_CREATED",
                "SUBSCRIPTION_PLAN",
                UUID.randomUUID().toString(),
                "{\"code\":\"enterprise\"}",
                "127.0.0.1",
                "SUCCESS"
        );
    }

    @Test
    @DisplayName("Shop OWNER can query tenant-scoped audit logs")
    void shouldAllowOwnerToQueryTenantAuditLogs() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAOwnerToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].tenantId", is(tenantA.getId().toString())))
                .andExpect(jsonPath("$.data.content[1].tenantId", is(tenantA.getId().toString())));
    }

    @Test
    @DisplayName("Cashier is forbidden from querying tenant audit logs")
    void shouldForbidCashierFromQueryingTenantAuditLogs() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantACashierToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant isolation: Tenant B Owner cannot view Tenant A audit logs")
    void shouldEnforceTenantIsolationOnAuditLogs() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantBOwnerToken)
                        .header("X-Tenant-ID", tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].tenantId", is(tenantB.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].action", is("CATEGORY_CREATED")));
    }

    @Test
    @DisplayName("Platform Admin can query /admin/audit-logs across all tenants and filter by tenantId and action")
    void shouldAllowPlatformAdminToQueryAndFilterLogs() throws Exception {
        // Query all platform logs
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(4)));

        // Filter by tenantId = Tenant A
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformAdminToken)
                        .param("tenantId", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Filter by action = "PLAN_CREATED"
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + platformAdminToken)
                        .param("action", "PLAN_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].entityType", is("SUBSCRIPTION_PLAN")));
    }

    @Test
    @DisplayName("Non-admin is forbidden from accessing /admin/audit-logs")
    void shouldForbidShopUsersFromAdminAuditEndpoint() throws Exception {
        mockMvc.perform(get(Constants.API_BASE_PATH + "/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tenantAOwnerToken))
                .andExpect(status().isForbidden());
    }
}
