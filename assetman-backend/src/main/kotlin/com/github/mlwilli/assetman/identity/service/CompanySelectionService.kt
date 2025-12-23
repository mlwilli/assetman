package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.error.ForbiddenException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import com.github.mlwilli.assetman.common.security.requireCurrentUser
import com.github.mlwilli.assetman.identity.repo.CompanyMemberRepository
import com.github.mlwilli.assetman.identity.repo.CompanyRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.MyCompanyDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CompanySelectionService(
    private val companyRepository: CompanyRepository,
    private val companyMemberRepository: CompanyMemberRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional(readOnly = true)
    fun myCompanies(): List<MyCompanyDto> {
        val principal = requireCurrentUser()
        val memberships = companyMemberRepository.findAllByTenantIdAndUserId(principal.tenantId, principal.userId)

        if (memberships.isEmpty()) return emptyList()

        val companiesById = companyRepository.findAllById(memberships.map { it.companyId })
            .associateBy { it.id }

        return memberships
            .mapNotNull { m ->
                val c = companiesById[m.companyId] ?: return@mapNotNull null
                MyCompanyDto(
                    companyId = c.id,
                    name = c.name,
                    slug = c.slug,
                    active = c.active,
                    memberActive = m.active,
                    roles = m.roles.map { it.name }.sorted()
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    @Transactional(readOnly = true)
    fun selectCompany(companyId: UUID): String {
        val principal = requireCurrentUser()

        val company = companyRepository.findById(companyId)
            .orElseThrow { NotFoundException("Company not found") }

        if (company.tenantId != principal.tenantId) {
            throw ForbiddenException("Cross-tenant access denied")
        }
        if (!company.active) {
            throw ForbiddenException("Company is inactive")
        }

        val isMember = companyMemberRepository.existsByTenantIdAndCompanyIdAndUserId(
            principal.tenantId,
            companyId,
            principal.userId
        )
        if (!isMember) {
            throw ForbiddenException("User is not a member of this company")
        }

        val user = userRepository.findById(principal.userId)
            .orElseThrow { ForbiddenException("User not found") }
        if (!user.active) throw ForbiddenException("User account is disabled")

        val membership = companyMemberRepository.findAllByTenantIdAndUserId(principal.tenantId, principal.userId)
            .firstOrNull { it.companyId == companyId }
            ?: throw ForbiddenException("Membership not found")

        if (!membership.active) {
            throw ForbiddenException("Membership is inactive")
        }

        return jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = user.tenantId,
            email = user.email,
            roles = membership.roles.map { it.name }.toSet(),
            companyId = companyId
        )
    }
}
