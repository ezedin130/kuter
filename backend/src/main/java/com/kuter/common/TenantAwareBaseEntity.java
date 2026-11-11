package com.kuter.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Base abstract superclass for all tenant-scoped domain entities.
 * Automatically enforces a non-null, immutable tenant_id foreign key.
 */
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder
@MappedSuperclass
public abstract class TenantAwareBaseEntity extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;
}
