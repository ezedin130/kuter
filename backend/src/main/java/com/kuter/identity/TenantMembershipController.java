package com.kuter.identity;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.TenantMemberDetailResponse;
import com.kuter.identity.dto.UpdateMemberDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller for managing tenant shop members and role assignments.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/tenants/{tenantId}/members")
public class TenantMembershipController {

    private final TenantMembershipService membershipService;

    public TenantMembershipController(TenantMembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping
    @PreAuthorize("@tenantSecurity.isMemberOf(#tenantId) or hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<List<TenantMemberDetailResponse>>> getMembers(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal KuterUserPrincipal principal) {
        List<TenantMemberDetailResponse> members = membershipService.getTenantMembers(tenantId, principal);
        return ResponseEntity.ok(ApiResponse.success(members, "Tenant members retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("@tenantSecurity.canManageMembersOf(#tenantId)")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> addMember(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AddMemberDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TenantMemberDetailResponse member = membershipService.addMember(tenantId, dto, principal, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(member, "Member added to tenant shop successfully"));
    }

    @PutMapping("/{memberId}")
    @PreAuthorize("@tenantSecurity.canManageMembersOf(#tenantId)")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> updateMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TenantMemberDetailResponse member = membershipService.updateMember(tenantId, memberId, dto, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(member, "Member updated successfully"));
    }

    @DeleteMapping("/{memberId}")
    @PreAuthorize("@tenantSecurity.canManageMembersOf(#tenantId)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID tenantId,
            @PathVariable UUID memberId,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        membershipService.removeMember(tenantId, memberId, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Member removed from tenant shop successfully"));
    }
}
