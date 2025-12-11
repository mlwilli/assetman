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

    @Query(
        """
        SELECT l 
        FROM Location l
        WHERE l.tenantId = :tenantId
          AND (:type IS NULL OR l.type = :type)
          AND (:parentId IS NULL OR l.parentId = :parentId)
          AND (:search IS NULL OR lower(l.name) LIKE lower(concat('%', :search, '%')))
        ORDER BY l.name ASC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("type") type: LocationType?,
        @Param("parentId") parentId: UUID?,
        @Param("search") search: String?
    ): List<Location>
}
