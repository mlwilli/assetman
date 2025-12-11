package com.github.mlwilli.assetman.asset.service

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.web.AssetDto
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.asset.web.UpdateAssetRequest
import com.github.mlwilli.assetman.shared.security.TenantContext
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AssetService(
    private val assetRepository: AssetRepository
) {

    @Transactional(readOnly = true)
    fun listAssetsForCurrentTenant(
        status: AssetStatus?,
        search: String?,
        pageable: Pageable
    ): Page<AssetDto> {
        val current = TenantContext.get() ?: error("No authenticated user in context")
        val page = assetRepository.search(current.tenantId, status, search, pageable)
        return page.map { it.toDto() }
    }

    @Transactional(readOnly = true)
    fun getAsset(id: UUID): AssetDto {
        val current = TenantContext.get() ?: error("No authenticated user in context")
        val asset = assetRepository.findByIdAndTenantId(id, current.tenantId)
            ?: throw EntityNotFoundException("Asset not found")
        return asset.toDto()
    }

    @Transactional
    fun createAsset(request: CreateAssetRequest): AssetDto {
        val current = TenantContext.get() ?: error("No authenticated user in context")

        val asset = Asset(
            tenantId = current.tenantId,
            name = request.name,
            status = request.status ?: AssetStatus.PLANNED,
            category = request.category,
            serialNumber = request.serialNumber,
            tags = request.tags?.joinToString(","),
            purchaseDate = request.purchaseDate,
            purchaseCost = request.purchaseCost,
            locationId = request.locationId,
            assignedUserId = request.assignedUserId,
            warrantyExpiryDate = request.warrantyExpiryDate,
            customFieldsJson = request.customFieldsJson
        )

        val saved = assetRepository.save(asset)
        return saved.toDto()
    }

    @Transactional
    fun updateAsset(id: UUID, request: UpdateAssetRequest): AssetDto {
        val current = TenantContext.get() ?: error("No authenticated user in context")

        val asset = assetRepository.findByIdAndTenantId(id, current.tenantId)
            ?: throw EntityNotFoundException("Asset not found")

        asset.name = request.name
        asset.status = request.status ?: asset.status
        asset.category = request.category
        asset.serialNumber = request.serialNumber
        asset.tags = request.tags?.joinToString(",")
        asset.purchaseDate = request.purchaseDate
        asset.purchaseCost = request.purchaseCost
        asset.locationId = request.locationId
        asset.assignedUserId = request.assignedUserId
        asset.warrantyExpiryDate = request.warrantyExpiryDate
        asset.customFieldsJson = request.customFieldsJson

        val saved = assetRepository.save(asset)
        return saved.toDto()
    }

    @Transactional
    fun deleteAsset(id: UUID) {
        val current = TenantContext.get() ?: error("No authenticated user in context")
        val asset = assetRepository.findByIdAndTenantId(id, current.tenantId)
            ?: return
        assetRepository.delete(asset)
    }

    private fun Asset.toDto(): AssetDto =
        AssetDto(
            id = id,
            tenantId = tenantId,
            name = name,
            status = status,
            category = category,
            serialNumber = serialNumber,
            tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
            purchaseDate = purchaseDate,
            purchaseCost = purchaseCost,
            locationId = locationId,
            assignedUserId = assignedUserId,
            warrantyExpiryDate = warrantyExpiryDate,
            customFieldsJson = customFieldsJson,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
