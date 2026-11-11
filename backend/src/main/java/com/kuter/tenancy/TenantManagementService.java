package com.kuter.tenancy;

import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.tenancy.dto.CreateTenantDto;
import com.kuter.tenancy.dto.TenantDetailResponse;
import com.kuter.tenancy.dto.TenantSummaryResponse;
import com.kuter.tenancy.dto.UpdateTenantDto;
import com.kuter.tenancy.dto.UpdateTenantStatusDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for platform-admin tenant operations and shop-level settings.
 */
public interface TenantManagementService {

    TenantDetailResponse createTenant(CreateTenantDto dto, KuterUserPrincipal adminPrincipal, HttpServletRequest httpRequest);

    PageResponse<TenantSummaryResponse> listTenants(TenantStatus status, String search, Pageable pageable);

    TenantDetailResponse getTenantById(UUID id);

    TenantDetailResponse updateTenant(UUID id, UpdateTenantDto dto, KuterUserPrincipal adminPrincipal, HttpServletRequest httpRequest);

    TenantDetailResponse updateTenantStatus(UUID id, UpdateTenantStatusDto dto, KuterUserPrincipal adminPrincipal, HttpServletRequest httpRequest);

    TenantDetailResponse getMyTenant();

    TenantDetailResponse updateMyTenant(UpdateTenantDto dto, KuterUserPrincipal principal, HttpServletRequest httpRequest);

    com.kuter.tenancy.dto.PlatformOverviewMetricsDto getPlatformOverviewMetrics();

    com.kuter.tenancy.dto.TenantStatusDetailDto getTenantStatus(UUID tenantId);
}
