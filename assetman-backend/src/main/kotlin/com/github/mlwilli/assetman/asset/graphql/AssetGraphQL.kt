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

    @QueryMapping
    fun assets(
        @Argument status: AssetStatus?,
        @Argument search: String?,
        @Argument page: Int,
        @Argument size: Int
    ): PageResponse<AssetDto> {
        val pageable = PageRequest.of(page, size)
        val resultPage = assetService.listAssetsForCurrentTenant(status, search, pageable)
        return PageResponse.Companion.from(resultPage)
    }

    @QueryMapping
    fun asset(@Argument id: UUID): AssetDto =
        assetService.getAsset(id)
}