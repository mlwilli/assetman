package com.github.mlwilli.assetman.asset.web

import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.service.AssetService
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
    fun listAssets(
        @RequestParam(required = false) status: AssetStatus?,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): PageResponse<AssetDto> {
        val pageable = PageRequest.of(page, size)
        val result = assetService.listAssetsForCurrentTenant(status, search, pageable)
        return PageResponse(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @GetMapping("/{id}")
    fun getAsset(@PathVariable id: UUID): AssetDto =
        assetService.getAsset(id)

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun createAsset(@RequestBody request: CreateAssetRequest): ResponseEntity<AssetDto> {
        val created = assetService.createAsset(request)
        return ResponseEntity.ok(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER')")
    fun updateAsset(
        @PathVariable id: UUID,
        @RequestBody request: UpdateAssetRequest
    ): ResponseEntity<AssetDto> {
        val updated = assetService.updateAsset(id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    fun deleteAsset(@PathVariable id: UUID): ResponseEntity<Void> {
        assetService.deleteAsset(id)
        return ResponseEntity.noContent().build()
    }
}
