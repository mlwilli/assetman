package com.github.mlwilli.assetman.asset.web

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ---------- DTO ----------

data class AssetDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val status: AssetStatus,
    val category: String?,
    val code: String?,
    val assetTag: String?,
    val manufacturer: String?,
    val model: String?,
    val serialNumber: String?,
    val tags: List<String>,
    val purchaseDate: LocalDate?,
    val purchaseCost: BigDecimal?,
    val inServiceDate: LocalDate?,
    val retiredDate: LocalDate?,
    val disposedDate: LocalDate?,
    val warrantyExpiryDate: LocalDate?,
    val depreciationYears: Int?,
    val residualValue: BigDecimal?,
    val locationId: UUID?,
    val propertyId: UUID?,
    val unitId: UUID?,
    val assignedUserId: UUID?,
    val externalRef: String?,
    val customFieldsJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

// ---------- Requests ----------

data class CreateAssetRequest(

    @field:NotBlank(message = "Asset name is required")
    @field:Size(max = 255, message = "Asset name must be at most 255 characters")
    val name: String,

    @field:NotNull(message = "Status is required")
    val status: AssetStatus,

    @field:Size(max = 255, message = "Category must be at most 255 characters")
    val category: String? = null,

    @field:Size(max = 64, message = "Code must be at most 64 characters")
    val code: String? = null,

    @field:Size(max = 128, message = "Asset tag must be at most 128 characters")
    val assetTag: String? = null,

    @field:Size(max = 255, message = "Manufacturer must be at most 255 characters")
    val manufacturer: String? = null,

    @field:Size(max = 255, message = "Model must be at most 255 characters")
    val model: String? = null,

    @field:Size(max = 255, message = "Serial number must be at most 255 characters")
    val serialNumber: String? = null,

    // tags: optional, no strict validation yet
    val tags: List<String>? = null,

    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,

    val inServiceDate: LocalDate? = null,
    val retiredDate: LocalDate? = null,
    val disposedDate: LocalDate? = null,
    val warrantyExpiryDate: LocalDate? = null,

    val depreciationYears: Int? = null,
    val residualValue: BigDecimal? = null,

    val locationId: UUID? = null,
    val propertyId: UUID? = null,
    val unitId: UUID? = null,
    val assignedUserId: UUID? = null,

    @field:Size(max = 128, message = "External reference must be at most 128 characters")
    val externalRef: String? = null,

    // JSON blob; we might later add custom validation / schema
    val customFieldsJson: String? = null
)

data class UpdateAssetRequest(

    @field:NotBlank(message = "Asset name is required")
    @field:Size(max = 255, message = "Asset name must be at most 255 characters")
    val name: String,

    @field:NotNull(message = "Status is required")
    val status: AssetStatus,

    @field:Size(max = 255, message = "Category must be at most 255 characters")
    val category: String? = null,

    @field:Size(max = 64, message = "Code must be at most 64 characters")
    val code: String? = null,

    @field:Size(max = 128, message = "Asset tag must be at most 128 characters")
    val assetTag: String? = null,

    @field:Size(max = 255, message = "Manufacturer must be at most 255 characters")
    val manufacturer: String? = null,

    @field:Size(max = 255, message = "Model must be at most 255 characters")
    val model: String? = null,

    @field:Size(max = 255, message = "Serial number must be at most 255 characters")
    val serialNumber: String? = null,

    val tags: List<String>? = null,

    val purchaseDate: LocalDate? = null,
    val purchaseCost: BigDecimal? = null,

    val inServiceDate: LocalDate? = null,
    val retiredDate: LocalDate? = null,
    val disposedDate: LocalDate? = null,
    val warrantyExpiryDate: LocalDate? = null,

    val depreciationYears: Int? = null,
    val residualValue: BigDecimal? = null,

    val locationId: UUID? = null,
    val propertyId: UUID? = null,
    val unitId: UUID? = null,
    val assignedUserId: UUID? = null,

    @field:Size(max = 128, message = "External reference must be at most 128 characters")
    val externalRef: String? = null,

    val customFieldsJson: String? = null
)

// ---------- Mapper ----------

fun Asset.toDto(): AssetDto =
    AssetDto(
        id = id,
        tenantId = tenantId,
        name = name,
        status = status,
        category = category,
        code = code,
        assetTag = assetTag,
        manufacturer = manufacturer,
        model = model,
        serialNumber = serialNumber,
        tags = tags
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
        purchaseDate = purchaseDate,
        purchaseCost = purchaseCost,
        inServiceDate = inServiceDate,
        retiredDate = retiredDate,
        disposedDate = disposedDate,
        warrantyExpiryDate = warrantyExpiryDate,
        depreciationYears = depreciationYears,
        residualValue = residualValue,
        locationId = locationId,
        propertyId = propertyId,
        unitId = unitId,
        assignedUserId = assignedUserId,
        externalRef = externalRef,
        customFieldsJson = customFieldsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
