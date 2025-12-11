package com.github.mlwilli.assetman.location.service

import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import com.github.mlwilli.assetman.location.web.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LocationService(
    private val locationRepository: LocationRepository
) {

    // ----------------------------------------------------------------------
    // Queries
    // ----------------------------------------------------------------------

    @Transactional(readOnly = true)
    fun listLocations(
        type: LocationType?,
        parentId: UUID?,
        active: Boolean?,
        search: String?
    ): List<LocationDto> {
        val tenantId = currentTenantId()

        val list = locationRepository.search(
            tenantId = tenantId,
            type = type,
            parentId = parentId,
            active = active,
            search = search
        )

        return list.map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getLocation(id: UUID): LocationDto {
        val tenantId = currentTenantId()

        val location = locationRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Location not found")

        return location.toDto()
    }

    @Transactional(readOnly = true)
    fun getLocationTree(): List<LocationTreeNodeDto> {
        val tenantId = currentTenantId()

        // Only active locations in the tree for now.
        val all = locationRepository.findAllByTenantIdAndActiveTrueOrderByNameAsc(tenantId)

        val byParent = all.groupBy { it.parentId }

        fun build(parentId: UUID?): List<LocationTreeNodeDto> {
            val children = byParent[parentId].orEmpty()
                .sortedWith(
                    compareBy<Location> { it.sortOrder ?: Int.MAX_VALUE }
                        .thenBy { it.name.lowercase() }
                )

            return children.map { loc ->
                LocationTreeNodeDto(
                    id = loc.id,
                    name = loc.name,
                    type = loc.type,
                    code = loc.code,
                    parentId = loc.parentId,
                    active = loc.active,
                    sortOrder = loc.sortOrder,
                    children = build(loc.id)
                )
            }
        }

        // root nodes = parentId == null
        return build(null)
    }

    // ----------------------------------------------------------------------
    // Mutations
    // ----------------------------------------------------------------------

    @Transactional
    fun createLocation(request: CreateLocationRequest): LocationDto {
        val tenantId = currentTenantId()

        val parent: Location? = request.parentId?.let { parentId ->
            locationRepository.findByIdAndTenantId(parentId, tenantId)
                ?: throw NotFoundException("Parent location not found")
        }

        // First persist without path so we get an ID.
        var location = Location(
            tenantId = tenantId,
            name = request.name,
            type = request.type,
            code = request.code,
            parentId = request.parentId,
            path = null,
            active = request.active,
            sortOrder = request.sortOrder,
            description = request.description,
            externalRef = request.externalRef,
            customFieldsJson = request.customFieldsJson
        )

        location = locationRepository.save(location)

        // Compute parent path. Prefer parent's existing path; fall back to "/{parentId}" defensively.
        val parentPath = parent?.path ?: parent?.id?.let { "/$it" }

        // Now compute path with the real ID.
        location.path = buildPath(parentPath, location.id)

        val saved = locationRepository.save(location)
        return saved.toDto()
    }

    @Transactional
    fun updateLocation(id: UUID, request: UpdateLocationRequest): LocationDto {
        val tenantId = currentTenantId()

        val location = locationRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Location not found")

        val oldPath = location.path ?: "/$id"

        // Resolve and validate parent, including cycle prevention.
        val parent: Location? = request.parentId?.let { parentId ->
            if (parentId == id) {
                throw ConflictException("Location cannot be its own parent")
            }

            val parentLocation = locationRepository.findByIdAndTenantId(parentId, tenantId)
                ?: throw NotFoundException("Parent location not found")

            val parentPath = parentLocation.path ?: "/${parentLocation.id}"

            // Prevent cycles: parent cannot be the node itself or any of its descendants.
            if (parentPath == oldPath || parentPath.startsWith("$oldPath/")) {
                throw ConflictException("Cannot set a descendant as parent")
            }

            parentLocation
        }

        // Compute new path based on parent.
        val parentPath = parent?.path ?: parent?.id?.let { "/$it" }
        val newPath = buildPath(parentPath, id)

        // Update fields from request.
        location.name = request.name
        location.type = request.type
        location.code = request.code
        location.parentId = request.parentId
        location.active = request.active
        location.sortOrder = request.sortOrder
        location.description = request.description
        location.externalRef = request.externalRef
        location.customFieldsJson = request.customFieldsJson
        location.path = newPath

        // If the path changed, we need to move the entire subtree.
        if (newPath != oldPath) {
            val descendants = locationRepository.findAllByTenantIdAndPathStartingWith(
                tenantId = tenantId,
                pathPrefix = "$oldPath/"
            )

            descendants.forEach { child ->
                val childOldPath = child.path ?: return@forEach
                // Replace the old prefix with the new prefix.
                child.path = newPath + childOldPath.removePrefix(oldPath)
                locationRepository.save(child)
            }
        }

        val saved = locationRepository.save(location)
        return saved.toDto()
    }

    @Transactional
    fun deleteLocation(id: UUID) {
        val tenantId = currentTenantId()

        val location = locationRepository.findByIdAndTenantId(id, tenantId)
            ?: return // keep idempotent delete semantics for now

        // Prevent deleting a location that still has children.
        if (locationRepository.existsByTenantIdAndParentId(tenantId, id)) {
            throw ConflictException("Cannot delete location with child locations")
        }

        // Later: enforce that no properties/assets reference this location.

        locationRepository.delete(location)
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    private fun buildPath(parentPath: String?, id: UUID): String =
        if (parentPath.isNullOrBlank()) {
            "/$id"
        } else {
            parentPath.trimEnd('/') + "/$id"
        }
}
