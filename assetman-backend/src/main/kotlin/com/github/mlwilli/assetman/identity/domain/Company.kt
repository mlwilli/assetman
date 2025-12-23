package com.github.mlwilli.assetman.identity.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "companies",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_company_tenant_slug",
            columnNames = ["tenant_id", "slug"]
        )
    ],
    indexes = [
        Index(name = "ix_company_tenant", columnList = "tenant_id")
    ]
)
class Company(

    @Column(name = "tenant_id", nullable = false, updatable = false)
    val tenantId: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var slug: String,

    @Column(nullable = false)
    var active: Boolean = true

) : BaseEntity()
