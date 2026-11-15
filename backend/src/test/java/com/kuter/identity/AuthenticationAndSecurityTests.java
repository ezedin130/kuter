package com.kuter.identity;

import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthenticationAndSecurityTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.kuter.catalog.ProductRepository productRepository;

    @Autowired
    private com.kuter.catalog.UnitDefinitionRepository unitRepository;

    @Autowired
    private com.kuter.catalog.CategoryRepository categoryRepository;

    @Autowired
    private com.kuter.catalog.ProductUnitConversionRepository conversionRepository;

    private Tenant shopA;
    private Tenant shopB;

    @BeforeEach
    void setUp() {
        conversionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        unitRepository.deleteAll();
        membershipRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        shopA = tenantRepository.save(Tenant.builder()
                .code("shop-a")
                .name("Shop A Materials")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());

        shopB = tenantRepository.save(Tenant.builder()
                .code("shop-b")
                .name("Shop B Hardware")
                .status(TenantStatus.ACTIVE)
                .currency("USD")
                .timezone("UTC")
                .build());
    }

    @Test
    @DisplayName("Password hashing: raw password is encrypted with BCrypt and verifies successfully")
    void passwordHashingVerification() {
        String rawPassword = "SecureBuildingPass2026!";
        String encoded = passwordEncoder.encode(rawPassword);

        assertThat(encoded).isNotEqualTo(rawPassword);
        assertThat(encoded).startsWith("$2a$");
        assertThat(passwordEncoder.matches(rawPassword, encoded)).isTrue();
        assertThat(passwordEncoder.matches("WrongPassword", encoded)).isFalse();
    }

    @Test
    @DisplayName("UserDetailsService loads active user with multi-tenant memberships and distinct roles")
    void userDetailsServiceLoadsMultiTenantMemberships() {
        String rawPassword = "ShopOwnerSecret99#";
        User user = userRepository.save(User.builder()
                .email("owner@multishop.com")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName("Sarah Multi-Owner")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(false)
                .build());

        // Membership 1: Shop A -> OWNER
        membershipRepository.save(TenantMembership.builder()
                .tenantId(shopA.getId())
                .userId(user.getId())
                .role(TenantRole.OWNER)
                .status(MembershipStatus.ACTIVE)
                .build());

        // Membership 2: Shop B -> CASHIER
        membershipRepository.save(TenantMembership.builder()
                .tenantId(shopB.getId())
                .userId(user.getId())
                .role(TenantRole.CASHIER)
                .status(MembershipStatus.ACTIVE)
                .build());

        // Load user via CustomUserDetailsService
        UserDetails details = userDetailsService.loadUserByUsername("owner@multishop.com");
        assertThat(details).isInstanceOf(KuterUserPrincipal.class);

        KuterUserPrincipal principal = (KuterUserPrincipal) details;
        assertThat(principal.getUsername()).isEqualTo("owner@multishop.com");
        assertThat(principal.isPlatformAdmin()).isFalse();
        assertThat(principal.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, principal.getPassword())).isTrue();

        // Verify multi-tenant memberships
        assertThat(principal.getAuthorizedTenantIds()).containsExactlyInAnyOrder(shopA.getId(), shopB.getId());
        assertThat(principal.getRoleIn(shopA.getId())).contains(TenantRole.OWNER);
        assertThat(principal.getRoleIn(shopB.getId())).contains(TenantRole.CASHIER);

        // Verify authorities contain both roles
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .contains("ROLE_TENANT_OWNER", "ROLE_TENANT_CASHIER");
    }

    @Test
    @DisplayName("UserDetailsService throws UsernameNotFoundException for non-existent email")
    void userNotFoundThrowsException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nonexistent@kuter.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email");
    }

    @Test
    @DisplayName("Platform admin user has ROLE_PLATFORM_ADMIN authority")
    void platformAdminAuthority() {
        User admin = userRepository.save(User.builder()
                .email("root@kuter.io")
                .passwordHash(passwordEncoder.encode("RootAdmin123!"))
                .fullName("System Administrator")
                .status(UserStatus.ACTIVE)
                .isPlatformAdmin(true)
                .build());

        UserDetails details = userDetailsService.loadUserByUsername("root@kuter.io");
        KuterUserPrincipal principal = (KuterUserPrincipal) details;

        assertThat(principal.isPlatformAdmin()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .contains("ROLE_PLATFORM_ADMIN");
    }
}
