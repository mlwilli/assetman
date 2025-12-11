package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.Tenant
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.PasswordResetTokenRepository
import com.github.mlwilli.assetman.identity.repo.RevokedTokenRepository
import com.github.mlwilli.assetman.identity.repo.TenantRepository
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.LoginRequest
import com.github.mlwilli.assetman.identity.web.SignupTenantRequest
import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private lateinit var tenantRepository: TenantRepository
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var revokedTokenRepository: RevokedTokenRepository

    private lateinit var authService: AuthService

    @BeforeEach
    fun setup() {
        tenantRepository = Mockito.mock(TenantRepository::class.java)
        userRepository = Mockito.mock(UserRepository::class.java)
        passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository::class.java)
        revokedTokenRepository = Mockito.mock(RevokedTokenRepository::class.java)

        authService = AuthService(
            tenantRepository = tenantRepository,
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenProvider = jwtTokenProvider,
            passwordResetTokenRepository = passwordResetTokenRepository,
            revokedTokenRepository = revokedTokenRepository
        )
    }

    @Test
    fun `signupTenant creates tenant, owner-admin user and returns tokens`() {
        val request = SignupTenantRequest(
            tenantName = "Acme Inc.",
            tenantSlug = "acme",
            adminName = "Alice Admin",
            adminEmail = "alice@acme.test",
            adminPassword = "Secret123!"
        )

        val savedTenant = Tenant(
            name = request.tenantName,
            slug = request.tenantSlug
        )

        val savedUser = User(
            tenantId = savedTenant.id,
            fullName = request.adminName,
            email = request.adminEmail,
            passwordHash = "encoded",
            displayName = request.adminName,
            roles = setOf(Role.OWNER, Role.ADMIN),
            active = true
        )

        Mockito.`when`(tenantRepository.findBySlug(eq(request.tenantSlug))).thenReturn(null)
        Mockito.`when`(tenantRepository.save(any())).thenReturn(savedTenant)

        Mockito.`when`(passwordEncoder.encode(eq(request.adminPassword))).thenReturn("encoded")
        Mockito.`when`(userRepository.save(any())).thenReturn(savedUser)

        Mockito.`when`(
            jwtTokenProvider.generateAccessToken(
                userId = savedUser.id,
                tenantId = savedTenant.id,
                email = savedUser.email,
                roles = setOf("OWNER", "ADMIN")
            )
        ).thenReturn("access-token")

        Mockito.`when`(
            jwtTokenProvider.generateRefreshToken(savedUser.id)
        ).thenReturn("refresh-token")

        val response = authService.signupTenant(request)

        // AuthDto only exposes tokens now
        assertEquals("access-token", response.accessToken)
        assertEquals("refresh-token", response.refreshToken)
    }

    @Test
    fun `login with valid credentials returns tokens`() {
        val tenant = Tenant(name = "Demo Org", slug = "demo")
        val user = User(
            tenantId = tenant.id,
            fullName = "Demo User",
            email = "user@demo.test",
            passwordHash = "hashed",
            displayName = "Demo",
            roles = setOf(Role.VIEWER),
            active = true
        )

        val request = LoginRequest(
            tenantSlug = tenant.slug,
            email = user.email,
            password = "plain-password"
        )

        Mockito.`when`(tenantRepository.findBySlug(eq(tenant.slug))).thenReturn(tenant)
        Mockito.`when`(userRepository.findByEmailAndTenantId(eq(user.email), eq(tenant.id))).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches(eq("plain-password"), eq("hashed"))).thenReturn(true)

        Mockito.`when`(
            jwtTokenProvider.generateAccessToken(
                userId = user.id,
                tenantId = tenant.id,
                email = user.email,
                roles = setOf("VIEWER")
            )
        ).thenReturn("access-token-demo")

        Mockito.`when`(jwtTokenProvider.generateRefreshToken(user.id)).thenReturn("refresh-token-demo")

        val response = authService.login(request)

        assertEquals("access-token-demo", response.accessToken)
        assertEquals("refresh-token-demo", response.refreshToken)
    }
}
