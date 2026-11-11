package com.kuter.audit;

import com.kuter.audit.dto.AuditLogResponseDto;
import com.kuter.common.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/**
 * Service contract for publishing and querying immutable audit events.
 */
public interface AuditService {

    void logEvent(UUID tenantId,
                  UUID actorId,
                  String actorEmail,
                  String action,
                  String entityType,
                  String entityId,
                  String details,
                  String ipAddress);

    void logEvent(UUID tenantId,
                  UUID actorId,
                  String actorEmail,
                  String action,
                  String entityType,
                  String entityId,
                  String details,
                  String ipAddress,
                  String outcome);

    PageResponse<AuditLogResponseDto> queryAuditLogs(UUID tenantId,
                                                    String action,
                                                    String entityType,
                                                    String entityId,
                                                    String actorEmail,
                                                    String outcome,
                                                    Instant startDate,
                                                    Instant endDate,
                                                    Pageable pageable);
}
