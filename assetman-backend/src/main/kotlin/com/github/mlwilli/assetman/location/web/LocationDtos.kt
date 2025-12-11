package com.github.mlwilli.assetman.location.web

import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class LocationDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val type: LocationType,
    val code: String?,
    val parentId: UUID?,
    val path: String?,
    val active: Boolean,
    val sortOrder: Int?,
    val description: String?,
    val externalRef: String?,
    val customFieldsJson: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * For tree representation in the UI.
 */
data class LocationTreeNodeDto(
    val id: UUID,
    val name: String,
    val type: LocationType,
    val code: String?,
    val parentId: UUID?,
    val active: Boolean,
    val sortOrder: Int?,
    val children: List<LocationTreeNodeDto>
)

data class CreateLocationRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val type: LocationType,

    val parentId: UUID? = null,

    @field:Size(max = 64)
    val code: String? = null,

    val active: Boolean = true,

    val sortOrder: Int? = null,

    @field:Size(max = 1024)
    val description: String? = null,

    @field:Size(max = 128)
    val externalRef: String? = null,

    val customFieldsJson: String? = null
)

data class UpdateLocationRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val name: String,

    val type: LocationType,

    val parentId: UUID? = null,

    @field:Size(max = 64)
    val code: String? = null,

    val active: Boolean = true,

    val sortOrder: Int? = null,

    @field:Size(max = 1024)
    val description: String? = null,

    @field:Size(max = 128)
    val externalRef: String? = null,

    val customFieldsJson: String? = null
)

/**
 * Mapping from entity → DTO.
 */
fun Location.toDto(): LocationDto =
    LocationDto(
        id = id,
        tenantId = tenantId,
        name = name,
        type = type,
        code = code,
        parentId = parentId,
        path = path,
        active = active,
        sortOrder = sortOrder,
        description = description,
        externalRef = externalRef,
        customFieldsJson = customFieldsJson,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
