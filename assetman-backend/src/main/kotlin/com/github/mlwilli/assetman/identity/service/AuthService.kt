package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import com.github.mlwilli.assetman.common.security.requireCurrentUser
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
    private val revokedTokenRepository: RevokedTokenRepository,
    private val companyBootstrapService: CompanyBootstrapService
) {


    private val log = LoggerFactory.getLogger(AuthService::class.java)

    // ==========
    // Helpers
    // ==========

    /**
     * Central helper to build AuthDto from a User.
     *
     * NOTE:
     * - This intentionally does NOT include companyId (cid) claim.
     * - Company selection happens via /api/companies/select which returns a NEW access token with cid.
     */
    private fun toAuthDto(user: User): AuthDto {
        val accessToken = jwtTokenProvider.generateAccessToken(
            userId = user.id,
            tenantId = user.tenantId,
            email = user.email,
            roles = user.roles.map { it.name }.toSet(),
            companyId = null
        )
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id)
        return AuthDto(accessToken, refreshToken)
    }

    // ==========
    // Sign-up & Login
    // ==========

    @Transactional
    fun signupTenant(request: SignupTenantRequest): AuthDto {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val adminEmail = request.adminEmail.trim().lowercase()

        val tenant = tenantRepository.save(
            Tenant(
                name = request.tenantName.trim(),
                slug = tenantSlug
            )
        )

        val user = userRepository.save(
            User(
                tenantId = tenant.id,
                fullName = request.adminName.trim(),
                email = adminEmail,
                passwordHash = passwordEncoder.encode(request.adminPassword),
                roles = mutableSetOf(Role.OWNER, Role.ADMIN)
            )
        )

        // Bootstrap initial company + membership (does NOT auto-select company in token)
        companyBootstrapService.bootstrapDefaultCompany(tenant = tenant, ownerUser = user)

        return toAuthDto(user)
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthDto {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val email = request.email.trim().lowercase()

        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw BadCredentialsException("Invalid credentials")

        val user = userRepository.findByTenantIdAndEmail(tenant.id, email)
            ?: throw BadCredentialsException("Invalid credentials")

        if (!user.active) throw BadCredentialsException("User account is disabled")
        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw BadCredentialsException("Invalid credentials")
        }

        // Return token WITHOUT cid; frontend must call /api/companies/mine then /api/companies/select
        return toAuthDto(user)
    }

    @Transactional(readOnly = true)
    fun refreshTokens(request: RefreshTokenRequest): AuthDto {
        if (revokedTokenRepository.existsById(request.refreshToken)) {
            throw BadCredentialsException("Refresh token has been revoked")
        }

        val userId: UUID = jwtTokenProvider.parseRefreshToken(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { BadCredentialsException("User not found") }

        if (!user.active) throw BadCredentialsException("User account is disabled")

        // Still returns token WITHOUT cid; selection is per-access-token via /api/companies/select
        return toAuthDto(user)
    }

    // ==========
    // Current user
    // ==========

    @Transactional(readOnly = true)
    fun currentUser(): CurrentUserDto {
        val principal = requireCurrentUser()

        val user = userRepository.findById(principal.userId)
            .orElseThrow { BadCredentialsException("Invalid credentials") }

        if (user.tenantId != principal.tenantId) {
            throw BadCredentialsException("Cross-tenant access denied")
        }

        return CurrentUserDto(
            userId = user.id,
            tenantId = user.tenantId,
            email = user.email,
            fullName = user.fullName,
            roles = user.roles.map { it.name }.sorted(),
            companyId = principal.companyId,
            companySelected = principal.companyId != null
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
            log.warn("Failed to process logout request", ex)
        }
    }

    // ==========
    // Change password (authenticated)
    // ==========

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val principal = requireCurrentUser()

        val user = userRepository.findById(principal.userId)
            .orElseThrow { IllegalStateException("User not found") }

        if (user.tenantId != principal.tenantId) {
            throw BadCredentialsException("Cross-tenant access denied")
        }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw BadCredentialsException("Current password is incorrect")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
    }

    // ==========
    // Forgot / Reset password (public)
    // ==========

    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest) {
        val tenantSlug = request.tenantSlug.trim().lowercase()
        val email = request.email.trim().lowercase()

        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: run {
                log.info("Forgot password for unknown tenant: {}", tenantSlug)
                return
            }

        val user = userRepository.findByTenantIdAndEmail(tenant.id, email)
            ?: run {
                log.info("Forgot password for unknown user: {} in tenant {}", email, tenant.id)
                return
            }

        if (!user.active) {
            log.info("Forgot password invoked for inactive user {} in tenant {}", email, tenant.slug)
            return
        }

        val token = PasswordResetToken(
            token = UUID.randomUUID().toString().replace("-", ""),
            userId = user.id,
            tenantId = tenant.id,
            expiresAt = Instant.now().plusSeconds(60 * 60),
            used = false
        )

        passwordResetTokenRepository.save(token)
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

        if (!user.active) throw BadCredentialsException("User account is disabled")

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        token.used = true
        passwordResetTokenRepository.save(token)
    }
}
