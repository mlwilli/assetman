package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/signup-tenant")
    fun signupTenant(@RequestBody request: SignupTenantRequest): ResponseEntity<AuthResponse> {
        val tokens = authService.signupTenant(request)
        return ResponseEntity.ok(tokens)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val tokens = authService.login(request)
        return ResponseEntity.ok(tokens)
    }
}
