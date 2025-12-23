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

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Asset?

    // Tenant-scoped lookup by externalRef (assumes uniqueness per tenant)
    fun findByTenantIdAndExternalRef(tenantId: UUID, externalRef: String): Asset?
    fun countByTenantId(tenantId: UUID): Long

    @Query(
        """
        SELECT a
        FROM Asset a
        WHERE a.tenantId = :tenantId
          AND (:status IS NULL OR a.status = :status)
          AND (:category IS NULL OR a.category = :category)
          AND (
                :locationIds IS NULL
             OR a.locationId IN :locationIds
          )
          AND (:propertyId IS NULL OR a.propertyId = :propertyId)
          AND (:unitId IS NULL OR a.unitId = :unitId)
          AND (:assignedUserId IS NULL OR a.assignedUserId = :assignedUserId)
          AND (
                :search IS NULL 
             OR lower(a.name) LIKE lower(CONCAT('%', :search, '%'))
             OR lower(COALESCE(a.serialNumber, '')) LIKE lower(CONCAT('%', :search, '%'))
             OR lower(COALESCE(a.assetTag, '')) LIKE lower(CONCAT('%', :search, '%'))
             OR lower(COALESCE(a.code, '')) LIKE lower(CONCAT('%', :search, '%'))
          )
        ORDER BY a.createdAt DESC, a.name ASC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("status") status: AssetStatus?,
        @Param("category") category: String?,
        @Param("locationIds") locationIds: List<UUID>?,   // subtree, handled by service
        @Param("propertyId") propertyId: UUID?,
        @Param("unitId") unitId: UUID?,
        @Param("assignedUserId") assignedUserId: UUID?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<Asset>
}
