package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    // -------- Public endpoints --------

    @PostMapping("/signup-tenant")
    fun signupTenant(@Valid @RequestBody request: SignupTenantRequest): ResponseEntity<AuthDto> =
        ResponseEntity.ok(authService.signupTenant(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthDto> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthDto> =
        ResponseEntity.ok(authService.refreshTokens(request))

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        authService.resetPassword(request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        authService.logout(request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequest): ResponseEntity<Void> {
        authService.changePassword(request)
        return ResponseEntity.noContent().build()
    }

}
