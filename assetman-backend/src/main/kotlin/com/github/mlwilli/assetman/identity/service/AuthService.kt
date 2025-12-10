package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.AuthResponse
import com.github.mlwilli.assetman.identity.web.LoginRequest
import com.github.mlwilli.assetman.identity.web.SignupTenantRequest
import com.github.mlwilli.assetman.shared.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun signupTenant(request: SignupTenantRequest): AuthResponse {
        // Create tenant
        val tenant = tenantRepository.save(
            Tenant(
                name = request.tenantName,
                slug = request.tenantSlug.lowercase()
            )
        )

        // Create owner user
        val user = userRepository.save(
            User(
                tenantId = tenant.id,
                fullName = request.adminName,
                email = request.adminEmail.lowercase(),
                passwordHash = passwordEncoder.encode(request.adminPassword),
                roles = mutableSetOf(Role.OWNER, Role.ADMIN)
            )
        )

        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = tenant.id,
            email = user.email,
            roles = user.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthResponse(accessToken, refreshToken)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val tenant = tenantRepository.findBySlug(request.tenantSlug.lowercase())
            ?: throw IllegalArgumentException("Tenant not found")

        val user = userRepository.findByEmailAndTenantId(request.email.lowercase(), tenant.id)
            ?: throw IllegalArgumentException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid credentials")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = tenant.id,
            email = user.email,
            roles = user.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthResponse(accessToken, refreshToken)
    }
}
