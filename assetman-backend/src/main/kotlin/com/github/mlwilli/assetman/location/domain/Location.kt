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

    @Column(nullable = false, length = 255)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    var type: LocationType,

    /**
     * Human-friendly code, e.g. "US-WEST", "BOISE-HQ", "BLDG-A".
     */
    @Column(name = "code", nullable = true, length = 64)
    var code: String? = null,

    /**
     * Optional parent location for hierarchy.
     */
    @Column(name = "parent_id", nullable = true)
    var parentId: UUID? = null,

    /**
     * Simple materialized-path style path, e.g. "/site/building/floor/room".
     * We store IDs to keep it stable: "/{rootId}/{childId}/...".
     */
    @Column(name = "path", nullable = true, length = 1024)
    var path: String? = null,

    /**
     * Whether this location is currently active/visible for the tenant.
     */
    @Column(name = "is_active", nullable = false)
    var active: Boolean = true,

    /**
     * Optional sort order within the same parent.
     */
    @Column(name = "sort_order", nullable = true)
    var sortOrder: Int? = null,

    /**
     * Free-form description.
     */
    @Column(name = "description", nullable = true, length = 1024)
    var description: String? = null,

    /**
     * External reference ID for integrations (CMMS, ERP, etc.).
     */
    @Column(name = "external_ref", nullable = true, length = 128)
    var externalRef: String? = null,

    /**
     * Escape hatch for tenant-specific data before we formalize the model.
     */
    @Column(name = "custom_fields_json", nullable = true, columnDefinition = "TEXT")
    var customFieldsJson: String? = null

) : BaseEntity(), TenantScoped
