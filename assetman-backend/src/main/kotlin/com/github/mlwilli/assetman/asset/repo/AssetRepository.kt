package com.github.mlwilli.assetman.asset.repo

import com.github.mlwilli.assetman.asset.domain.Asset
import com.github.mlwilli.assetman.asset.domain.AssetStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface AssetRepository : JpaRepository<Asset, UUID> {

    @Query(
        """
        SELECT a
        FROM Asset a
        WHERE a.tenantId = :tenantId
          AND (:status IS NULL OR a.status = :status)
          AND (
                :search IS NULL 
             OR lower(a.name) LIKE lower(concat('%', :search, '%'))
             OR lower(COALESCE(a.serialNumber, '')) LIKE lower(concat('%', :search, '%'))
          )
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("status") status: AssetStatus?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Asset>

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Asset?
}
