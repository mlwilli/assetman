package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.CompanyMember
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CompanyMemberRepository : JpaRepository<CompanyMember, UUID> {
    fun findAllByTenantIdAndCompanyId(tenantId: UUID, companyId: UUID): List<CompanyMember>

    fun findAllByTenantIdAndUserId(tenantId: UUID, userId: UUID): List<CompanyMember>

    fun existsByTenantIdAndCompanyIdAndUserId(tenantId: UUID, companyId: UUID, userId: UUID): Boolean
}
