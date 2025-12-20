package com.github.mlwilli.assetman.identity.web

import java.util.UUID

data class UserDirectoryDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val displayName: String?,
    val active: Boolean
) {
    val label: String
        get() = displayName?.takeIf { it.isNotBlank() } ?: fullName
}
