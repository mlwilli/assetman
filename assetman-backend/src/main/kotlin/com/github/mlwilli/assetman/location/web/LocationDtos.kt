package com.github.mlwilli.assetman.location.web

import com.github.mlwilli.assetman.location.domain.LocationType
import java.time.Instant
import java.util.UUID

data class LocationDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val type: LocationType,
    val parentId: UUID?,
    val path: String?,
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
    val parentId: UUID?,
    val children: List<LocationTreeNodeDto>
)

data class CreateLocationRequest(
    val name: String,
    val type: LocationType,
    val parentId: UUID? = null
)

data class UpdateLocationRequest(
    val name: String,
    val type: LocationType,
    val parentId: UUID? = null
)
