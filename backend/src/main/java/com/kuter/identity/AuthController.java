package com.kuter.identity;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.identity.dto.AuthResponse;
import com.kuter.identity.dto.ChangePasswordRequest;
import com.kuter.identity.dto.LoginRequest;
import com.kuter.identity.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing user authentication, login, logout, self profile retrieval, and password management endpoints.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal KuterUserPrincipal principal,
                                                    HttpServletRequest httpRequest) {
        authService.logout(principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMe(@AuthenticationPrincipal KuterUserPrincipal principal) {
        UserProfileResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal KuterUserPrincipal principal,
                                                            @Valid @RequestBody ChangePasswordRequest request,
                                                            HttpServletRequest httpRequest) {
        authService.changePassword(principal, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Password changed successfully"));
    }
}
