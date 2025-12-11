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

data class CreateAssetRequest(
    val name: String,
    val status: AssetStatus? = null,
    val category: String? = null,
    val serialNumber: String? = null,
    val tags: List<String>? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,
    val locationId: UUID? = null,
    val assignedUserId: UUID? = null,
    val warrantyExpiryDate: LocalDate? = null,
    val customFieldsJson: String? = null
)

data class UpdateAssetRequest(
    val name: String,
    val status: AssetStatus? = null,
    val category: String? = null,
    val serialNumber: String? = null,
    val tags: List<String>? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,
    val locationId: UUID? = null,
    val assignedUserId: UUID? = null,
    val warrantyExpiryDate: LocalDate? = null,
    val customFieldsJson: String? = null
)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
