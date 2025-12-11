package com.github.mlwilli.assetman.asset.domain

import com.github.mlwilli.assetman.common.domain.BaseEntity
import com.github.mlwilli.assetman.common.domain.TenantScoped
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "assets")
class Asset(
    @Column(name = "tenant_id", nullable = false)
    override val tenantId: UUID,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AssetStatus = AssetStatus.IN_SERVICE,

    @Column(nullable = true)
    var category: String? = null,

    @Column(nullable = true)
    var serialNumber: String? = null,

    /**
     * Comma-separated tags for now (e.g. "network,server,critical").
     * We'll normalize later if needed.
     */
    @Column(nullable = true, length = 1024)
    var tags: String? = null,

    @Column(nullable = true)
    var purchaseDate: LocalDate? = null,

    @Column(nullable = true, precision = 19, scale = 2)
    var purchaseCost: BigDecimal? = null,

    @Column(nullable = true)
    var locationId: UUID? = null,

    @Column(nullable = true)
    var assignedUserId: UUID? = null,

    @Column(nullable = true)
    var warrantyExpiryDate: LocalDate? = null,

    /**
     * JSON string for flexible custom fields.
     * e.g. {"rack":"R12","u":"14","ipAddress":"10.0.0.5"}
     */
    @Lob
    @Column(nullable = true)
    var customFieldsJson: String? = null
) : BaseEntity(), TenantScoped
