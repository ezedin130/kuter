package com.kuter.identity;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Custom UserDetailsService loading User and active TenantMemberships.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TenantMembershipRepository tenantMembershipRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    TenantMembershipRepository tenantMembershipRepository) {
        this.userRepository = userRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<TenantMembership> memberships = tenantMembershipRepository.findAllByUserId(user.getId());
        return new KuterUserPrincipal(user, memberships);
    }
}
