package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.PasswordResetToken
import com.github.mlwilli.assetman.identity.domain.RevokedToken
import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.PasswordResetTokenRepository
import com.github.mlwilli.assetman.identity.repo.RevokedTokenRepository
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.*
import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import com.github.mlwilli.assetman.common.security.TenantContext
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
    private val tenantRepository: TenantRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val revokedTokenRepository: RevokedTokenRepository
) {

    private val log = LoggerFactory.getLogger(AuthService::class.java)

    // ==========
    // Sign-up & Login
    // ==========

    @Transactional
    fun signupTenant(request: SignupTenantRequest): AuthDto {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val adminEmail = request.adminEmail.trim().lowercase()

        // Create tenant
        val tenant = tenantRepository.save(
            Tenant(
                name = request.tenantName.trim(),
                slug = tenantSlug
            )
        )

        // Create owner/admin user
        val user = userRepository.save(
            User(
                tenantId = tenant.id,
                fullName = request.adminName.trim(),
                email = adminEmail,
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

        return AuthDto(accessToken, refreshToken)
    }

    /**
     * Tenant-aware login: requires tenantSlug + email + password.
     */
    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthDto {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val email = request.email.trim().lowercase()

        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw BadCredentialsException("Invalid credentials")

        val user = userRepository.findByEmailAndTenantId(email, tenant.id)
            ?: throw BadCredentialsException("Invalid credentials")

        if (!user.active) {
            throw BadCredentialsException("User account is disabled")
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadCredentialsException("Invalid credentials")
        }

        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = tenant.id,
            email = user.email,
            roles = user.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthDto(accessToken, refreshToken)
    }

    /**
     * Refresh tokens using a valid, non-revoked refresh token.
     */
    @Transactional(readOnly = true)
    fun refreshTokens(request: RefreshTokenRequest): AuthDto {
        // Check blacklist first
        if (revokedTokenRepository.existsById(request.refreshToken)) {
            throw BadCredentialsException("Refresh token has been revoked")
        }

        val userId: UUID = jwtTokenProvider.parseRefreshToken(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { BadCredentialsException("User not found") }

        if (!user.active) {
            throw BadCredentialsException("User account is disabled")
        }

        val tenantId = user.tenantId

        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = tenantId,
            email = user.email,
            roles = user.roles.map { it.name }.toSet()
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)

        return AuthDto(accessToken, refreshToken)
    }

    // ==========
    // Current user
    // ==========

    @Transactional(readOnly = true)
    fun currentUser(): CurrentUserDto {
        val principal = TenantContext.get() ?: throw BadCredentialsException("Not authenticated")

        val user = userRepository.findById(principal.userId)
            .orElseThrow { IllegalStateException("User not found") }

        return CurrentUserDto(
            userId = user.id,
            tenantId = user.tenantId,
            email = user.email,
            fullName = user.fullName,
            roles = user.roles.map { it.name }.sorted()
        )
    }

    // ==========
    // Logout with refresh-token revocation
    // ==========

    @Transactional
    fun logout(request: RefreshTokenRequest) {
        try {
            val userId = jwtTokenProvider.parseRefreshToken(request.refreshToken)
            if (!revokedTokenRepository.existsById(request.refreshToken)) {
                revokedTokenRepository.save(
                    RevokedToken(
                        token = request.refreshToken,
                        userId = userId
                    )
                )
            }
        } catch (ex: Exception) {
            // Logout should be idempotent; just log and move on
            log.warn("Failed to process logout request", ex)
        }
    }

    // ==========
    // Change password (authenticated)
    // ==========

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val principal = TenantContext.get() ?: throw BadCredentialsException("Not authenticated")

        val user = userRepository.findById(principal.userId)
            .orElseThrow { IllegalStateException("User not found") }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadCredentialsException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
    }

    // ==========
    // Forgot / Reset password (public)
    // ==========

    /**
     * Gen a password reset token for the given email + tenant. eventually send an email;
     * here we just persist
     * the token and log it. See dev logs.
     */
    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest) {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val email = request.email.trim().lowercase()

        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: run {
                log.info("Forgot password for unknown tenant: {}", tenantSlug)
                return // do not leak which tenants / users exist
            }

        val user = userRepository.findByEmailAndTenantId(email, tenant.id)
            ?: run {
                log.info("Forgot password for unknown user: {} in tenant {}", email, tenant.id)
                return
            }

        if (!user.active) {
            // treat as no-op, we don't reveal anything
            log.info("Forgot password invoked for inactive user {} in tenant {}", email, tenant.slug)
            return
        }

        val token = PasswordResetToken(
            token = UUID.randomUUID().toString().replace("-", ""),
            userId = user.id,
            tenantId = tenant.id,
            expiresAt = Instant.now().plusSeconds(60 * 60), // 1 hour
            used = false
        )

        passwordResetTokenRepository.save(token)

        // In production: send this token in an email link.
        // For dev: log it so you can grab it for testing.
        log.warn("Password reset token for {} (tenant {}): {}", user.email, tenant.slug, token.token)
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest) {
        val token = passwordResetTokenRepository.findById(request.token)
            .orElseThrow { BadCredentialsException("Invalid or expired reset token") }

        if (token.used || token.expiresAt.isBefore(Instant.now())) {
            throw BadCredentialsException("Invalid or expired reset token")
        }

        val user = userRepository.findById(token.userId)
            .orElseThrow { IllegalStateException("User not found for reset token") }

        if (!user.active) {
            throw BadCredentialsException("User account is disabled")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        token.used = true
        passwordResetTokenRepository.save(token)
    }
}
