package com.github.mlwilli.assetman.testsupport

import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import java.time.Instant
import java.util.UUID

fun <T> pageOf(vararg items: T): Page<T> = PageImpl(items.toList())

inline fun <T> withTenantContext(
    userId: UUID,
    tenantId: UUID,
    email: String = "tester@tenant.test",
    roles: Set<String> = setOf("ADMIN"),
    block: () -> T
): T {
    TenantContext.set(
        AuthenticatedUser(
            userId = userId,
            tenantId = tenantId,
            email = email,
            roles = roles
        )
    )
    try {
        return block()
    } finally {
        TenantContext.clear()
    }
}

/**
 * Sets BaseEntity fields (id/createdAt/updatedAt) via reflection.
 * Works even if BaseEntity is not the direct superclass.
 */
fun setBaseEntityFields(entity: Any, id: UUID) {
    var clazz: Class<*>? = entity.javaClass
    while (clazz != null) {
        try {
            val idField = clazz.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(entity, id)

            val createdField = clazz.getDeclaredField("createdAt")
            createdField.isAccessible = true
            createdField.set(entity, Instant.now())

            val updatedField = clazz.getDeclaredField("updatedAt")
            updatedField.isAccessible = true
            updatedField.set(entity, Instant.now())

            return
        } catch (_: NoSuchFieldException) {
            // keep walking up
        }
        clazz = clazz.superclass
    }
    error("id/createdAt/updatedAt not found in class hierarchy of ${entity.javaClass}")
}
