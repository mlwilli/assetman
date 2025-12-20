package com.github.mlwilli.assetman.location.repo

import com.github.mlwilli.assetman.location.domain.Location
import com.github.mlwilli.assetman.location.domain.LocationType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface LocationRepository : JpaRepository<Location, UUID> {

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Location?

    fun findAllByTenantIdOrderByNameAsc(tenantId: UUID): List<Location>

    fun findAllByTenantIdAndParentIdOrderByNameAsc(tenantId: UUID, parentId: UUID?): List<Location>

    fun existsByTenantIdAndParentId(tenantId: UUID, parentId: UUID): Boolean

    fun findAllByTenantIdAndActiveTrueOrderByNameAsc(tenantId: UUID): List<Location>

    fun countByTenantId(tenantId: UUID): Long

    @Query(
        """
        SELECT l 
        FROM Location l
        WHERE l.tenantId = :tenantId
          AND (:type IS NULL OR l.type = :type)
          AND (:parentId IS NULL OR l.parentId = :parentId)
          AND (:active IS NULL OR l.active = :active)
          AND (:search IS NULL OR lower(l.name) LIKE lower(concat('%', :search, '%')))
        ORDER BY 
            COALESCE(l.sortOrder, 999999) ASC,
            l.name ASC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("type") type: LocationType?,
        @Param("parentId") parentId: UUID?,
        @Param("active") active: Boolean?,
        @Param("search") search: String?
    ): List<Location>

    // NEW: generic subtree fetch for path-based hierarchy
    @Query(
        """
        SELECT l
        FROM Location l
        WHERE l.tenantId = :tenantId
          AND l.path IS NOT NULL
          AND l.path LIKE CONCAT(:pathPrefix, '%')
        """
    )
    fun findAllByTenantIdAndPathStartingWith(
        @Param("tenantId") tenantId: UUID,
        @Param("pathPrefix") pathPrefix: String
    ): List<Location>
}
