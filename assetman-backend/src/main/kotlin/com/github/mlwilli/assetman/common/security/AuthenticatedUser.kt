package com.github.mlwilli.assetman.common.security

import java.util.UUID

/**
 * Represents the authenticated user and the tenant they belong to.
 */

data class AuthenticatedUser(
    val userId: UUID,
    val tenantId: UUID,
    val email: String,
    val roles: Set<String>,
    val companyId: UUID? = null
)
