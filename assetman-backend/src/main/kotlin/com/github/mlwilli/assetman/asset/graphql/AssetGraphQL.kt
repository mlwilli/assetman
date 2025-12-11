package com.github.mlwilli.assetman.asset.graphql

import com.github.mlwilli.assetman.asset.domain.AssetStatus
import com.github.mlwilli.assetman.asset.service.AssetService
import com.github.mlwilli.assetman.asset.web.AssetDto
import com.github.mlwilli.assetman.common.web.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
class AssetGraphQL(
    private val assetService: AssetService
) {

    /**
     * Query: assets(status, search, page, size): AssetPage!
     *
     * Uses AssetService.listAssetsForCurrentTenant with simple filters.
     */
    @QueryMapping
    fun assets(
        @Argument status: AssetStatus?,
        @Argument search: String?,
        @Argument page: Int = 0,
        @Argument size: Int = 20
    ): PageResponse<AssetDto> {
        val pageable = PageRequest.of(page, size)

        val resultPage = assetService.listAssetsForCurrentTenant(
            status = status,
            category = null,
            locationId = null,
            propertyId = null,
            unitId = null,
            assignedUserId = null,
            search = search,
            pageable = pageable
        )

        return PageResponse.from(resultPage)
    }

    /**
     * Query: asset(id: ID!): Asset
     */
    @QueryMapping
    fun asset(@Argument id: UUID): AssetDto =
        assetService.getAsset(id)

    /**
     * Query: assetByExternalRef(externalRef: String!): Asset
     *
     * Tenant-scoped lookup by external reference.
     */
    @QueryMapping
    fun assetByExternalRef(
        @Argument externalRef: String
    ): AssetDto =
        assetService.getAssetByExternalRef(externalRef)

    /**
     * Query: assetsByStatusAndLocation(
     *   status: AssetStatus!,
     *   locationId: ID!,
     *   page: Int = 0,
     *   size: Int = 20
     * ): AssetPage!
     *
     * Uses the same location subtree behavior as REST:
     * - AssetService resolves the location path and filters by subtree.
     */
    @QueryMapping
    fun assetsByStatusAndLocation(
        @Argument status: AssetStatus,
        @Argument locationId: UUID,
        @Argument page: Int = 0,
        @Argument size: Int = 20
    ): PageResponse<AssetDto> {
        val pageable = PageRequest.of(page, size)

        val resultPage = assetService.listAssetsForCurrentTenant(
            status = status,
            category = null,
            locationId = locationId,
            propertyId = null,
            unitId = null,
            assignedUserId = null,
            search = null,
            pageable = pageable
        )

        return PageResponse.from(resultPage)
    }
}
