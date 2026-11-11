package com.kuter.identity;

import com.kuter.identity.dto.AuthResponse;
import com.kuter.identity.dto.ChangePasswordRequest;
import com.kuter.identity.dto.LoginRequest;
import com.kuter.identity.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for user authentication and session management.
 */
public interface AuthService {

    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    void logout(KuterUserPrincipal principal, HttpServletRequest httpRequest);

    UserProfileResponse getCurrentUser(KuterUserPrincipal principal);

    void changePassword(KuterUserPrincipal principal, ChangePasswordRequest request, HttpServletRequest httpRequest);
}
