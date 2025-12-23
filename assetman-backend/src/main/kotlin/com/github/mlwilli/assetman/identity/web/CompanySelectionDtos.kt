package com.github.mlwilli.assetman.identity.web

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class MyCompanyDto(
    val companyId: UUID,
    val name: String,
    val slug: String,
    val active: Boolean,
    val memberActive: Boolean,
    val roles: List<String>
)

data class SelectCompanyRequest(
    @field:NotBlank(message = "companyId is required")
    val companyId: String
)

data class SelectCompanyResponse(
    val accessToken: String
)
