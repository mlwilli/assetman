package com.github.mlwilli.assetman.asset.service

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.repo.AssetRepository
import com.github.mlwilli.assetman.asset.web.AssetDto
import com.github.mlwilli.assetman.asset.web.CreateAssetRequest
import com.github.mlwilli.assetman.asset.web.UpdateAssetRequest
import com.github.mlwilli.assetman.asset.web.toDto
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.location.repo.LocationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AssetService(
    private val assetRepository: AssetRepository,
    private val locationRepository: LocationRepository
) {

    @Transactional
    fun createAsset(request: CreateAssetRequest): AssetDto {
        val tenantId = currentTenantId()

        val asset = Asset(
            tenantId = tenantId,
            name = request.name,
            status = request.status,
            category = request.category,
            code = request.code,
            assetTag = request.assetTag,
            manufacturer = request.manufacturer,
            model = request.model,
            serialNumber = request.serialNumber,
            tags = request.tags?.joinToString(","),
            purchaseDate = request.purchaseDate,
            purchaseCost = request.purchaseCost,
            inServiceDate = request.inServiceDate,
            retiredDate = request.retiredDate,
            disposedDate = request.disposedDate,
            warrantyExpiryDate = request.warrantyExpiryDate,
            depreciationYears = request.depreciationYears,
            residualValue = request.residualValue,
            locationId = request.locationId,
            propertyId = request.propertyId,
            unitId = request.unitId,
            assignedUserId = request.assignedUserId,
            externalRef = request.externalRef,
            customFieldsJson = request.customFieldsJson
        )

        val saved = assetRepository.save(asset)
        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun getAsset(id: UUID): AssetDto {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        return asset.toDto()
    }

    @Transactional(readOnly = true)
    fun listAssetsForCurrentTenant(
        status: AssetStatus?,
        category: String?,
        locationId: UUID?,
        propertyId: UUID?,
        unitId: UUID?,
        assignedUserId: UUID?,
        search: String?,
        pageable: Pageable
    ): Page<AssetDto> {
        val tenantId = currentTenantId()

        // Compute subtree IDs when a location filter is provided
        val locationIds: List<UUID>? =
            if (locationId == null) {
                null
            } else {
                val root = locationRepository.findByIdAndTenantId(locationId, tenantId)
                    ?: throw NotFoundException("Location not found")

                val path = root.path ?: "/${root.id}"
                val pathPrefix = path.trimEnd('/') + "/"

                val subtree = locationRepository.findAllByTenantIdAndPathStartingWith(
                    tenantId = tenantId,
                    pathPrefix = pathPrefix
                )

                // include root itself
                (subtree.map { it.id } + root.id).distinct()
            }

        val page = assetRepository.search(
            tenantId = tenantId,
            status = status,
            category = category,
            locationIds = locationIds,
            propertyId = propertyId,
            unitId = unitId,
            assignedUserId = assignedUserId,
            search = search,
            pageable = pageable
        )

        return page.map { it.toDto() }
    }

    @Transactional
    fun updateAsset(id: UUID, request: UpdateAssetRequest): AssetDto {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        asset.name = request.name
        asset.status = request.status
        asset.category = request.category
        asset.code = request.code
        asset.assetTag = request.assetTag
        asset.manufacturer = request.manufacturer
        asset.model = request.model
        asset.serialNumber = request.serialNumber
        asset.tags = request.tags?.joinToString(",")
        asset.purchaseDate = request.purchaseDate
        asset.purchaseCost = request.purchaseCost
        asset.inServiceDate = request.inServiceDate
        asset.retiredDate = request.retiredDate
        asset.disposedDate = request.disposedDate
        asset.warrantyExpiryDate = request.warrantyExpiryDate
        asset.depreciationYears = request.depreciationYears
        asset.residualValue = request.residualValue
        asset.locationId = request.locationId
        asset.propertyId = request.propertyId
        asset.unitId = request.unitId
        asset.assignedUserId = request.assignedUserId
        asset.externalRef = request.externalRef
        asset.customFieldsJson = request.customFieldsJson

        val updated = assetRepository.save(asset)
        return updated.toDto()
    }

    @Transactional
    fun deleteAsset(id: UUID) {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByIdAndTenantId(id, tenantId)
            ?: throw NotFoundException("Asset not found")

        assetRepository.delete(asset)
    }

    @Transactional(readOnly = true)
    fun getAssetByExternalRef(externalRef: String): AssetDto {
        val tenantId = currentTenantId()

        val asset = assetRepository.findByTenantIdAndExternalRef(tenantId, externalRef)
            ?: throw NotFoundException("Asset not found for externalRef: $externalRef")

        return asset.toDto()
    }

    @Transactional(readOnly = true)
    fun listAssetsByStatusAndLocation(
        status: AssetStatus,
        locationId: UUID,
        pageable: Pageable
    ): Page<AssetDto> {
        return listAssetsForCurrentTenant(
            status = status,
            category = null,
            locationId = locationId,
            propertyId = null,
            unitId = null,
            assignedUserId = null,
            search = null,
            pageable = pageable
        )
    }

}
