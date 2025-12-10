package com.github.mlwilli.assetman.identity.web

data class SignupTenantRequest(
    val tenantName: String,
    val tenantSlug: String,
    val adminName: String,
    val adminEmail: String,
    val adminPassword: String
)

data class LoginRequest(
    val tenantSlug: String,
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String
)
