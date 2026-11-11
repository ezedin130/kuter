package com.kuter.audit;

import com.kuter.audit.dto.AuditLogResponseDto;
import com.kuter.common.Constants;
import com.kuter.common.PageResponse;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of AuditService persisting and querying immutable audit records.
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void logEvent(UUID tenantId,
                         UUID actorId,
                         String actorEmail,
                         String action,
                         String entityType,
                         String entityId,
                         String details,
                         String ipAddress) {
        logEvent(tenantId, actorId, actorEmail, action, entityType, entityId, details, ipAddress, "SUCCESS");
    }

    @Override
    @Transactional
    public void logEvent(UUID tenantId,
                         UUID actorId,
                         String actorEmail,
                         String action,
                         String entityType,
                         String entityId,
                         String details,
                         String ipAddress,
                         String outcome) {
        String correlationId = MDC.get(Constants.CORRELATION_ID_MDC_KEY);

        AuditLog logEntry = AuditLog.builder()
                .tenantId(tenantId)
                .actorId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .correlationId(correlationId)
                .outcome(StringUtils.hasText(outcome) ? outcome : "SUCCESS")
                .build();

        auditLogRepository.save(logEntry);
        log.debug("Audit log recorded: action={}, entityType={}, entityId={}, tenantId={}, outcome={}",
                action, entityType, entityId, tenantId, outcome);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponseDto> queryAuditLogs(UUID tenantId,
                                                           String action,
                                                           String entityType,
                                                           String entityId,
                                                           String actorEmail,
                                                           String outcome,
                                                           Instant startDate,
                                                           Instant endDate,
                                                           Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null) {
                predicates.add(cb.equal(root.get("tenantId"), tenantId));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(entityType)) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (StringUtils.hasText(entityId)) {
                predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            }
            if (StringUtils.hasText(actorEmail)) {
                predicates.add(cb.like(cb.lower(root.get("actorEmail")), "%" + actorEmail.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(outcome)) {
                predicates.add(cb.equal(root.get("outcome"), outcome.trim().toUpperCase()));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        List<AuditLogResponseDto> dtos = page.getContent().stream()
                .map(this::mapToDto)
                .toList();

        return PageResponse.of(page, dtos);
    }

    private AuditLogResponseDto mapToDto(AuditLog log) {
        return AuditLogResponseDto.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .actorId(log.getActorId())
                .actorEmail(log.getActorEmail())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .correlationId(log.getCorrelationId())
                .outcome(log.getOutcome())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
