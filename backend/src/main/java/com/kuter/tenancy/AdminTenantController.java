package com.kuter.tenancy;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.tenancy.dto.CreateTenantDto;
import com.kuter.tenancy.dto.TenantDetailResponse;
import com.kuter.tenancy.dto.TenantStatusDetailDto;
import com.kuter.tenancy.dto.TenantSummaryResponse;
import com.kuter.tenancy.dto.UpdateTenantDto;
import com.kuter.tenancy.dto.UpdateTenantStatusDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Platform Administration Controller for managing tenant shops across the platform.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/admin/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminTenantController {

    private final TenantManagementService tenantService;

    public AdminTenantController(TenantManagementService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TenantDetailResponse>> createTenant(
            @Valid @RequestBody CreateTenantDto dto,
            @AuthenticationPrincipal KuterUserPrincipal adminPrincipal,
            HttpServletRequest httpRequest) {
        TenantDetailResponse response = tenantService.createTenant(dto, adminPrincipal, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Tenant shop created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TenantSummaryResponse>>> listTenants(
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TenantSummaryResponse> response = tenantService.listTenants(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenants retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> getTenantById(@PathVariable UUID id) {
        TenantDetailResponse response = tenantService.getTenantById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant details retrieved successfully"));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TenantStatusDetailDto>> getTenantStatus(@PathVariable UUID id) {
        TenantStatusDetailDto response = tenantService.getTenantStatus(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant status details retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> updateTenant(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantDto dto,
            @AuthenticationPrincipal KuterUserPrincipal adminPrincipal,
            HttpServletRequest httpRequest) {
        TenantDetailResponse response = tenantService.updateTenant(id, dto, adminPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant updated successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TenantDetailResponse>> updateTenantStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTenantStatusDto dto,
            @AuthenticationPrincipal KuterUserPrincipal adminPrincipal,
            HttpServletRequest httpRequest) {
        TenantDetailResponse response = tenantService.updateTenantStatus(id, dto, adminPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "Tenant status updated successfully"));
    }
}
