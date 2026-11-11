package com.kuter.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.audit.AuditLog;
import com.kuter.audit.AuditLogRepository;
import com.kuter.config.JwtService;
import com.kuter.identity.dto.LoginRequest;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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

    private User activeUser;
    private User suspendedUser;
    private Tenant testShop;

    @BeforeEach
    void setUp() {
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitDefinitionRepository.deleteAll();
        requestRepository.deleteAll();
        subscriptionRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        testShop = tenantRepository.save(Tenant.builder()
                .code("alpha-hardware")
                .name("Alpha Hardware Supplies")
                .currency("USD")
                .status(TenantStatus.ACTIVE)
                .build());

        activeUser = userRepository.save(User.builder()
                .email("owner@alphahardware.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .fullName("Alex Merchant")
                .phoneNumber("+1 555 1234")
                .isPlatformAdmin(false)
                .status(UserStatus.ACTIVE)
                .build());

        suspendedUser = userRepository.save(User.builder()
                .email("locked@alphahardware.com")
                .passwordHash(passwordEncoder.encode("SecretPass123!"))
                .fullName("Suspended Staff")
                .isPlatformAdmin(false)
                .status(UserStatus.SUSPENDED)
                .build());

        membershipRepository.save(TenantMembership.builder()
                .tenantId(testShop.getId())
                .userId(activeUser.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Valid login returns JWT access token, user profile, and active memberships")
    void validLoginReturnsTokenAndMemberships() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("owner@alphahardware.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is("owner@alphahardware.com")))
                .andExpect(jsonPath("$.data.user.fullName", is("Alex Merchant")))
                .andExpect(jsonPath("$.data.user.isPlatformAdmin", is(false)))
                .andExpect(jsonPath("$.data.memberships[0].tenantCode", is("alpha-hardware")))
                .andExpect(jsonPath("$.data.memberships[0].role", is("OWNER")));

        // Verify audit event recorded
        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "USER_LOGIN_SUCCESS".equals(l.getAction()) &&
                "owner@alphahardware.com".equals(l.getActorEmail()));
    }

    @Test
    @DisplayName("Invalid credentials returns generic error message without account enumeration")
    void invalidCredentialsReturnsGenericErrorMessage() throws Exception {
        LoginRequest badPassword = LoginRequest.builder()
                .email("owner@alphahardware.com")
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        LoginRequest unknownEmail = LoginRequest.builder()
                .email("doesnotexist@example.com")
                .password("AnyPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("Suspended user is blocked from logging in with 403 Forbidden")
    void suspendedUserBlockedFromLogin() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("locked@alphahardware.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("User account is suspended")));
    }

    @Test
    @DisplayName("Authenticated user can retrieve their profile via /api/v1/users/me and /api/v1/auth/me")
    void currentUserProfileReturnsDetailsAndActiveTenant() throws Exception {
        KuterUserPrincipal principal = new KuterUserPrincipal(activeUser, membershipRepository.findAllByUserId(activeUser.getId()));
        String token = jwtService.generateToken(principal);

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Tenant-ID", testShop.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("owner@alphahardware.com")))
                .andExpect(jsonPath("$.data.activeTenantId", is(testShop.getId().toString())))
                .andExpect(jsonPath("$.data.memberships[0].role", is("OWNER")));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email", is("owner@alphahardware.com")));
    }

    @Test
    @DisplayName("Logout endpoint records audit log event")
    void logoutCreatesAuditLog() throws Exception {
        KuterUserPrincipal principal = new KuterUserPrincipal(activeUser, membershipRepository.findAllByUserId(activeUser.getId()));
        String token = jwtService.generateToken(principal);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Logout successful")));

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).anyMatch(l -> "USER_LOGOUT".equals(l.getAction()) &&
                "owner@alphahardware.com".equals(l.getActorEmail()));
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoints is rejected with 401 Unauthorized")
    void unauthenticatedRequestsToProtectedRoutesRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));

        mockMvc.perform(get("/api/v1/tenants/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Password hashes are never returned in user API responses")
    void passwordHashNeverExposedInResponses() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("owner@alphahardware.com")
                .password("SecretPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    @DisplayName("Staff created with initial password can login and change password later")
    void staffCreatedWithInitialPasswordCanLoginAndChangePasswordLater() throws Exception {
        // 1. Owner logs in
        KuterUserPrincipal ownerPrincipal = new KuterUserPrincipal(activeUser, List.of(
                TenantMembership.builder()
                        .tenantId(testShop.getId())
                        .userId(activeUser.getId())
                        .role(TenantRole.OWNER)
                        .status(MembershipStatus.ACTIVE)
                        .permissions(new java.util.HashSet<>(java.util.List.of(ShopPermission.USERS_MANAGE)))
                        .build()
        ));
        String ownerToken = jwtService.generateToken(ownerPrincipal);

        // 2. Owner adds staff member with initial password
        com.kuter.identity.dto.AddMemberDto addMemberDto = com.kuter.identity.dto.AddMemberDto.builder()
                .email("cashier@alphahardware.com")
                .fullName("Jane Cashier")
                .role(TenantRole.CASHIER)
                .password("InitialStaffPass123!")
                .build();

        mockMvc.perform(post("/api/v1/tenants/me/members")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .header("X-Tenant-ID", testShop.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addMemberDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email", is("cashier@alphahardware.com")));

        // 3. Newly created staff member logs in with the initial password defined by the owner
        LoginRequest initialLogin = LoginRequest.builder()
                .email("cashier@alphahardware.com")
                .password("InitialStaffPass123!")
                .build();

        String cashierToken = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("cashier@alphahardware.com")))
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(cashierToken).path("data").path("accessToken").asText();

        // 4. Staff member changes their password
        com.kuter.identity.dto.ChangePasswordRequest changeReq = com.kuter.identity.dto.ChangePasswordRequest.builder()
                .currentPassword("InitialStaffPass123!")
                .newPassword("NewCashierSecret987!")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        // 5. Old password no longer works
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initialLogin)))
                .andExpect(status().isUnauthorized());

        // 6. New password works
        LoginRequest newLogin = LoginRequest.builder()
                .email("cashier@alphahardware.com")
                .password("NewCashierSecret987!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Changing password with invalid current password fails with 400 Bad Request")
    void changePasswordFailsWithWrongCurrentPassword() throws Exception {
        KuterUserPrincipal principal = new KuterUserPrincipal(activeUser, List.of());
        String token = jwtService.generateToken(principal);

        com.kuter.identity.dto.ChangePasswordRequest wrongCurrent = com.kuter.identity.dto.ChangePasswordRequest.builder()
                .currentPassword("WrongOldPass123!")
                .newPassword("SomeBrandNewPass456!")
                .build();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongCurrent)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Current password is incorrect")));
    }
}
