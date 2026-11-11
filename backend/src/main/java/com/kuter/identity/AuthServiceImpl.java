package com.kuter.identity;

import com.kuter.audit.AuditService;
import com.kuter.common.BadRequestException;
import com.kuter.common.ForbiddenException;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.config.JwtService;
import com.kuter.identity.dto.AuthResponse;
import com.kuter.identity.dto.ChangePasswordRequest;
import com.kuter.identity.dto.LoginRequest;
import com.kuter.identity.dto.TenantMembershipDto;
import com.kuter.identity.dto.UserProfileResponse;
import com.kuter.identity.dto.UserSummaryDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantContext;
import com.kuter.tenancy.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of AuthService handling secure authentication, JWT issuance,
 * user profile retrieval, and security auditing.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           UserRepository userRepository,
                           TenantMembershipRepository membershipRepository,
                           TenantRepository tenantRepository,
                           AuditService auditService,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().trim().toLowerCase();
        String ipAddress = extractClientIp(httpRequest);

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            KuterUserPrincipal principal = (KuterUserPrincipal) auth.getPrincipal();

            if (principal.getStatus() != UserStatus.ACTIVE) {
                auditService.logEvent(null, principal.getUserId(), email, "USER_LOGIN_BLOCKED",
                        "USER", principal.getUserId().toString(), "Account is " + principal.getStatus(), ipAddress);
                throw new LockedException("User account is suspended or inactive");
            }

            // Update last login timestamp
            userRepository.findById(principal.getUserId()).ifPresent(user -> {
                user.setLastLoginAt(Instant.now());
                userRepository.save(user);
            });

            String token = jwtService.generateToken(principal);
            List<TenantMembershipDto> memberships = buildMembershipDtos(principal.getUserId());

            auditService.logEvent(null, principal.getUserId(), email, "USER_LOGIN_SUCCESS",
                    "USER", principal.getUserId().toString(), "Login successful", ipAddress);

            UserSummaryDto userSummary = UserSummaryDto.builder()
                    .id(principal.getUserId())
                    .email(principal.getEmail())
                    .fullName(principal.getFullName())
                    .phoneNumber(userRepository.findById(principal.getUserId()).map(User::getPhoneNumber).orElse(null))
                    .isPlatformAdmin(principal.isPlatformAdmin())
                    .status(principal.getStatus())
                    .build();

            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .expiresIn(jwtService.getExpirationMs())
                    .user(userSummary)
                    .memberships(memberships)
                    .build();

        } catch (BadCredentialsException ex) {
            auditService.logEvent(null, null, email, "USER_LOGIN_FAILURE",
                    "USER", "none", "Bad credentials provided", ipAddress);
            log.warn("Failed login attempt for email '{}' from IP '{}'", email, ipAddress);
            throw ex;
        } catch (DisabledException | LockedException ex) {
            throw ex;
        }
    }

    @Override
    public void logout(KuterUserPrincipal principal, HttpServletRequest httpRequest) {
        if (principal != null) {
            String ipAddress = extractClientIp(httpRequest);
            auditService.logEvent(null, principal.getUserId(), principal.getEmail(), "USER_LOGOUT",
                    "USER", principal.getUserId().toString(), "User logged out", ipAddress);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUser(KuterUserPrincipal principal) {
        if (principal == null) {
            return null;
        }

        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        List<TenantMembershipDto> memberships = buildMembershipDtos(user.getId());
        UUID activeTenantId = TenantContext.getTenantId().orElse(principal.getCurrentTenantId());

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isPlatformAdmin(user.isPlatformAdmin())
                .status(user.getStatus())
                .memberships(memberships)
                .activeTenantId(activeTenantId)
                .build();
    }

    private List<TenantMembershipDto> buildMembershipDtos(UUID userId) {
        List<TenantMembership> memberships = membershipRepository.findAllByUserId(userId);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> tenantIds = memberships.stream()
                .map(TenantMembership::getTenantId)
                .toList();

        Map<UUID, Tenant> tenantMap = tenantRepository.findAllById(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, t -> t));

        return memberships.stream().map(m -> {
            Tenant t = tenantMap.get(m.getTenantId());
            return TenantMembershipDto.builder()
                    .id(m.getId())
                    .tenantId(m.getTenantId())
                    .tenantCode(t != null ? t.getCode() : "unknown")
                    .tenantName(t != null ? t.getName() : "Unknown Shop")
                    .role(m.getRole())
                    .status(m.getStatus())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public void changePassword(KuterUserPrincipal principal, ChangePasswordRequest request, HttpServletRequest httpRequest) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required to change password.");
        }

        String ipAddress = extractClientIp(httpRequest);
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + principal.getUserId()));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditService.logEvent(null, user.getId(), user.getEmail(), "USER_PASSWORD_CHANGE_FAILED",
                    "USER", user.getId().toString(), "Incorrect current password", ipAddress);
            throw new BadRequestException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);

        auditService.logEvent(null, user.getId(), user.getEmail(), "USER_PASSWORD_CHANGED",
                "USER", user.getId().toString(), "Password changed successfully", ipAddress);
        log.info("Password changed successfully for user id={}, email='{}'", user.getId(), user.getEmail());
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
