package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmailAndTenantId(email: String, tenantId: UUID): User?
}
