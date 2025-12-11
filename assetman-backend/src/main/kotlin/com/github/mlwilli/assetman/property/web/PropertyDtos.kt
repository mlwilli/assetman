package com.github.mlwilli.assetman.property.web

import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ===== Property DTOs =====

data class PropertyDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val type: PropertyType,
    val code: String?,
    val locationId: UUID?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val notes: String?,
    val active: Boolean,
    val yearBuilt: Int?,
    val totalUnits: Int?,
    val externalRef: String?,
    val customFieldsJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreatePropertyRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val type: PropertyType,

    @field:Size(max = 64)
    val code: String? = null,

    val locationId: UUID? = null,

    @field:Size(max = 255)
    val addressLine1: String? = null,

    @field:Size(max = 255)
    val addressLine2: String? = null,

    @field:Size(max = 128)
    val city: String? = null,

    @field:Size(max = 128)
    val state: String? = null,

    @field:Size(max = 32)
    val postalCode: String? = null,

    @field:Size(max = 128)
    val country: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null,

    val active: Boolean = true,
    val yearBuilt: Int? = null,
    val totalUnits: Int? = null,

    @field:Size(max = 128)
    val externalRef: String? = null,

    val customFieldsJson: String? = null
)

data class UpdatePropertyRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val type: PropertyType,

    @field:Size(max = 64)
    val code: String? = null,

    val locationId: UUID? = null,

    @field:Size(max = 255)
    val addressLine1: String? = null,

    @field:Size(max = 255)
    val addressLine2: String? = null,

    @field:Size(max = 128)
    val city: String? = null,

    @field:Size(max = 128)
    val state: String? = null,

    @field:Size(max = 32)
    val postalCode: String? = null,

    @field:Size(max = 128)
    val country: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null,

    val active: Boolean = true,
    val yearBuilt: Int? = null,
    val totalUnits: Int? = null,

    @field:Size(max = 128)
    val externalRef: String? = null,

    val customFieldsJson: String? = null
)

fun Property.toDto(): PropertyDto =
    PropertyDto(
        id = id,
        tenantId = tenantId,
        name = name,
        type = type,
        code = code,
        locationId = locationId,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country,
        notes = notes,
        active = active,
        yearBuilt = yearBuilt,
        totalUnits = totalUnits,
        externalRef = externalRef,
        customFieldsJson = customFieldsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

// ===== Unit DTOs =====

data class UnitDto(
    val id: UUID,
    val tenantId: UUID,
    val propertyId: UUID,
    val name: String,
    val floor: String?,
    val status: UnitStatus,
    val bedrooms: Int?,
    val bathrooms: Int?,
    val areaSqFt: BigDecimal?,
    val monthlyRent: BigDecimal?,
    val currency: String?,
    val availableFrom: LocalDate?,
    val availableTo: LocalDate?,
    val maxOccupancy: Int?,
    val furnished: Boolean,
    val externalRef: String?,
    val notes: String?,
    val customFieldsJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateUnitRequest(
    val propertyId: UUID,

    @field:NotBlank
    @field:Size(max = 128)
    val name: String,

    @field:Size(max = 32)
    val floor: String? = null,

    val status: UnitStatus = UnitStatus.VACANT,

    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val areaSqFt: BigDecimal? = null,
    val monthlyRent: BigDecimal? = null,

    @field:Size(max = 3)
    val currency: String? = "USD",

    val availableFrom: LocalDate? = null,
    val availableTo: LocalDate? = null,
    val maxOccupancy: Int? = null,
    val furnished: Boolean = false,

    @field:Size(max = 128)
    val externalRef: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null,

    val customFieldsJson: String? = null
)

data class UpdateUnitRequest(
    @field:NotBlank
    @field:Size(max = 128)
    val name: String,

    @field:Size(max = 32)
    val floor: String? = null,

    val status: UnitStatus,

    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val areaSqFt: BigDecimal? = null,
    val monthlyRent: BigDecimal? = null,

    @field:Size(max = 3)
    val currency: String? = "USD",

    val availableFrom: LocalDate? = null,
    val availableTo: LocalDate? = null,
    val maxOccupancy: Int? = null,
    val furnished: Boolean = false,

    @field:Size(max = 128)
    val externalRef: String? = null,

    @field:Size(max = 4000)
    val notes: String? = null,

    val customFieldsJson: String? = null
)

fun Unit.toDto(): UnitDto =
    UnitDto(
        id = id,
        tenantId = tenantId,
        propertyId = propertyId,
        name = name,
        floor = floor,
        status = status,
        bedrooms = bedrooms,
        bathrooms = bathrooms,
        areaSqFt = areaSqFt,
        monthlyRent = monthlyRent,
        currency = currency,
        availableFrom = availableFrom,
        availableTo = availableTo,
        maxOccupancy = maxOccupancy,
        furnished = furnished,
        externalRef = externalRef,
        notes = notes,
        customFieldsJson = customFieldsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
