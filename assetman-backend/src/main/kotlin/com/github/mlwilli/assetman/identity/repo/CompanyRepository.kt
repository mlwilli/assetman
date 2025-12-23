package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.Company
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CompanyRepository : JpaRepository<Company, UUID> {
    fun findByTenantId(tenantId: UUID): List<Company>
    fun findByTenantIdAndSlug(tenantId: UUID, slug: String): Company?
}
