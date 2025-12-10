package com.github.mlwilli.assetman.shared.security

/**
 * Maintains the currently authenticated user for the request lifecycle.
 * Cleared automatically after each request by the auth filter.
 */
object TenantContext {
    private val holder = ThreadLocal<AuthenticatedUser?>()

    fun set(user: AuthenticatedUser?) = holder.set(user)
    fun get(): AuthenticatedUser? = holder.get()
    fun clear() = holder.remove()
}
