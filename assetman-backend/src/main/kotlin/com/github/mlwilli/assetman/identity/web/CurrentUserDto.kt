package com.github.mlwilli.assetman.identity.web

import java.util.UUID

data class CurrentUserDto(
    val userId: UUID,
    val tenantId: UUID,
    val email: String,
    val fullName: String,
    val roles: List<String>,
    val companyId: UUID?,
    val companySelected: Boolean
)
