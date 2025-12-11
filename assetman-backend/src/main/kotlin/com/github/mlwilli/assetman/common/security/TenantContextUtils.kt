package com.github.mlwilli.assetman.common.security

import java.util.UUID

/**
 * Helper to require an authenticated user in the current TenantContext.
 * Throws IllegalStateException if called without an authenticated user.
 */
fun requireCurrentUser(): AuthenticatedUser =
    TenantContext.get()
        ?: throw IllegalStateException("No authenticated user in TenantContext")

fun currentTenantId(): UUID = requireCurrentUser().tenantId

fun currentUserId(): UUID = requireCurrentUser().userId
