package com.github.mlwilli.assetman.testsupport

import java.time.Instant
import java.util.UUID

/**
 * Sets BaseEntity private fields (id/createdAt/updatedAt) by walking class hierarchy.
 * Works even if BaseEntity is not the direct superclass.
 */
fun setBaseEntityFields(entity: Any, id: UUID, now: Instant = Instant.now()) {
    var clazz: Class<*>? = entity.javaClass
    while (clazz != null) {
        try {
            val idField = clazz.getDeclaredField("id").apply { isAccessible = true }
            idField.set(entity, id)

            val createdField = clazz.getDeclaredField("createdAt").apply { isAccessible = true }
            createdField.set(entity, now)

            val updatedField = clazz.getDeclaredField("updatedAt").apply { isAccessible = true }
            updatedField.set(entity, now)

            return
        } catch (_: NoSuchFieldException) {
            clazz = clazz.superclass
        }
    }
    error("id / createdAt / updatedAt not found in class hierarchy of ${entity.javaClass}")
}
