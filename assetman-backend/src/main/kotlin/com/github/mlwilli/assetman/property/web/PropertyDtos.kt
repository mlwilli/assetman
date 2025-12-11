package com.github.mlwilli.assetman.property.web

import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.domain.UnitStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreatePropertyRequest(
    val name: String,
    val type: PropertyType,
    val code: String? = null,
    val locationId: UUID? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val notes: String? = null
)

data class UpdatePropertyRequest(
    val name: String,
    val type: PropertyType,
    val code: String? = null,
    val locationId: UUID? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val notes: String? = null
)

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
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateUnitRequest(
    val propertyId: UUID,
    val name: String,
    val floor: String? = null,
    val status: UnitStatus = UnitStatus.VACANT,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val areaSqFt: BigDecimal? = null,
    val monthlyRent: BigDecimal? = null,
    val currency: String? = "USD",
    val notes: String? = null
)

data class UpdateUnitRequest(
    val name: String,
    val floor: String? = null,
    val status: UnitStatus,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val areaSqFt: BigDecimal? = null,
    val monthlyRent: BigDecimal? = null,
    val currency: String? = "USD",
    val notes: String? = null
)
