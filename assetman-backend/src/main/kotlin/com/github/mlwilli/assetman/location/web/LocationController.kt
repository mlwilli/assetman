package com.github.mlwilli.assetman.location.web

import com.github.mlwilli.assetman.location.domain.LocationType
import com.github.mlwilli.assetman.location.service.LocationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/locations")
class LocationController(
    private val locationService: LocationService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun listLocations(
        @RequestParam(required = false) type: LocationType?,
        @RequestParam(required = false) parentId: UUID?,
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) search: String?
    ): List<LocationDto> =
        locationService.listLocations(
            type = type,
            parentId = parentId,
            active = active,
            search = search
        )

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun getLocationTree(): List<LocationTreeNodeDto> =
        locationService.getLocationTree()

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun getLocation(@PathVariable id: UUID): LocationDto =
        locationService.getLocation(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun createLocation(
        @RequestBody request: CreateLocationRequest
    ): ResponseEntity<LocationDto> {
        val created = locationService.createLocation(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun updateLocation(
        @PathVariable id: UUID,
        @RequestBody request: UpdateLocationRequest
    ): ResponseEntity<LocationDto> {
        val updated = locationService.updateLocation(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun deleteLocation(@PathVariable id: UUID): ResponseEntity<Void> {
        locationService.deleteLocation(id)
        return ResponseEntity.noContent().build()
    }
}
