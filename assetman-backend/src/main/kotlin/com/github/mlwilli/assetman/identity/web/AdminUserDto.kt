package com.github.mlwilli.assetman.identity.web

import java.util.*

data class AdminUserDto(
    val id: UUID,
    val email: String,
    val displayName: String?,
    val roles: List<String>,
    val active: Boolean
)
