package com.github.mlwilli.assetman.common.web

import java.time.Instant

data class ApiErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String?,
    val path: String?,
    val code: String? = null,
    val validationErrors: List<ApiValidationError>? = null
)
