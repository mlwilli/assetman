package com.github.mlwilli.assetman.shared.domain

import jakarta.persistence.Column
import java.util.UUID

/**
 * Marker interface for entities that belong to a tenant.
 */
interface TenantScoped {
    @get:Column(name = "tenant_id", nullable = false)
    val tenantId: UUID
}
