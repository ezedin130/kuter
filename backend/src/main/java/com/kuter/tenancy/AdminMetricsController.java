package com.kuter.tenancy;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import com.kuter.tenancy.dto.PlatformOverviewMetricsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for platform administrators to query high-level operational platform metrics.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH + "/admin/metrics")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminMetricsController {

    private final TenantManagementService tenantService;

    public AdminMetricsController(TenantManagementService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping({"/overview", ""})
    public ResponseEntity<ApiResponse<PlatformOverviewMetricsDto>> getPlatformOverview() {
        PlatformOverviewMetricsDto response = tenantService.getPlatformOverviewMetrics();
        return ResponseEntity.ok(ApiResponse.success(response, "Platform overview metrics retrieved successfully"));
    }
}
