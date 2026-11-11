package com.kuter.identity;

import com.kuter.audit.AuditService;
import com.kuter.common.BadRequestException;
import com.kuter.common.ConflictException;
import com.kuter.common.ForbiddenException;
import com.kuter.common.ResourceNotFoundException;
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.TenantMemberDetailResponse;
import com.kuter.identity.dto.TransferOwnershipDto;
import com.kuter.identity.dto.UpdateMemberDto;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TenantMembershipService enforcing RBAC, self-demotion prevention,
 * and last active owner safeguards.
 */
@Service
public class TenantMembershipServiceImpl implements TenantMembershipService {

    private static final Logger log = LoggerFactory.getLogger(TenantMembershipServiceImpl.class);

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final com.kuter.subscriptions.EntitlementService entitlementService;

    public TenantMembershipServiceImpl(TenantRepository tenantRepository,
                                       TenantMembershipRepository membershipRepository,
                                       UserRepository userRepository,
                                       PasswordEncoder passwordEncoder,
                                       AuditService auditService,
                                       com.kuter.subscriptions.EntitlementService entitlementService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.entitlementService = entitlementService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantMemberDetailResponse> getTenantMembers(UUID tenantId, KuterUserPrincipal principal) {
        validateTenantExists(tenantId);
        validateCallerCanAccessTenant(tenantId, principal);

        List<TenantMembership> memberships = membershipRepository.findAllByTenantId(tenantId);
        if (memberships.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> userIds = memberships.stream().map(TenantMembership::getUserId).toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return memberships.stream().map(m -> {
            User u = userMap.get(m.getUserId());
            return TenantMemberDetailResponse.builder()
                    .id(m.getId())
                    .tenantId(m.getTenantId())
                    .userId(m.getUserId())
                    .email(u != null ? u.getEmail() : "unknown")
                    .fullName(u != null ? u.getFullName() : "Unknown User")
                    .phoneNumber(u != null ? u.getPhoneNumber() : null)
                    .role(m.getRole())
                    .status(m.getStatus())
                    .permissions(m.getPermissions() != null ? new HashSet<>(m.getPermissions()) : Collections.emptySet())
                    .createdAt(m.getCreatedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public TenantMemberDetailResponse addMember(UUID tenantId,
                                                AddMemberDto dto,
                                                KuterUserPrincipal principal,
                                                HttpServletRequest httpRequest) {
        validateCallerCanManageTenant(tenantId, principal);
        Tenant tenant = tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        if (tenant.getStatus() == TenantStatus.SUSPENDED) {
            throw new ForbiddenException("Cannot add members to a suspended tenant shop.");
        }

        validatePermissionsForRole(dto.getRole(), dto.getPermissions());
        entitlementService.checkUserLimit(tenantId);

        String email = dto.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (membershipRepository.existsByTenantIdAndUserId(tenantId, user.getId())) {
                throw new ConflictException("User with email '" + email + "' is already a member of this tenant shop.");
            }
        } else {
            String initialPassword = StringUtils.hasText(dto.getPassword())
                    ? dto.getPassword().trim()
                    : UUID.randomUUID().toString().replace("-", "");
            user = User.builder()
                    .email(email)
                    .fullName(dto.getFullName().trim())
                    .phoneNumber(dto.getPhoneNumber() != null ? dto.getPhoneNumber().trim() : null)
                    .passwordHash(passwordEncoder.encode(initialPassword))
                    .status(UserStatus.ACTIVE)
                    .isPlatformAdmin(false)
                    .build();
            user = userRepository.save(user);
        }

        Set<ShopPermission> permsToAssign = dto.getPermissions() != null
                ? new HashSet<>(dto.getPermissions())
                : getDefaultPermissionsForRole(dto.getRole());

        TenantMembership membership = TenantMembership.builder()
                .tenantId(tenantId)
                .userId(user.getId())
                .role(dto.getRole())
                .status(MembershipStatus.ACTIVE)
                .permissions(permsToAssign)
                .build();
        membership = membershipRepository.save(membership);

        String ipAddress = extractClientIp(httpRequest);
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";

        auditService.logEvent(tenantId, actorId, actorEmail, "MEMBERSHIP_CREATED",
                "TENANT_MEMBERSHIP", membership.getId().toString(),
                "Added user " + email + " with role " + dto.getRole(), ipAddress);

        return TenantMemberDetailResponse.builder()
                .id(membership.getId())
                .tenantId(tenantId)
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(membership.getRole())
                .status(membership.getStatus())
                .permissions(membership.getPermissions() != null ? new HashSet<>(membership.getPermissions()) : Collections.emptySet())
                .createdAt(membership.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public TenantMemberDetailResponse updateMember(UUID tenantId,
                                                   UUID memberId,
                                                   UpdateMemberDto dto,
                                                   KuterUserPrincipal principal,
                                                   HttpServletRequest httpRequest) {
        validateCallerCanManageTenant(tenantId, principal);
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        TenantMembership member = membershipRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + memberId));

        // Self-demotion / privilege escalation defense: user cannot modify their own role
        if (principal != null && member.getUserId().equals(principal.getUserId()) && dto.getRole() != null && dto.getRole() != member.getRole()) {
            throw new ForbiddenException("Cannot modify your own role to escalate or alter privileges.");
        }

        // Last active OWNER defense
        if (member.getRole() == TenantRole.OWNER && (dto.getRole() != null && dto.getRole() != TenantRole.OWNER ||
                (dto.getStatus() != null && dto.getStatus() != MembershipStatus.ACTIVE))) {
            long activeOwners = membershipRepository.countByTenantIdAndRoleAndStatus(tenantId, TenantRole.OWNER, MembershipStatus.ACTIVE);
            if (activeOwners <= 1) {
                throw new BadRequestException("Cannot demote or deactivate the last active owner of the tenant shop.");
            }
        }

        // Reactivation capacity check
        if (dto.getStatus() == MembershipStatus.ACTIVE && member.getStatus() != MembershipStatus.ACTIVE) {
            entitlementService.checkUserLimit(tenantId);
        }

        TenantRole oldRole = member.getRole();
        if (dto.getRole() != null) {
            member.setRole(dto.getRole());
        }
        if (dto.getStatus() != null) {
            member.setStatus(dto.getStatus());
        }

        if (dto.getPermissions() != null) {
            validatePermissionsForRole(member.getRole(), dto.getPermissions());
            member.setPermissions(new HashSet<>(dto.getPermissions()));
        } else if (dto.getRole() != null && dto.getRole() != oldRole) {
            member.setPermissions(getDefaultPermissionsForRole(dto.getRole()));
        }

        TenantMembership updated = membershipRepository.save(member);
        User user = userRepository.findById(member.getUserId()).orElse(null);

        String ipAddress = extractClientIp(httpRequest);
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";

        auditService.logEvent(tenantId, actorId, actorEmail, "MEMBERSHIP_UPDATED",
                "TENANT_MEMBERSHIP", member.getId().toString(),
                "Updated member " + (user != null ? user.getEmail() : member.getUserId()) + " role to " + member.getRole(), ipAddress);

        return TenantMemberDetailResponse.builder()
                .id(updated.getId())
                .tenantId(tenantId)
                .userId(member.getUserId())
                .email(user != null ? user.getEmail() : "unknown")
                .fullName(user != null ? user.getFullName() : "Unknown User")
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .role(updated.getRole())
                .status(updated.getStatus())
                .permissions(updated.getPermissions() != null ? new HashSet<>(updated.getPermissions()) : Collections.emptySet())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public TenantMemberDetailResponse updateMemberStatus(UUID tenantId,
                                                         UUID memberId,
                                                         MembershipStatus status,
                                                         KuterUserPrincipal principal,
                                                         HttpServletRequest httpRequest) {
        validateCallerCanManageTenant(tenantId, principal);
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        TenantMembership member = membershipRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + memberId));

        if (status == null) {
            throw new BadRequestException("Status is required");
        }

        if (member.getRole() == TenantRole.OWNER && status != MembershipStatus.ACTIVE) {
            long activeOwners = membershipRepository.countByTenantIdAndRoleAndStatus(tenantId, TenantRole.OWNER, MembershipStatus.ACTIVE);
            if (activeOwners <= 1) {
                throw new BadRequestException("Cannot deactivate the last active owner of the tenant shop.");
            }
        }

        if (status == MembershipStatus.ACTIVE && member.getStatus() != MembershipStatus.ACTIVE) {
            entitlementService.checkUserLimit(tenantId);
        }

        member.setStatus(status);
        TenantMembership updated = membershipRepository.save(member);
        User user = userRepository.findById(member.getUserId()).orElse(null);

        String ipAddress = extractClientIp(httpRequest);
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";

        auditService.logEvent(tenantId, actorId, actorEmail, "MEMBERSHIP_STATUS_CHANGED",
                "TENANT_MEMBERSHIP", member.getId().toString(),
                "Changed status to " + status + " for user " + (user != null ? user.getEmail() : member.getUserId()), ipAddress);

        return TenantMemberDetailResponse.builder()
                .id(updated.getId())
                .tenantId(tenantId)
                .userId(member.getUserId())
                .email(user != null ? user.getEmail() : "unknown")
                .fullName(user != null ? user.getFullName() : "Unknown User")
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .role(updated.getRole())
                .status(updated.getStatus())
                .permissions(updated.getPermissions() != null ? new HashSet<>(updated.getPermissions()) : Collections.emptySet())
                .createdAt(updated.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public TenantMemberDetailResponse transferOwnership(UUID tenantId,
                                                        TransferOwnershipDto dto,
                                                        KuterUserPrincipal principal,
                                                        HttpServletRequest httpRequest) {
        validateCallerCanManageTenant(tenantId, principal);
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        if (principal == null || principal.getUserId() == null) {
            throw new ForbiddenException("Authentication required");
        }

        User currentOwnerUser = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        if (!passwordEncoder.matches(dto.getCurrentOwnerPassword(), currentOwnerUser.getPasswordHash())) {
            throw new BadRequestException("Invalid password confirmation. Ownership transfer denied.");
        }

        if (principal.getUserId().equals(dto.getNewOwnerUserId())) {
            throw new BadRequestException("You are already the owner of this shop.");
        }

        TenantMembership targetMembership = membershipRepository.findByTenantIdAndUserId(tenantId, dto.getNewOwnerUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this tenant shop."));

        if (targetMembership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BadRequestException("Cannot transfer ownership to an inactive or suspended user.");
        }

        TenantMembership currentOwnerMembership = membershipRepository.findByTenantIdAndUserId(tenantId, principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Current owner membership not found."));

        // Demote current owner to MANAGER and remove owner-exclusive permissions
        currentOwnerMembership.setRole(TenantRole.MANAGER);
        currentOwnerMembership.setPermissions(getDefaultPermissionsForRole(TenantRole.MANAGER));
        membershipRepository.save(currentOwnerMembership);

        // Promote target member to OWNER with all permissions
        targetMembership.setRole(TenantRole.OWNER);
        targetMembership.setPermissions(getDefaultPermissionsForRole(TenantRole.OWNER));
        TenantMembership updatedTarget = membershipRepository.save(targetMembership);

        User newOwnerUser = userRepository.findById(targetMembership.getUserId()).orElse(null);

        String ipAddress = extractClientIp(httpRequest);
        auditService.logEvent(tenantId, principal.getUserId(), principal.getEmail(), "TENANT_OWNERSHIP_TRANSFERRED",
                "TENANT", tenantId.toString(),
                "Ownership transferred from " + currentOwnerUser.getEmail() + " to " + (newOwnerUser != null ? newOwnerUser.getEmail() : targetMembership.getUserId()), ipAddress);

        return TenantMemberDetailResponse.builder()
                .id(updatedTarget.getId())
                .tenantId(tenantId)
                .userId(updatedTarget.getUserId())
                .email(newOwnerUser != null ? newOwnerUser.getEmail() : "unknown")
                .fullName(newOwnerUser != null ? newOwnerUser.getFullName() : "New Owner")
                .phoneNumber(newOwnerUser != null ? newOwnerUser.getPhoneNumber() : null)
                .role(updatedTarget.getRole())
                .status(updatedTarget.getStatus())
                .permissions(updatedTarget.getPermissions() != null ? new HashSet<>(updatedTarget.getPermissions()) : Collections.emptySet())
                .createdAt(updatedTarget.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void removeMember(UUID tenantId,
                             UUID memberId,
                             KuterUserPrincipal principal,
                             HttpServletRequest httpRequest) {
        validateCallerCanManageTenant(tenantId, principal);
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        TenantMembership member = membershipRepository.findByIdAndTenantId(memberId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found with ID: " + memberId));

        // Self-removal defense: user cannot delete their own membership
        if (principal != null && member.getUserId().equals(principal.getUserId())) {
            throw new BadRequestException("Cannot remove your own membership. Transfer ownership first.");
        }

        // Last active OWNER defense
        if (member.getRole() == TenantRole.OWNER && member.getStatus() == MembershipStatus.ACTIVE) {
            long activeOwners = membershipRepository.countByTenantIdAndRoleAndStatus(tenantId, TenantRole.OWNER, MembershipStatus.ACTIVE);
            if (activeOwners <= 1) {
                throw new BadRequestException("Cannot remove the last active owner of the tenant shop.");
            }
        }

        membershipRepository.delete(member);

        String ipAddress = extractClientIp(httpRequest);
        UUID actorId = principal != null ? principal.getUserId() : null;
        String actorEmail = principal != null ? principal.getEmail() : "system";

        auditService.logEvent(tenantId, actorId, actorEmail, "MEMBERSHIP_DELETED",
                "TENANT_MEMBERSHIP", memberId.toString(),
                "Removed membership ID: " + memberId, ipAddress);
    }

    private void validatePermissionsForRole(TenantRole role, Set<ShopPermission> permissions) {
        if (permissions != null && role != TenantRole.OWNER) {
            if (permissions.contains(ShopPermission.PRICES_MANAGE) || permissions.contains(ShopPermission.USERS_MANAGE)) {
                throw new BadRequestException("Permissions 'PRICES_MANAGE' and 'USERS_MANAGE' are strictly reserved for shop owners and cannot be delegated to staff.");
            }
        }
    }

    private Set<ShopPermission> getDefaultPermissionsForRole(TenantRole role) {
        Set<ShopPermission> perms = new HashSet<>();
        perms.add(ShopPermission.PRODUCTS_VIEW);
        perms.add(ShopPermission.INVENTORY_VIEW);
        perms.add(ShopPermission.SALES_VIEW);

        switch (role) {
            case OWNER -> perms.addAll(Arrays.asList(ShopPermission.values()));
            case MANAGER -> {
                perms.add(ShopPermission.PRODUCTS_MANAGE);
                perms.add(ShopPermission.INVENTORY_RECEIVE);
                perms.add(ShopPermission.INVENTORY_ADJUST);
                perms.add(ShopPermission.SALES_CREATE);
                perms.add(ShopPermission.SALES_REPORTS_VIEW);
                perms.add(ShopPermission.LENDING_REQUEST);
                perms.add(ShopPermission.LENDING_MANAGE);
                perms.add(ShopPermission.LENDING_VIEW);
                perms.add(ShopPermission.SUBSCRIPTION_VIEW);
            }
            case STOREKEEPER -> {
                perms.add(ShopPermission.INVENTORY_RECEIVE);
                perms.add(ShopPermission.INVENTORY_ADJUST);
                perms.add(ShopPermission.LENDING_VIEW);
                perms.add(ShopPermission.LENDING_REQUEST);
                perms.add(ShopPermission.LENDING_MANAGE);
            }
            case CASHIER -> perms.add(ShopPermission.SALES_CREATE);
            case VIEWER -> {
                // base 3
            }
        }
        return perms;
    }

    private Tenant validateTenantExists(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));
    }

    private void validateCallerCanAccessTenant(UUID tenantId, KuterUserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (principal.isPlatformAdmin()) {
            return;
        }
        if (!principal.hasMembershipIn(tenantId)) {
            throw new ForbiddenException("Access denied: You are not a member of this tenant shop.");
        }
    }

    private void validateCallerCanManageTenant(UUID tenantId, KuterUserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (principal.isPlatformAdmin()) {
            return;
        }
        Optional<TenantRole> role = principal.getRoleIn(tenantId);
        if (role.isEmpty() || role.get() != TenantRole.OWNER) {
            throw new ForbiddenException("Access denied: Only the shop owner can manage shop users and permissions.");
        }
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
