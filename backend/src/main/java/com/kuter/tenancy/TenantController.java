package com.kuter.tenancy;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.identity.MembershipStatus;
import com.kuter.identity.TenantMembershipService;
import com.kuter.identity.dto.AddMemberDto;
import com.kuter.identity.dto.TenantMemberDetailResponse;
import com.kuter.identity.dto.TransferOwnershipDto;
import com.kuter.identity.dto.UpdateMemberDto;
import com.kuter.tenancy.dto.TenantDetailResponse;
import com.kuter.tenancy.dto.TenantStatusDetailDto;
import com.kuter.tenancy.dto.UpdateTenantDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller allowing authorized shop staff to view and update their own shop settings.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/tenants")
public class TenantController {

    private final TenantManagementService tenantService;
    private final TenantMembershipService membershipService;

    public TenantController(TenantManagementService tenantService, TenantMembershipService membershipService) {
        this.tenantService = tenantService;
        this.membershipService = membershipService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> getMyTenant() {
        TenantDetailResponse response = tenantService.getMyTenant();
        return ResponseEntity.ok(ApiResponse.success(response, "Current tenant shop details retrieved successfully"));
    }

    @GetMapping("/me/members")
    @PreAuthorize("@tenantSecurity.hasAnyRole('OWNER', 'MANAGER', 'STOREKEEPER', 'CASHIER')")
    public ResponseEntity<ApiResponse<List<TenantMemberDetailResponse>>> getMyTenantMembers(
            @AuthenticationPrincipal KuterUserPrincipal principal) {
        UUID tenantId = TenantContext.requireTenantId();
        List<TenantMemberDetailResponse> members = membershipService.getTenantMembers(tenantId, principal);
        return ResponseEntity.ok(ApiResponse.success(members, "Current tenant shop staff members retrieved successfully"));
    }

    @PostMapping("/me/members")
    @PreAuthorize("@tenantSecurity.hasPermission('USERS_MANAGE')")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> addMyTenantMember(
            @Valid @RequestBody AddMemberDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMemberDetailResponse response = membershipService.addMember(tenantId, dto, principal, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Staff member added successfully"));
    }

    @PutMapping("/me/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasPermission('USERS_MANAGE')")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> updateMyTenantMember(
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateMemberDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMemberDetailResponse response = membershipService.updateMember(tenantId, memberId, dto, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Staff member updated successfully"));
    }

    @PatchMapping("/me/members/{memberId}/status")
    @PreAuthorize("@tenantSecurity.hasPermission('USERS_MANAGE')")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> updateMyTenantMemberStatus(
            @PathVariable UUID memberId,
            @RequestParam MembershipStatus status,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMemberDetailResponse response = membershipService.updateMemberStatus(tenantId, memberId, status, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Staff member status updated successfully"));
    }

    @DeleteMapping("/me/members/{memberId}")
    @PreAuthorize("@tenantSecurity.hasPermission('USERS_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> removeMyTenantMember(
            @PathVariable UUID memberId,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.requireTenantId();
        membershipService.removeMember(tenantId, memberId, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(null, "Staff member removed successfully"));
    }

    @PostMapping("/me/transfer-ownership")
    @PreAuthorize("@tenantSecurity.hasPermission('USERS_MANAGE')")
    public ResponseEntity<ApiResponse<TenantMemberDetailResponse>> transferOwnership(
            @Valid @RequestBody TransferOwnershipDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.requireTenantId();
        TenantMemberDetailResponse response = membershipService.transferOwnership(tenantId, dto, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Shop ownership transferred successfully"));
    }

    @GetMapping("/me/status")
    public ResponseEntity<ApiResponse<TenantStatusDetailDto>> getMyTenantStatus() {
        UUID tenantId = TenantContext.requireTenantId();
        TenantStatusDetailDto response = tenantService.getTenantStatus(tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Current tenant status details retrieved successfully"));
    }

    @PutMapping("/me")
    @PreAuthorize("@tenantSecurity.hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> updateMyTenant(
            @Valid @RequestBody UpdateTenantDto dto,
            @AuthenticationPrincipal KuterUserPrincipal principal,
            HttpServletRequest httpRequest) {
        TenantDetailResponse response = tenantService.updateMyTenant(dto, principal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant shop settings updated successfully"));
    }
}
