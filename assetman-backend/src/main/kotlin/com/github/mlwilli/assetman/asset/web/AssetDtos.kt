package com.github.mlwilli.assetman.asset.web

import com.github.mlwilli.assetman.asset.domain.AssetStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class AssetDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val status: AssetStatus,
    val category: String?,
    val serialNumber: String?,
    val tags: List<String>,
    val purchaseDate: LocalDate?,
    val purchaseCost: BigDecimal?,
    val locationId: UUID?,
    val assignedUserId: UUID?,
    val warrantyExpiryDate: LocalDate?,
    val customFieldsJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)


