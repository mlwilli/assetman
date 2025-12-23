package com.github.mlwilli.assetman.common.security

import java.util.UUID

/**
 * Maintains the currently authenticated user for the request lifecycle.
 * Cleared automatically after each request by the auth filter.
 *
 * NOTE: Also supports scoped execution for dev seeding / tests.
 */
object TenantContext {
    private val holder = ThreadLocal<AuthenticatedUser?>()

    fun set(user: AuthenticatedUser?) = holder.set(user)
    fun get(): AuthenticatedUser? = holder.get()
    fun clear() = holder.remove()

    /**
     * Runs [block] with [user] in TenantContext, then restores the previous context.
     * Safe for nested calls.
     */
    fun <T> withUser(user: AuthenticatedUser, block: () -> T): T {
        val previous = holder.get()
        try {
            holder.set(user)
            return block()
        } finally {
            holder.set(previous)
        }
    }

    /**
     * Convenience for dev/test flows that need tenant/user context.
     */
    fun <T> withTenantUser(
        tenantId: UUID,
        userId: UUID,
        email: String,
        roles: Set<String>,
        block: () -> T
    ): T = withUser(
        AuthenticatedUser(
            userId = userId,
            tenantId = tenantId,
            email = email,
            roles = roles
        ),
        block
    )
}
