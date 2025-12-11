package com.github.mlwilli.assetman.location.service

import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.repo.LocationRepository
import com.github.mlwilli.assetman.location.web.CreateLocationRequest
import com.github.mlwilli.assetman.location.web.LocationDto
import com.github.mlwilli.assetman.location.web.LocationTreeNodeDto
import com.github.mlwilli.assetman.location.web.UpdateLocationRequest
import com.github.mlwilli.assetman.shared.security.TenantContext
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class LocationService(
    private val locationRepository: LocationRepository
) {

    @Transactional(readOnly = true)
    fun listLocations(
        type: LocationType?,
        parentId: UUID?,
        search: String?
    ): List<LocationDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val list = locationRepository.search(
            tenantId = ctx.tenantId,
            type = type,
            parentId = parentId,
            search = search
        )
        return list.map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getLocation(id: UUID): LocationDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val location = locationRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Location not found")
        return location.toDto()
    }

    @Transactional
    fun createLocation(request: CreateLocationRequest): LocationDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val parentPath = request.parentId?.let { parentId ->
            val parent = locationRepository.findByIdAndTenantId(parentId, ctx.tenantId)
                ?: throw EntityNotFoundException("Parent location not found")
            parent.path ?: "/${parent.id}"
        }

        val location = Location(
            tenantId = ctx.tenantId,
            name = request.name,
            type = request.type,
            parentId = request.parentId,
            path = buildPath(parentPath)
        )

        val saved = locationRepository.save(location)

        // After we have ID, adjust path if needed.
        if (saved.path == null) {
            saved.path = "/${saved.id}"
        }

        return saved.toDto()
    }

    @Transactional
    fun updateLocation(id: UUID, request: UpdateLocationRequest): LocationDto {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")

        val location = locationRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: throw EntityNotFoundException("Location not found")

        location.name = request.name
        location.type = request.type
        location.parentId = request.parentId

        val parentPath = request.parentId?.let { parentId ->
            val parent = locationRepository.findByIdAndTenantId(parentId, ctx.tenantId)
                ?: throw EntityNotFoundException("Parent location not found")
            parent.path ?: "/${parent.id}"
        }
        location.path = buildPath(parentPath, id)

        val saved = locationRepository.save(location)
        return saved.toDto()
    }

    @Transactional
    fun deleteLocation(id: UUID) {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val location = locationRepository.findByIdAndTenantId(id, ctx.tenantId)
            ?: return

        // NOTE: in a real system we might prevent delete if children or assets exist.
        // For now, we just delete.
        locationRepository.delete(location)
    }

    @Transactional(readOnly = true)
    fun getLocationTree(): List<LocationTreeNodeDto> {
        val ctx = TenantContext.get() ?: error("No authenticated user in context")
        val all = locationRepository.findAllByTenantIdOrderByNameAsc(ctx.tenantId)

        val byParent = all.groupBy { it.parentId }
        fun build(parentId: UUID?): List<LocationTreeNodeDto> {
            val children = byParent[parentId].orEmpty()
            return children.map { loc ->
                LocationTreeNodeDto(
                    id = loc.id,
                    name = loc.name,
                    type = loc.type,
                    parentId = loc.parentId,
                    children = build(loc.id)
                )
            }
        }

        // root nodes = parentId == null
        return build(null)
    }

    private fun buildPath(parentPath: String?, idOverride: UUID? = null): String? {
        return if (parentPath == null) null
        else "$parentPath/${idOverride ?: ""}".trimEnd('/')
    }

    private fun Location.toDto(): LocationDto =
        LocationDto(
            id = id,
            tenantId = tenantId,
            name = name,
            type = type,
            parentId = parentId,
            path = path,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
