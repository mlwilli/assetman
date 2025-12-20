package com.github.mlwilli.assetman.identity.web

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be a valid email address")
    val email: String,

    // Optional, so no @NotBlank - but we can still bound the length if present.
    @field:Size(max = 255, message = "Display name must be at most 255 characters")
    val displayName: String?,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    val password: String,

    @field:NotEmpty(message = "At least one role is required")
    val roles: List<@NotBlank(message = "Role must not be blank") String>
)

data class UpdateUserRolesRequest(
    @field:NotEmpty(message = "At least one role is required")
    val roles: List<@NotBlank(message = "Role must not be blank") String>
)

data class UpdateUserStatusRequest(
    // Boolean is required by the JSON body, so no extra validation needed.
    val active: Boolean
)
