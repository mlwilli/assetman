package com.github.mlwilli.assetman.property.web

import com.github.mlwilli.assetman.property.domain.UnitStatus
import com.github.mlwilli.assetman.property.service.UnitService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/units")
class UnitController(
    private val unitService: UnitService
) {

    @GetMapping
    fun listUnits(
        @RequestParam(required = false) propertyId: UUID?,
        @RequestParam(required = false) status: UnitStatus?,
        @RequestParam(required = false) search: String?
    ): List<UnitDto> =
        unitService.listUnits(propertyId, status, search)

    @GetMapping("/{id}")
    fun getUnit(@PathVariable id: UUID): UnitDto =
        unitService.getUnit(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun createUnit(
        @RequestBody request: CreateUnitRequest
    ): ResponseEntity<UnitDto> {
        val created = unitService.createUnit(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun updateUnit(
        @PathVariable id: UUID,
        @RequestBody request: UpdateUnitRequest
    ): ResponseEntity<UnitDto> {
        val updated = unitService.updateUnit(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun deleteUnit(@PathVariable id: UUID): ResponseEntity<Void> {
        unitService.deleteUnit(id)
        return ResponseEntity.noContent().build()
    }
}
