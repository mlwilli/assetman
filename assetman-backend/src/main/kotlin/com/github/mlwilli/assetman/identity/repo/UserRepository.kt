package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {

    fun findByTenantIdAndEmailIgnoreCase(tenantId: UUID, email: String): User?

    fun findByTenantIdAndEmail(tenantId: UUID, email: String): User?

    fun findByEmailIgnoreCase(email: String): User?

    fun existsByEmailIgnoreCase(email: String): Boolean

    fun findByIdAndTenantId(id: UUID, tenantId: UUID): User?

    fun findAllByTenantId(tenantId: UUID, pageable: Pageable): Page<User>

    fun findAllByTenantIdAndActive(
        tenantId: UUID,
        active: Boolean,
        pageable: Pageable
    ): Page<User>
}
