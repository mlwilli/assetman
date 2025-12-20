package com.github.mlwilli.assetman.identity.web3

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupTenantRequest(
    @field:NotBlank(message = "Tenant name is required")
    val tenantName: String,

    @field:NotBlank(message = "Tenant slug is required")
    @field:Pattern(
        regexp = "^[a-z0-9-]{3,50}$",
        message = "Tenant slug must be 3-50 characters, lowercase letters, digits, or hyphens"
    )
    val tenantSlug: String,

    @field:NotBlank(message = "Admin name is required")
    val adminName: String,

    @field:NotBlank(message = "Admin email is required")
    @field:Email(message = "Admin email must be a valid email address")
    val adminEmail: String,

    @field:NotBlank(message = "Admin password is required")
    @field:Size(min = 8, max = 72, message = "Admin password must be between 8 and 72 characters")
    val adminPassword: String
)

data class LoginRequest(
    @field:NotBlank(message = "Tenant slug is required")
    val tenantSlug: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String
)

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
    val newPassword: String
)

data class ForgotPasswordRequest(
    @field:NotBlank(message = "Tenant slug is required")
    val tenantSlug: String,

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String
)

data class ResetPasswordRequest(
    @field:NotBlank(message = "Reset token is required")
    val token: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
    val newPassword: String
)
