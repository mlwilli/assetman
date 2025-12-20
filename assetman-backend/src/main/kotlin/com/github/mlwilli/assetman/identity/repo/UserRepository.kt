package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

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

    @Query(
        """
    select u from User u
    where u.tenantId = :tenantId
      and (:activeOnly = false or u.active = true)
      and (
        lower(u.email) like lower(concat('%', :q, '%'))
        or lower(u.fullName) like lower(concat('%', :q, '%'))
        or lower(coalesce(u.displayName, '')) like lower(concat('%', :q, '%'))
      )
    """
    )
    fun searchDirectory(
        @Param("tenantId") tenantId: UUID,
        @Param("q") q: String,
        @Param("activeOnly") activeOnly: Boolean,
        pageable: Pageable
    ): Page<User>

}
