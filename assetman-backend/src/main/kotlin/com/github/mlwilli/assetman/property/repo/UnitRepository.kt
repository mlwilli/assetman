package com.github.mlwilli.assetman.property.repo

import com.github.mlwilli.assetman.property.domain.Unit
import com.github.mlwilli.assetman.property.domain.UnitStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UnitRepository : JpaRepository<Unit, UUID> {

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Unit?

    fun findAllByTenantIdAndPropertyIdOrderByNameAsc(
        tenantId: UUID,
        propertyId: UUID
    ): List<Unit>

    @Query(
        """
        SELECT u
        FROM Unit u
        WHERE u.tenantId = :tenantId
          AND (:propertyId IS NULL OR u.propertyId = :propertyId)
          AND (:status IS NULL OR u.status = :status)
          AND (:search IS NULL OR lower(u.name) LIKE lower(concat('%', :search, '%')))
        ORDER BY u.name ASC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("propertyId") propertyId: UUID?,
        @Param("status") status: UnitStatus?,
        @Param("search") search: String?
    ): List<Unit>

    fun existsByTenantIdAndPropertyId(tenantId: UUID, propertyId: UUID): Boolean
}
