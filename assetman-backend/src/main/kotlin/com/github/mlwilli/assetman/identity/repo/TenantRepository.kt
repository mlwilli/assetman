package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.Tenant
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface TenantRepository : JpaRepository<Tenant, UUID> {
    fun findBySlug(slug: String): Tenant?
}
