package com.kuter.catalog;

import com.kuter.common.TenantAwareBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Category entity for grouping related building materials.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@ToString(callSuper = true)
@lombok.experimental.SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends TenantAwareBaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
