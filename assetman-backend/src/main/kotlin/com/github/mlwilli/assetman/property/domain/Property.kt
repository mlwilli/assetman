package com.github.mlwilli.assetman.property.domain

import com.github.mlwilli.assetman.shared.domain.BaseEntity
import com.github.mlwilli.assetman.shared.domain.TenantScoped
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(
    name = "properties",
    indexes = [
        Index(name = "idx_properties_tenant", columnList = "tenant_id"),
        Index(name = "idx_properties_location", columnList = "location_id")
    ]
)
class Property(
    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: PropertyType,

    /**
     * Human-friendly code, e.g. “BOISE-HQ” or “BLDG-A”.
     */
    @Column(name = "code", nullable = true, length = 64)
    var code: String? = null,

    /**
     * Optional link into the Location hierarchy (SITE/BUILDING/FLOOR/etc).
     */
    @Column(name = "location_id", nullable = true)
    var locationId: UUID? = null,

    // Address fields – can be refined later.
    @Column(name = "address_line1", nullable = true, length = 255)
    var addressLine1: String? = null,

    @Column(name = "address_line2", nullable = true, length = 255)
    var addressLine2: String? = null,

    @Column(name = "city", nullable = true, length = 128)
    var city: String? = null,

    @Column(name = "state", nullable = true, length = 128)
    var state: String? = null,

    @Column(name = "postal_code", nullable = true, length = 32)
    var postalCode: String? = null,

    @Column(name = "country", nullable = true, length = 128)
    var country: String? = null,

    @Column(name = "notes", nullable = true, length = 4000)
    var notes: String? = null
) : BaseEntity(), TenantScoped
