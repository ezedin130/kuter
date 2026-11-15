package com.kuter.config;

import com.kuter.identity.TenantMembership;
import com.kuter.identity.TenantMembershipRepository;
import com.kuter.identity.TenantRole;
import com.kuter.identity.User;
import com.kuter.identity.UserRepository;
import com.kuter.identity.UserStatus;
import com.kuter.onboarding.OnboardingStatus;
import com.kuter.onboarding.TenantOnboardingRequest;
import com.kuter.onboarding.TenantOnboardingRequestRepository;
import com.kuter.subscriptions.SubscriptionPlan;
import com.kuter.subscriptions.SubscriptionPlanRepository;
import com.kuter.subscriptions.SubscriptionStatus;
import com.kuter.subscriptions.TenantSubscription;
import com.kuter.subscriptions.TenantSubscriptionRepository;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Automatically seeds platform administration credentials and baseline demo data
 * for development and demonstration environments. Disabled during automated test runs.
 */
@Component
@Profile("!test")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantOnboardingRequestRepository onboardingRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository userRepository,
                          TenantRepository tenantRepository,
                          TenantMembershipRepository membershipRepository,
                          SubscriptionPlanRepository planRepository,
                          TenantSubscriptionRepository subscriptionRepository,
                          TenantOnboardingRequestRepository onboardingRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.onboardingRepository = onboardingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPlatformAdmins();
        seedDemoTenantsAndWorkflows();
    }

    private void seedPlatformAdmins() {
        if (userRepository.findByEmail("admin@kuterplatform.com").isEmpty()) {
            log.info("Seeding Super Admin user: admin@kuterplatform.com");
            userRepository.save(User.builder()
                    .email("admin@kuterplatform.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .fullName("Platform Administrator")
                    .phoneNumber("+251911010001")
                    .isPlatformAdmin(true)
                    .status(UserStatus.ACTIVE)
                    .build());
        }

        if (userRepository.findByEmail("operator@kuterplatform.com").isEmpty()) {
            log.info("Seeding Platform Operator user: operator@kuterplatform.com");
            userRepository.save(User.builder()
                    .email("operator@kuterplatform.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .fullName("Platform Operator")
                    .phoneNumber("+251911010002")
                    .isPlatformAdmin(true)
                    .status(UserStatus.ACTIVE)
                    .build());
        }
    }

    private void seedDemoTenantsAndWorkflows() {
        // 1. Al-Amoudi Building Materials
        Tenant amoudiTenant = tenantRepository.findByCode("amoudi-materials").orElseGet(() -> {
            log.info("Seeding demo tenant: Al-Amoudi Building Materials");
            return tenantRepository.save(Tenant.builder()
                    .code("amoudi-materials")
                    .name("Al-Amoudi Building Materials")
                    .status(TenantStatus.ACTIVE)
                    .currency("ETB")
                    .timezone("UTC")
                    .build());
        });

        // 2. Kuter Express Hardware
        tenantRepository.findByCode("kuter-express").orElseGet(() -> {
            log.info("Seeding demo tenant: Kuter Express Hardware");
            return tenantRepository.save(Tenant.builder()
                    .code("kuter-express")
                    .name("Kuter Express Hardware")
                    .status(TenantStatus.ACTIVE)
                    .currency("ETB")
                    .timezone("UTC")
                    .build());
        });

        // 3. Red Sea Timber & Steel (Suspended)
        tenantRepository.findByCode("redsea-timber").orElseGet(() -> {
            log.info("Seeding suspended demo tenant: Red Sea Timber & Steel");
            return tenantRepository.save(Tenant.builder()
                    .code("redsea-timber")
                    .name("Red Sea Timber & Steel")
                    .status(TenantStatus.SUSPENDED)
                    .currency("ETB")
                    .timezone("UTC")
                    .build());
        });

        // Demo Owner User
        User amoudiOwner = userRepository.findByEmail("owner@amoudi.com").orElseGet(() -> {
            log.info("Seeding demo tenant owner: owner@amoudi.com");
            return userRepository.save(User.builder()
                    .email("owner@amoudi.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .fullName("Tariq Al-Amoudi")
                    .phoneNumber("+251922020001")
                    .isPlatformAdmin(false)
                    .status(UserStatus.ACTIVE)
                    .build());
        });

        // Owner Membership
        if (membershipRepository.findByTenantIdAndUserId(amoudiTenant.getId(), amoudiOwner.getId()).isEmpty()) {
            membershipRepository.save(TenantMembership.builder()
                    .tenantId(amoudiTenant.getId())
                    .userId(amoudiOwner.getId())
                    .role(TenantRole.OWNER)
                    .status(com.kuter.identity.MembershipStatus.ACTIVE)
                    .build());
        }

        // Subscriptions
        Optional<SubscriptionPlan> growthPlan = planRepository.findByCode("growth");
        if (growthPlan.isPresent() && subscriptionRepository.findByTenantIdAndIsCurrentTrue(amoudiTenant.getId()).isEmpty()) {
            log.info("Seeding active subscription for Al-Amoudi Building Materials");
            subscriptionRepository.save(TenantSubscription.builder()
                    .tenantId(amoudiTenant.getId())
                    .planId(growthPlan.get().getId())
                    .status(SubscriptionStatus.ACTIVE)
                    .currentPeriodStart(Instant.now())
                    .currentPeriodEnd(Instant.now().plus(365, ChronoUnit.DAYS))
                    .cancelAtPeriodEnd(false)
                    .isCurrent(true)
                    .version(0L)
                    .build());
        }

        // Onboarding Requests for Review Queue
        if (onboardingRepository.findByReferenceCode("REQ-2026-00001").isEmpty()) {
            log.info("Seeding pending onboarding request REQ-2026-00001");
            onboardingRepository.save(TenantOnboardingRequest.builder()
                    .referenceCode("REQ-2026-00001")
                    .shopName("Nile Valley Concrete & Sand")
                    .ownerFullName("Ahmed Hassan")
                    .ownerEmail("ahmed@nileconcrete.com")
                    .ownerPhone("+1 555-030-0001")
                    .requestedPlanCode("growth")
                    .preferredShopCode("nile-concrete")
                    .businessAddress("Port Road Yard 4, Industrial Zone")
                    .currency("ETB")
                    .status(OnboardingStatus.PENDING)
                    .applicantNotes("Wholesale distributor supplying structural aggregates, cement, and reinforcement steel.")
                    .build());
        }

        if (onboardingRepository.findByReferenceCode("REQ-2026-00002").isEmpty()) {
            log.info("Seeding pending onboarding request REQ-2026-00002");
            onboardingRepository.save(TenantOnboardingRequest.builder()
                    .referenceCode("REQ-2026-00002")
                    .shopName("Apex Masonry Supply")
                    .ownerFullName("Sarah Jenkins")
                    .ownerEmail("sarah@apexmasonry.com")
                    .ownerPhone("+1 555-030-0002")
                    .requestedPlanCode("starter")
                    .preferredShopCode("apex-masonry")
                    .businessAddress("108 Commercial Way, Building B")
                    .currency("ETB")
                    .status(OnboardingStatus.PENDING)
                    .applicantNotes("Single location brick, paver, and mortar retailer.")
                    .build());
        }
    }
}
