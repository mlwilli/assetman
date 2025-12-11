package com.github.mlwilli.assetman.property.domain

import com.github.mlwilli.assetman.shared.domain.BaseEntity
import com.github.mlwilli.assetman.shared.domain.TenantScoped
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(
    name = "units",
    indexes = [
        Index(name = "idx_units_tenant", columnList = "tenant_id"),
        Index(name = "idx_units_property", columnList = "property_id"),
        Index(name = "idx_units_status", columnList = "status")
    ]
)
class Unit(
    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(name = "property_id", nullable = false)
    var propertyId: UUID,

    @Column(nullable = false, length = 128)
    var name: String, // e.g., “Suite 301”, “Unit 2B”

    @Column(name = "floor", nullable = true, length = 32)
    var floor: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: UnitStatus = UnitStatus.VACANT,

    // Simple basic fields – later we can expand for leases, etc.
    @Column(name = "bedrooms", nullable = true)
    var bedrooms: Int? = null,

    @Column(name = "bathrooms", nullable = true)
    var bathrooms: Int? = null,

    @Column(name = "area_sq_ft", nullable = true, precision = 12, scale = 2)
    var areaSqFt: BigDecimal? = null,

    @Column(name = "monthly_rent", nullable = true, precision = 14, scale = 2)
    var monthlyRent: BigDecimal? = null,

    @Column(name = "currency", nullable = true, length = 3)
    var currency: String? = "USD",

    @Column(name = "notes", nullable = true, length = 4000)
    var notes: String? = null
) : BaseEntity(), TenantScoped
