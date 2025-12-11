package com.github.mlwilli.assetman.asset.service

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.web.AssetDto
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.asset.web.UpdateAssetRequest
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.currentTenantId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssetService(
    private val assetRepository: AssetRepository
) {

    fun createAsset(request: CreateAssetRequest): AssetDto {
        val tenantId = currentTenantId()

        val asset = Asset(
            tenantId = tenantId,
            name = request.name,
            status = request.status ?: AssetStatus.IN_SERVICE, // or enforce non-null via validation
            tags = request.tags?.joinToString(","),
            category = request.category,
            serialNumber = request.serialNumber,
            purchaseDate = request.purchaseDate,
            purchaseCost = request.purchaseCost,
            locationId = request.locationId,
            assignedUserId = request.assignedUserId,
            warrantyExpiryDate = request.warrantyExpiryDate,
            customFieldsJson = request.customFieldsJson
        )

        val saved = assetRepository.save(asset)
        return toDto(saved)
    }

    fun getAsset(id: UUID): AssetDto {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        return toDto(asset)
    }

    fun listAssetsForCurrentTenant(
        status: AssetStatus?,
        search: String?,
        pageable: Pageable
    ): Page<AssetDto> {
        val tenantId = currentTenantId()
        val page = assetRepository.search(tenantId, status, search, pageable)
        return page.map { asset -> toDto(asset) }
    }

    fun updateAsset(id: UUID, request: UpdateAssetRequest): AssetDto {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        asset.name = request.name
        asset.status = request.status!! // validated as @NotNull
        asset.category = request.category
        asset.serialNumber = request.serialNumber
        asset.tags = request.tags?.joinToString(",")
        asset.purchaseDate = request.purchaseDate
        asset.purchaseCost = request.purchaseCost
        asset.locationId = request.locationId
        asset.assignedUserId = request.assignedUserId
        asset.warrantyExpiryDate = request.warrantyExpiryDate
        asset.customFieldsJson = request.customFieldsJson

        val updated = assetRepository.save(asset)
        return toDto(updated)
    }

    fun deleteAsset(id: UUID) {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        assetRepository.delete(asset)
    }

    // --- mapping ---

    private fun toDto(asset: Asset): AssetDto =
        AssetDto(
            id = asset.id,
            tenantId = asset.tenantId,
            name = asset.name,
            status = asset.status,
            category = asset.category,
            serialNumber = asset.serialNumber,
            // entity.tags: String? -> DTO: List<String>
            tags = asset.tags
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList(),
            purchaseDate = asset.purchaseDate,
            purchaseCost = asset.purchaseCost,
            locationId = asset.locationId,
            assignedUserId = asset.assignedUserId,
            warrantyExpiryDate = asset.warrantyExpiryDate,
            customFieldsJson = asset.customFieldsJson,
            createdAt = asset.createdAt,
            updatedAt = asset.updatedAt
        )
}
