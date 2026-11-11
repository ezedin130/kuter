package com.kuter.identity;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.identity.dto.UserProfileResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing user management and self-identity endpoints.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(@AuthenticationPrincipal KuterUserPrincipal principal) {
        UserProfileResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.success(response, "Current user profile retrieved successfully"));
    }
}
