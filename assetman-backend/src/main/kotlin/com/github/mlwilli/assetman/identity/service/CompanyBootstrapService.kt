package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.Company
import com.github.mlwilli.assetman.identity.domain.CompanyMember
import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.CompanyMemberRepository
import com.github.mlwilli.assetman.identity.repo.CompanyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CompanyBootstrapService(
    private val companyRepository: CompanyRepository,
    private val companyMemberRepository: CompanyMemberRepository
) {

    @Transactional
    fun bootstrapDefaultCompany(
        tenant: Tenant,
        ownerUser: User
    ): BootstrapResult {
        require(tenant.id == ownerUser.tenantId) { "User tenant mismatch" }

        val baseSlug = tenant.slug.trim().lowercase()
        val name = tenant.name.trim()

        val slug = nextAvailableSlug(tenantId = tenant.id, baseSlug = baseSlug)

        val company = companyRepository.save(
            Company(
                tenantId = tenant.id,
                name = name,
                slug = slug,
                active = true
            )
        )

        val membership = CompanyMember(
            tenantId = tenant.id,
            companyId = company.id,
            userId = ownerUser.id,
            active = true
        )
        membership.setRoles(setOf(Role.OWNER, Role.ADMIN))
        companyMemberRepository.save(membership)

        return BootstrapResult(companyId = company.id, companySlug = company.slug)
    }

    private fun nextAvailableSlug(tenantId: UUID, baseSlug: String): String {
        var slug = baseSlug
        var i = 0
        while (companyRepository.findByTenantIdAndSlug(tenantId, slug) != null) {
            i += 1
            slug = "$baseSlug-$i"
        }
        return slug
    }

    data class BootstrapResult(
        val companyId: UUID,
        val companySlug: String
    )
}
