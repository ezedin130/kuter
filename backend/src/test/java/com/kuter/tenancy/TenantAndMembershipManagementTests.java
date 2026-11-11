package com.kuter.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.config.JwtService;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.UpdateMemberDto;
import com.kuter.tenancy.dto.CreateTenantDto;
import com.kuter.tenancy.dto.UpdateTenantDto;
import com.kuter.tenancy.dto.UpdateTenantStatusDto;
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

import java.util.Collections;
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
class TenantAndMembershipManagementTests {

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
    private com.kuter.catalog.ProductUnitConversionRepository conversionRepository;

    @Autowired
    private com.kuter.catalog.ProductRepository productRepository;

    @Autowired
    private com.kuter.catalog.CategoryRepository categoryRepository;

    @Autowired
    private com.kuter.catalog.UnitDefinitionRepository unitDefinitionRepository;

    @Autowired
    private com.kuter.subscriptions.TenantSubscriptionRepository subscriptionRepository;

    @Autowired
    private com.kuter.onboarding.TenantOnboardingRequestRepository requestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Tenant tenantA;
    private Tenant tenantB;
    private User adminUser;
    private User ownerA;
    private User ownerB;
    private User cashierA;

    private String adminToken;
    private String ownerAToken;
    private String ownerBToken;
    private String cashierAToken;

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
        categoryRepository.deleteAll();
        unitDefinitionRepository.deleteAll();
        requestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // 1. Create Platform Admin
        adminUser = userRepository.save(User.builder()
                .email("admin@kuterplatform.com")
                .fullName("System Administrator")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(true)
                .status(UserStatus.ACTIVE)
                .build());
        adminToken = jwtService.generateToken(new KuterUserPrincipal(adminUser, Collections.emptyList()));

        // 2. Create Tenant A & Owner A
        tenantA = tenantRepository.save(Tenant.builder()
                .code("yard-alpha")
                .name("Alpha Materials Yard")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerA = userRepository.save(User.builder()
                .email("owner.a@alpha.example")
                .fullName("Alice Owner")
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

        // 3. Create Tenant B & Owner B
        tenantB = tenantRepository.save(Tenant.builder()
                .code("yard-beta")
                .name("Beta Hardware Depot")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        ownerB = userRepository.save(User.builder()
                .email("owner.b@beta.example")
                .fullName("Bob Owner")
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

        // 4. Create Cashier in Tenant A
        cashierA = userRepository.save(User.builder()
                .email("cashier.a@alpha.example")
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
    }

    @Test
    @DisplayName("Platform administrator can create a new tenant directly")
    void platformAdminCanCreateTenantDirectly() throws Exception {
        CreateTenantDto dto = CreateTenantDto.builder()
                .name("Delta Concrete Depot")
                .code("delta-concrete")
                .currency("USD")
                .timezone("America/New_York")
                .ownerEmail("dan@deltaconcrete.example")
                .ownerFullName("Dan Builder")
                .ownerPhone("+1 555 8899")
                .build();

        mockMvc.perform(post("/api/v1/admin/tenants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.code", is("delta-concrete")))
                .andExpect(jsonPath("$.data.name", is("Delta Concrete Depot")));

        assertThat(tenantRepository.existsByCode("delta-concrete")).isTrue();
        assertThat(userRepository.findByEmail("dan@deltaconcrete.example")).isPresent();
    }

    @Test
    @DisplayName("Suspending a tenant prevents all users from accessing the shop context")
    void platformAdminCanSuspendTenantAndSuspensionBlocksContext() throws Exception {
        UpdateTenantStatusDto suspendDto = UpdateTenantStatusDto.builder()
                .status(TenantStatus.SUSPENDED)
                .reason("Account payment past due")
                .build();

        mockMvc.perform(patch("/api/v1/admin/tenants/" + tenantA.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suspendDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")));

        // Attempt to access tenantA with ownerA credentials
        mockMvc.perform(get("/api/v1/tenants/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("Tenant shop is suspended")));
    }

    @Test
    @DisplayName("Tenant owner can view and update their own shop settings via /api/v1/tenants/me")
    void tenantOwnerCanViewAndUpdateOwnShopSettings() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code", is("yard-alpha")))
                .andExpect(jsonPath("$.data.name", is("Alpha Materials Yard")));

        UpdateTenantDto updateDto = UpdateTenantDto.builder()
                .name("Alpha Materials Superstore")
                .businessRegistrationNumber("TAX-998822")
                .currency("USD")
                .timezone("UTC")
                .build();

        mockMvc.perform(put("/api/v1/tenants/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Alpha Materials Superstore")))
                .andExpect(jsonPath("$.data.businessRegistrationNumber", is("TAX-998822")));
    }

    @Test
    @DisplayName("Tenant owner can list and add staff members to their shop")
    void tenantOwnerCanListAndAddMembers() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + tenantA.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2))); // Owner and Cashier

        AddMemberDto addMemberDto = AddMemberDto.builder()
                .email("storekeeper@alpha.example")
                .fullName("Sam Storekeeper")
                .role(TenantRole.STOREKEEPER)
                .phoneNumber("+1 555 4321")
                .build();

        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("storekeeper@alpha.example")))
                .andExpect(jsonPath("$.data.role", is("STOREKEEPER")));
    }

    @Test
    @DisplayName("Adding a duplicate user to the same tenant returns 409 Conflict")
    void duplicateMemberAdditionRejectedWithConflict() throws Exception {
        AddMemberDto addMemberDto = AddMemberDto.builder()
                .email("cashier.a@alpha.example") // Already a member of Tenant A
                .fullName("Charlie Cashier")
                .role(TenantRole.CASHIER)
                .build();

        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already a member")));
    }

    @Test
    @DisplayName("Staff cannot manage members; attempting to access member management returns 403 Forbidden")
    void staffCannotManageMembers() throws Exception {
        AddMemberDto addMemberDto = AddMemberDto.builder()
                .email("newstaff@alpha.example")
                .fullName("New Staff")
                .role(TenantRole.VIEWER)
                .build();

        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberDto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Last active owner cannot be demoted or removed")
    void lastActiveOwnerCannotBeDemotedOrRemoved() throws Exception {
        TenantMembership ownerMembership = membershipRepository.findByTenantIdAndUserId(tenantA.getId(), ownerA.getId()).orElseThrow();

        // 1. Demotion attempt via Admin
        UpdateMemberDto demoteDto = UpdateMemberDto.builder()
                .role(TenantRole.VIEWER)
                .build();

        mockMvc.perform(put("/api/v1/tenants/" + tenantA.getId() + "/members/" + ownerMembership.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot demote or deactivate the last active owner")));

        // 2. Removal attempt via Admin
        mockMvc.perform(delete("/api/v1/tenants/" + tenantA.getId() + "/members/" + ownerMembership.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot remove the last active owner")));
    }

    @Test
    @DisplayName("Tenant A owner cannot view or manage Tenant B members (cross-tenant isolation)")
    void crossTenantMemberAccessAndModificationRejectedWithForbidden() throws Exception {
        // Owner A tries to list Tenant B members
        mockMvc.perform(get("/api/v1/tenants/" + tenantB.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString()))
                .andExpect(status().isForbidden());

        // Owner A tries to add a member to Tenant B
        AddMemberDto dto = AddMemberDto.builder()
                .email("intruder@example.com")
                .fullName("Intruder")
                .role(TenantRole.CASHIER)
                .build();

        mockMvc.perform(post("/api/v1/tenants/" + tenantB.getId() + "/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerAToken)
                        .header("X-Tenant-ID", tenantA.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
