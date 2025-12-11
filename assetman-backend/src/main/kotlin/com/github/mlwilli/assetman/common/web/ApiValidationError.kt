package com.github.mlwilli.assetman.common.web


data class ApiValidationError(
    val field: String,
    val message: String,
    val rejectedValue: Any? = null
)
