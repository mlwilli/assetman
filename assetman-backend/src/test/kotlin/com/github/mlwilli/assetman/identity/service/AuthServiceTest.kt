package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.JwtTokenProvider
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.identity.domain.*
import com.github.mlwilli.assetman.identity.repo.*
import com.github.mlwilli.assetman.identity.web.*
import com.github.mlwilli.assetman.testsupport.setBaseEntityFields
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID

class AuthServiceTest {

    private lateinit var tenantRepository: TenantRepository
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var revokedTokenRepository: RevokedTokenRepository
    private lateinit var service: AuthService

    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        tenantRepository = Mockito.mock(TenantRepository::class.java)
        userRepository = Mockito.mock(UserRepository::class.java)
        passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
        jwtTokenProvider = Mockito.mock(JwtTokenProvider::class.java)
        passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository::class.java)
        revokedTokenRepository = Mockito.mock(RevokedTokenRepository::class.java)

        service = AuthService(
            tenantRepository,
            userRepository,
            passwordEncoder,
            jwtTokenProvider,
            passwordResetTokenRepository,
            revokedTokenRepository
        )

        TenantContext.clear()
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    // --------------------------------------------------------------------
    // signupTenant
    // --------------------------------------------------------------------

    @Test
    fun `signupTenant creates tenant and owner-admin user and returns tokens`() {
        val request = SignupTenantRequest(
            tenantName = "Acme Inc",
            tenantSlug = "acme",
            adminName = "Alice Admin",
            adminEmail = "alice@acme.test",
            adminPassword = "Secret123!"
        )

        val tenant = Tenant(
            name = request.tenantName.trim(),
            slug = request.tenantSlug.trim().lowercase()
        )
        setBaseEntityFields(tenant, tenantId)

        val user = User(
            tenantId = tenantId,
            fullName = request.adminName.trim(),
            email = request.adminEmail.trim().lowercase(),
            passwordHash = "hashed",
            roles = setOf(Role.OWNER, Role.ADMIN)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(
            tenantRepository.save(any())
        ).thenReturn(tenant)

        Mockito.`when`(
            passwordEncoder.encode(eq(request.adminPassword))
        ).thenReturn("hashed")

        Mockito.`when`(
            userRepository.save(any())
        ).thenReturn(user)

        Mockito.`when`(
            jwtTokenProvider.generateAccessToken(
                userId = eq(userId),
                tenantId = eq(tenantId),
                email = eq(user.email),
                roles = eq(setOf("OWNER", "ADMIN"))
            )
        ).thenReturn("access-token")

        Mockito.`when`(
            jwtTokenProvider.generateRefreshToken(eq(userId))
        ).thenReturn("refresh-token")

        val dto = service.signupTenant(request)

        assertEquals("access-token", dto.accessToken)
        assertEquals("refresh-token", dto.refreshToken)
    }

    // --------------------------------------------------------------------
    // login
    // --------------------------------------------------------------------

    @Test
    fun `login with valid credentials returns tokens`() {
        val request = LoginRequest(
            tenantSlug = "acme",
            email = "user@acme.test",
            password = "pw"
        )

        val tenant = Tenant(name = "Acme", slug = "acme")
        setBaseEntityFields(tenant, tenantId)

        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "hashed",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(tenantRepository.findBySlug(eq("acme"))).thenReturn(tenant)
        Mockito.`when`(userRepository.findByTenantIdAndEmail(eq(tenantId), eq("user@acme.test"))).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches(eq("pw"), eq("hashed"))).thenReturn(true)
        Mockito.`when`(
            jwtTokenProvider.generateAccessToken(
                userId = eq(userId),
                tenantId = eq(tenantId),
                email = eq(user.email),
                roles = eq(setOf("USER"))
            )
        ).thenReturn("access")
        Mockito.`when`(
            jwtTokenProvider.generateRefreshToken(eq(userId))
        ).thenReturn("refresh")

        val dto = service.login(request)

        assertEquals("access", dto.accessToken)
        assertEquals("refresh", dto.refreshToken)
    }

    @Test
    fun `login with wrong password throws BadCredentialsException`() {
        val request = LoginRequest(
            tenantSlug = "acme",
            email = "user@acme.test",
            password = "wrong"
        )

        val tenant = Tenant(name = "Acme", slug = "acme")
        setBaseEntityFields(tenant, tenantId)

        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "hashed",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(tenantRepository.findBySlug(eq("acme"))).thenReturn(tenant)
        Mockito.`when`(userRepository.findByTenantIdAndEmail(eq(tenantId), eq("user@acme.test"))).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches(eq("wrong"), eq("hashed"))).thenReturn(false)

        assertThrows<BadCredentialsException> {
            service.login(request)
        }
    }

    // --------------------------------------------------------------------
    // refreshTokens
    // --------------------------------------------------------------------

    @Test
    fun `refreshTokens with valid non-revoked token returns new tokens`() {
        val refreshToken = "refresh-token"
        val request = RefreshTokenRequest(refreshToken)

        Mockito.`when`(revokedTokenRepository.existsById(eq(refreshToken))).thenReturn(false)
        Mockito.`when`(jwtTokenProvider.parseRefreshToken(eq(refreshToken))).thenReturn(userId)

        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "hashed",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(userRepository.findById(eq(userId))).thenReturn(java.util.Optional.of(user))

        Mockito.`when`(
            jwtTokenProvider.generateAccessToken(
                userId = eq(userId),
                tenantId = eq(tenantId),
                email = eq(user.email),
                roles = eq(setOf("USER"))
            )
        ).thenReturn("new-access")

        Mockito.`when`(
            jwtTokenProvider.generateRefreshToken(eq(userId))
        ).thenReturn("new-refresh")

        val dto = service.refreshTokens(request)

        assertEquals("new-access", dto.accessToken)
        assertEquals("new-refresh", dto.refreshToken)
    }

    // --------------------------------------------------------------------
    // currentUser
    // --------------------------------------------------------------------

    @Test
    fun `currentUser returns dto based on TenantContext principal`() {
        val user = User(
            tenantId = tenantId,
            fullName = "Current User",
            email = "current@acme.test",
            passwordHash = "hashed",
            roles = setOf(Role.ADMIN)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(userRepository.findById(eq(userId))).thenReturn(java.util.Optional.of(user))

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = user.email,
                roles = setOf("ADMIN")
            )
        )

        val dto = service.currentUser()

        assertEquals(userId, dto.userId)
        assertEquals(tenantId, dto.tenantId)
        assertEquals("current@acme.test", dto.email)
        assertEquals(listOf("ADMIN"), dto.roles)
    }

    // --------------------------------------------------------------------
    // changePassword
    // --------------------------------------------------------------------

    @Test
    fun `changePassword updates password when current password matches`() {
        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "old-hash",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(userRepository.findById(eq(userId))).thenReturn(java.util.Optional.of(user))
        Mockito.`when`(passwordEncoder.matches(eq("old"), eq("old-hash"))).thenReturn(true)
        Mockito.`when`(passwordEncoder.encode(eq("new"))).thenReturn("new-hash")
        Mockito.`when`(userRepository.save(any())).thenAnswer { it.arguments[0] as User }

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = user.email,
                roles = setOf("USER")
            )
        )

        service.changePassword(
            ChangePasswordRequest(
                currentPassword = "old",
                newPassword = "new"
            )
        )

        assertEquals("new-hash", user.passwordHash)
    }

    // --------------------------------------------------------------------
    // forgotPassword + resetPassword (happy path)
    // --------------------------------------------------------------------

    @Test
    fun `forgotPassword with valid tenant and user saves reset token`() {
        val tenant = Tenant(name = "Acme", slug = "acme")
        setBaseEntityFields(tenant, tenantId)

        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "hash",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(tenantRepository.findBySlug(eq("acme"))).thenReturn(tenant)
        Mockito.`when`(userRepository.findByTenantIdAndEmail(eq(tenantId), eq("user@acme.test"))).thenReturn(user)

        val captor = ArgumentCaptor.forClass(PasswordResetToken::class.java)
        Mockito.`when`(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        service.forgotPassword(
            ForgotPasswordRequest(
                tenantSlug = "acme",
                email = "user@acme.test"
            )
        )

        Mockito.verify(passwordResetTokenRepository).save(captor.capture())
        val token = captor.value

        assertEquals(userId, token.userId)
        assertEquals(tenantId, token.tenantId)
        assertFalse(token.used)
    }

    @Test
    fun `resetPassword with valid token updates user password and marks token used`() {
        val tokenId = "token-123"

        val token = PasswordResetToken(
            token = tokenId,
            userId = userId,
            tenantId = tenantId,
            expiresAt = Instant.now().plusSeconds(3600),
            used = false
        )

        val user = User(
            tenantId = tenantId,
            fullName = "User",
            email = "user@acme.test",
            passwordHash = "old",
            roles = setOf(Role.USER)
        )
        setBaseEntityFields(user, userId)

        Mockito.`when`(passwordResetTokenRepository.findById(eq(tokenId)))
            .thenReturn(java.util.Optional.of(token))

        Mockito.`when`(userRepository.findById(eq(userId)))
            .thenReturn(java.util.Optional.of(user))

        Mockito.`when`(passwordEncoder.encode(eq("new")))
            .thenReturn("new-hash")

        Mockito.`when`(userRepository.save(any())).thenAnswer { it.arguments[0] }
        Mockito.`when`(passwordResetTokenRepository.save(any())).thenAnswer { it.arguments[0] }

        service.resetPassword(
            ResetPasswordRequest(
                token = tokenId,
                newPassword = "new"
            )
        )

        assertEquals("new-hash", user.passwordHash)
        assertTrue(token.used)
    }

    // --------------------------------------------------------------------
    // helpers
    // --------------------------------------------------------------------





}
