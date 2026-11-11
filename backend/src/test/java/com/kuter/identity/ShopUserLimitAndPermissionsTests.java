package com.kuter.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.common.Constants;
import com.kuter.config.JwtService;
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.TransferOwnershipDto;
import com.kuter.identity.dto.UpdateMemberDto;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
class ShopUserLimitAndPermissionsTests {

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
    private JdbcTemplate jdbcTemplate;

    private Tenant tenant;
    private User owner;
    private String ownerToken;

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
                .code("kuter-buildmart")
                .name("Kuter BuildMart Ltd")
                .currency("USD")
                .timezone("UTC")
                .status(TenantStatus.ACTIVE)
                .build());

        owner = userRepository.save(User.builder()
                .email("owner@buildmart.example")
                .fullName("Shop Owner")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership ownerMembership = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(owner.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(ShopPermission.values()))
                .build());

        ownerToken = jwtService.generateToken(new KuterUserPrincipal(owner, List.of(ownerMembership)));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM tenant_membership_permissions");
        jdbcTemplate.execute("DELETE FROM tenant_memberships");
    }

    @Test
    @DisplayName("Enforces strict 4-user limit: 1 owner + 3 staff allowed; 5th account rejected with 409 Conflict")
    void enforcesFourUserLimit() throws Exception {
        // Current: 1 active user (Owner)
        // Add User 2 (Manager)
        AddMemberDto user2 = AddMemberDto.builder()
                .email("manager@buildmart.example")
                .fullName("Bob Manager")
                .role(TenantRole.MANAGER)
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("manager@buildmart.example")))
                .andExpect(jsonPath("$.data.role", is("MANAGER")));

        // Add User 3 (Storekeeper)
        AddMemberDto user3 = AddMemberDto.builder()
                .email("store@buildmart.example")
                .fullName("Charlie Storekeeper")
                .role(TenantRole.STOREKEEPER)
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user3)))
                .andExpect(status().isCreated());

        // Add User 4 (Cashier)
        AddMemberDto user4 = AddMemberDto.builder()
                .email("cashier@buildmart.example")
                .fullName("David Cashier")
                .role(TenantRole.CASHIER)
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user4)))
                .andExpect(status().isCreated());

        // Now exactly 4 active accounts exist.
        // Adding User 5 must be rejected with 409 Conflict
        AddMemberDto user5 = AddMemberDto.builder()
                .email("extra@buildmart.example")
                .fullName("Eve Extra")
                .role(TenantRole.CASHIER)
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user5)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Shop user limit reached")));
    }

    @Test
    @DisplayName("Deactivated users do not count towards 4-user limit; reactivation checks seat capacity")
    void deactivatedUsersAndReactivationCapacity() throws Exception {
        // Add 3 staff members to reach capacity (4 total)
        for (int i = 1; i <= 3; i++) {
            AddMemberDto dto = AddMemberDto.builder()
                    .email("staff" + i + "@buildmart.example")
                    .fullName("Staff " + i)
                    .role(TenantRole.CASHIER)
                    .build();
            mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        // Get members list to find staff1
        List<TenantMembership> memberships = membershipRepository.findAllByTenantId(tenant.getId());
        TenantMembership staff1 = memberships.stream()
                .filter(m -> !m.getUserId().equals(owner.getId()))
                .findFirst().orElseThrow();

        // Deactivate staff1 (PATCH status -> SUSPENDED)
        mockMvc.perform(patch(Constants.API_BASE_PATH + "/tenants/me/members/" + staff1.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("SUSPENDED")));

        // Now active count is 3. We can add a replacement user (staff4)
        AddMemberDto replacement = AddMemberDto.builder()
                .email("replacement@buildmart.example")
                .fullName("Replacement Staff")
                .role(TenantRole.CASHIER)
                .build();
        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                            .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(replacement)))
                .andExpect(status().isCreated());

        // Now active count is 4 again. Attempting to reactivate staff1 must fail with 409 Conflict
        mockMvc.perform(patch(Constants.API_BASE_PATH + "/tenants/me/members/" + staff1.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .param("status", "ACTIVE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Shop user limit reached")));
    }

    @Test
    @DisplayName("Non-delegable permissions PRICES_MANAGE and USERS_MANAGE cannot be assigned to staff")
    void nonDelegablePermissionsRejectedForStaff() throws Exception {
        AddMemberDto attemptWithPricesManage = AddMemberDto.builder()
                .email("badstaff@buildmart.example")
                .fullName("Bad Staff")
                .role(TenantRole.MANAGER)
                .permissions(Set.of(ShopPermission.PRODUCTS_VIEW, ShopPermission.PRICES_MANAGE))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptWithPricesManage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("PRICES_MANAGE")));

        AddMemberDto attemptWithUsersManage = AddMemberDto.builder()
                .email("badstaff2@buildmart.example")
                .fullName("Bad Staff 2")
                .role(TenantRole.STOREKEEPER)
                .permissions(Set.of(ShopPermission.INVENTORY_VIEW, ShopPermission.USERS_MANAGE))
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attemptWithUsersManage)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("USERS_MANAGE")));
    }

    @Test
    @DisplayName("Non-owner staff cannot manage users or permissions (403 Forbidden)")
    void nonOwnerDeniedUserManagement() throws Exception {
        // Create a cashier
        User cashier = userRepository.save(User.builder()
                .email("cashier.test@buildmart.example")
                .fullName("Cashier Test")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership cashierMem = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(cashier.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(ShopPermission.SALES_CREATE, ShopPermission.SALES_VIEW))
                .build());

        String cashierToken = jwtService.generateToken(new KuterUserPrincipal(cashier, List.of(cashierMem)));

        // Cashier attempts to add member -> 403 Forbidden
        AddMemberDto newMember = AddMemberDto.builder()
                .email("substaff@buildmart.example")
                .fullName("Sub Staff")
                .role(TenantRole.CASHIER)
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cashierToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMember)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Explicit ownership transfer demotes previous owner to MANAGER and promotes target to OWNER")
    void ownershipTransferWorkflow() throws Exception {
        // Add a manager
        User manager = userRepository.save(User.builder()
                .email("promoted.manager@buildmart.example")
                .fullName("Promoted Manager")
                .passwordHash(passwordEncoder.encode("Pass123!"))
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        TenantMembership managerMem = membershipRepository.save(TenantMembership.builder()
                .tenantId(tenant.getId())
                .userId(manager.getId())
                .role(TenantRole.MANAGER)
                .status(MembershipStatus.ACTIVE)
                .permissions(Set.of(ShopPermission.PRODUCTS_VIEW, ShopPermission.PRODUCTS_MANAGE))
                .build());

        TransferOwnershipDto transferDto = TransferOwnershipDto.builder()
                .newOwnerUserId(manager.getId())
                .currentOwnerPassword("Pass123!")
                .build();

        mockMvc.perform(post(Constants.API_BASE_PATH + "/tenants/me/transfer-ownership")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header(Constants.TENANT_ID_HEADER, tenant.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("OWNER")))
                .andExpect(jsonPath("$.data.userId", is(manager.getId().toString())));

        // Verify previous owner is now MANAGER
        TenantMembership oldOwnerMem = membershipRepository.findByTenantIdAndUserId(tenant.getId(), owner.getId()).orElseThrow();
        assertThat(oldOwnerMem.getRole()).isEqualTo(TenantRole.MANAGER);
        assertThat(oldOwnerMem.getPermissions()).doesNotContain(ShopPermission.USERS_MANAGE);

        // Verify new owner is OWNER with full permissions
        TenantMembership newOwnerMem = membershipRepository.findById(managerMem.getId()).orElseThrow();
        assertThat(newOwnerMem.getRole()).isEqualTo(TenantRole.OWNER);
        assertThat(newOwnerMem.getPermissions()).contains(ShopPermission.USERS_MANAGE, ShopPermission.PRICES_MANAGE);
    }
}
