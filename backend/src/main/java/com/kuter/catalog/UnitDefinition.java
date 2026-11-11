package com.kuter.catalog;

import com.kuter.common.BaseEntity;
import com.kuter.common.TenantAware;
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
 * UnitDefinition represents a unit of measure within a tenant shop context
 * (e.g., Piece, 50kg Bag, Metric Tonne, Bundle, Pallet, Box).
 */
@Entity
@Table(name = "unit_definitions")
@Getter
@Setter
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitDefinition extends BaseEntity implements TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
}
