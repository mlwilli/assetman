package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.CreateUserRequest
import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AdminUserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var adminUserService: AdminUserService

    private val tenantId: UUID = UUID.randomUUID()
    private val adminUserId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        userRepository = Mockito.mock(UserRepository::class.java)
        passwordEncoder = Mockito.mock(PasswordEncoder::class.java)

        adminUserService = AdminUserService(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder
        )

        TenantContext.set(
            AuthenticatedUser(
                userId = adminUserId,
                tenantId = tenantId,
                email = "admin@tenant.test",
                roles = setOf("OWNER", "ADMIN")
            )
        )
    }

    @AfterEach
    fun cleanup() {
        TenantContext.clear()
    }

    @Test
    fun `createUser creates active user in current tenant`() {
        val request = CreateUserRequest(
            email = "new.user@tenant.test",
            displayName = "New User",
            password = "Password123!",
            roles = listOf("USER")
        )

        whenever(userRepository.findByEmailAndTenantId(eq(request.email), eq(tenantId))).thenReturn(null)
        whenever(passwordEncoder.encode(eq(request.password))).thenReturn("encoded-pass")

        val savedUser = User(
            tenantId = tenantId,
            fullName = request.displayName ?: request.email,
            email = request.email,
            passwordHash = "encoded-pass",
            displayName = request.displayName,
            roles = setOf(Role.VIEWER),
            active = true
        )

        whenever(userRepository.save(any())).thenReturn(savedUser)

        val dto = adminUserService.createUser(request)

        assertEquals(savedUser.id, dto.id)
        assertEquals(savedUser.email, dto.email)
        assertEquals(savedUser.displayName, dto.displayName)
        assertTrue(dto.roles.contains("VIEWER"))
        assertTrue(dto.active)
    }

    @Test
    fun `createUser throws ConflictException when email already exists`() {
        val request = CreateUserRequest(
            email = "existing@tenant.test",
            displayName = null,
            password = "Password123!",
            roles = listOf("VIEWER")
        )

        val existing = User(
            tenantId = tenantId,
            fullName = "Existing",
            email = request.email,
            passwordHash = "hash",
            displayName = "Existing",
            roles = setOf(Role.VIEWER),
            active = true
        )

        whenever(userRepository.findByEmailAndTenantId(eq(request.email), eq(tenantId))).thenReturn(existing)

        assertThrows(ConflictException::class.java) {
            adminUserService.createUser(request)
        }
    }
}
