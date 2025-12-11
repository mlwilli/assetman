package com.github.mlwilli.assetman.property.repo

import com.github.mlwilli.assetman.property.domain.Property
import com.github.mlwilli.assetman.property.domain.PropertyType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface PropertyRepository : JpaRepository<Property, UUID> {

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Property?

    @Query(
        """
        SELECT p
        FROM Property p
        WHERE p.tenantId = :tenantId
          AND (:type IS NULL OR p.type = :type)
          AND (:search IS NULL OR lower(p.name) LIKE lower(concat('%', :search, '%')))
        ORDER BY p.name ASC
        """
    )
    fun search(
        @Param("tenantId") tenantId: UUID,
        @Param("type") type: PropertyType?,
        @Param("search") search: String?
    ): List<Property>
}
