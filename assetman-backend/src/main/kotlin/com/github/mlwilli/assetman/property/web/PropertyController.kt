package com.github.mlwilli.assetman.property.web

import com.github.mlwilli.assetman.property.domain.PropertyType
import com.github.mlwilli.assetman.property.service.PropertyService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/properties")
class PropertyController(
    private val propertyService: PropertyService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun listProperties(
        @RequestParam(required = false) type: PropertyType?,
        @RequestParam(required = false) search: String?
    ): List<PropertyDto> =
        propertyService.listProperties(type, search)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun getProperty(@PathVariable id: UUID): PropertyDto =
        propertyService.getProperty(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun createProperty(
        @RequestBody request: CreatePropertyRequest
    ): ResponseEntity<PropertyDto> {
        val created = propertyService.createProperty(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun updateProperty(
        @PathVariable id: UUID,
        @RequestBody request: UpdatePropertyRequest
    ): ResponseEntity<PropertyDto> {
        val updated = propertyService.updateProperty(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun deleteProperty(@PathVariable id: UUID): ResponseEntity<Void> {
        propertyService.deleteProperty(id)
        return ResponseEntity.noContent().build()
    }
}
