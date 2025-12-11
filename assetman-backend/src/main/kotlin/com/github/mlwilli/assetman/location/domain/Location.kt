package com.github.mlwilli.assetman.location.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import com.github.mlwilli.assetman.common.domain.TenantScoped
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "locations",
    indexes = [
        Index(name = "idx_locations_tenant", columnList = "tenant_id"),
        Index(name = "idx_locations_parent", columnList = "parent_id")
    ]
)
class Location(
    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: LocationType,

    /**
     * Optional parent location for hierarchy.
     */
    @Column(name = "parent_id", nullable = true)
    var parentId: UUID? = null,

    /**
     * Simple materialized-path style path, e.g. /country/site/building/floor/room
     * for fast tree queries. We can improve later.
     */
    @Column(name = "path", nullable = true, length = 1024)
    var path: String? = null
) : BaseEntity(), TenantScoped
