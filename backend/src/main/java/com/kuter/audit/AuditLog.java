package com.kuter.audit;

import com.kuter.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * AuditLog entity representing an immutable tamper-evident record of
 * security-critical and business-critical operations.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(name = "tenant_id", updatable = false)
    private UUID tenantId;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_email", nullable = false, length = 255, updatable = false)
    private String actorEmail;

    @Column(name = "action", nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 100, updatable = false)
    private String entityId;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "ip_address", length = 50, updatable = false)
    private String ipAddress;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    @Column(name = "outcome", length = 50, nullable = false, updatable = false)
    @Builder.Default
    private String outcome = "SUCCESS";
}
