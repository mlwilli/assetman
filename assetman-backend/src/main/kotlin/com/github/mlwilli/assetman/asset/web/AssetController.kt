package com.github.mlwilli.assetman.asset.web

import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.service.AssetService
import com.github.mlwilli.assetman.common.web.PageResponse
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/assets")
class AssetController(
    private val assetService: AssetService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun listAssets(
        @RequestParam(required = false) status: AssetStatus?,
        @RequestParam(required = false) category: String?,
        /**
         * Filter by a location and all of its descendants in the location tree.
         */
        @RequestParam(required = false) locationId: UUID?,
        @RequestParam(required = false) propertyId: UUID?,
        @RequestParam(required = false) unitId: UUID?,
        @RequestParam(required = false) assignedUserId: UUID?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<AssetDto> {
        val pageable = PageRequest.of(page, size)
        val result = assetService.listAssetsForCurrentTenant(
            status = status,
            category = category,
            locationId = locationId,
            propertyId = propertyId,
            unitId = unitId,
            assignedUserId = assignedUserId,
            search = search,
            pageable = pageable
        )
        return PageResponse(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun getAsset(@PathVariable id: UUID): AssetDto =
        assetService.getAsset(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun createAsset(
        @Valid @RequestBody request: CreateAssetRequest
    ): ResponseEntity<AssetDto> {
        val created = assetService.createAsset(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun updateAsset(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAssetRequest
    ): ResponseEntity<AssetDto> {
        val updated = assetService.updateAsset(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun deleteAsset(@PathVariable id: UUID): ResponseEntity<Void> {
        assetService.deleteAsset(id)
        return ResponseEntity.noContent().build()
    }
}
