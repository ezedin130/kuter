package com.kuter.identity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetails implementation encapsulating authenticated identity,
 * platform administrator privileges, and authorized tenant memberships.
 */
@Getter
public class KuterUserPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final boolean isPlatformAdmin;
    private final UserStatus status;
    private final Map<UUID, TenantMembership> membershipsByTenantId;
    private final Collection<? extends GrantedAuthority> authorities;

    @Setter
    private UUID currentTenantId;

    public KuterUserPrincipal(User user, Collection<TenantMembership> memberships) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.fullName = user.getFullName();
        this.isPlatformAdmin = user.isPlatformAdmin();
        this.status = user.getStatus();

        if (memberships != null) {
            this.membershipsByTenantId = memberships.stream()
                    .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                    .collect(Collectors.toMap(TenantMembership::getTenantId, m -> m, (a, b) -> a));
        } else {
            this.membershipsByTenantId = Collections.emptyMap();
        }

        Set<GrantedAuthority> auths = new HashSet<>();
        if (this.isPlatformAdmin) {
            auths.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }

        for (TenantMembership membership : this.membershipsByTenantId.values()) {
            auths.add(new SimpleGrantedAuthority("ROLE_TENANT_" + membership.getRole().name()));
        }

        this.authorities = Collections.unmodifiableSet(auths);
    }

    public boolean hasMembershipIn(UUID tenantId) {
        return tenantId != null && membershipsByTenantId.containsKey(tenantId);
    }

    public Optional<TenantRole> getRoleIn(UUID tenantId) {
        return Optional.ofNullable(membershipsByTenantId.get(tenantId))
                .map(TenantMembership::getRole);
    }

    public Optional<TenantMembership> getMembershipIn(UUID tenantId) {
        return Optional.ofNullable(membershipsByTenantId.get(tenantId));
    }

    public Set<UUID> getAuthorizedTenantIds() {
        return Collections.unmodifiableSet(membershipsByTenantId.keySet());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
