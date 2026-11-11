package com.kuter.audit;

import com.kuter.audit.dto.AuditLogResponseDto;
import com.kuter.common.ApiResponse;
import com.kuter.common.DateTimeUtils;
import com.kuter.common.PageResponse;
import com.kuter.tenancy.TenantContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for querying tamper-evident audit logs and user activity.
 * Supports tenant-scoped queries for shop owners/managers and platform-level queries for platform admins.
 */
@RestController
@RequestMapping("/api/v1")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Tenant-scoped audit log query for authorized shop roles (OWNER, MANAGER).
     */
    @GetMapping("/audit-logs")
    @PreAuthorize("@tenantSecurity.hasAnyRole('OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponseDto>>> getTenantAuditLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String outcome,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UUID tenantId = TenantContext.requireTenantId();
        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        PageResponse<AuditLogResponseDto> response = auditService.queryAuditLogs(
                tenantId, action, entityType, entityId, actorEmail, outcome, start, end, pageable);

        return ResponseEntity.ok(ApiResponse.success(response, "Tenant audit logs retrieved successfully"));
    }

    /**
     * Platform-level audit log query for platform administrators.
     */
    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponseDto>>> getAdminAuditLogs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String outcome,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Instant start = DateTimeUtils.parseIsoOrDate(startDate, true);
        Instant end = DateTimeUtils.parseIsoOrDate(endDate, false);
        DateTimeUtils.validateDateRange(start, end);

        PageResponse<AuditLogResponseDto> response = auditService.queryAuditLogs(
                tenantId, action, entityType, entityId, actorEmail, outcome, start, end, pageable);

        return ResponseEntity.ok(ApiResponse.success(response, "Platform audit logs retrieved successfully"));
    }
}
