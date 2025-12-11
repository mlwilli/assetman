package com.github.mlwilli.assetman.asset.web

import com.github.mlwilli.assetman.asset.domain.AssetStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateAssetRequest(
    @field:NotBlank(message = "Asset name is required")
    @field:Size(max = 255, message = "Asset name must be at most 255 characters")
    val name: String,

    @field:NotNull(message = "Status is required")
    val status: AssetStatus? = null,

    @field:Size(max = 255, message = "Category must be at most 255 characters")
    val category: String? = null,

    @field:Size(max = 255, message = "Serial number must be at most 255 characters")
    val serialNumber: String? = null,

    // tags: optional, no strict validation yet, can add @Size or per-entry checks later
    val tags: List<String>? = null,

    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,
    val locationId: UUID? = null,
    val assignedUserId: UUID? = null,
    val warrantyExpiryDate: LocalDate? = null,

    // JSON blob; we might later add custom validation / schema
    val customFieldsJson: String? = null
)

data class UpdateAssetRequest(
    @field:NotBlank(message = "Asset name is required")
    @field:Size(max = 255, message = "Asset name must be at most 255 characters")
    val name: String,

    @field:NotNull(message = "Status is required")
    val status: AssetStatus? = null,

    @field:Size(max = 255, message = "Category must be at most 255 characters")
    val category: String? = null,

    @field:Size(max = 255, message = "Serial number must be at most 255 characters")
    val serialNumber: String? = null,

    val tags: List<String>? = null,
    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,
    val locationId: UUID? = null,
    val assignedUserId: UUID? = null,
    val warrantyExpiryDate: LocalDate? = null,
    val customFieldsJson: String? = null
)
