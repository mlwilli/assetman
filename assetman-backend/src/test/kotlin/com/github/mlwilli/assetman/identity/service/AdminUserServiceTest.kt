package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.CreateUserRequest
import com.github.mlwilli.assetman.identity.web.UpdateUserRolesRequest
import com.github.mlwilli.assetman.identity.web.UpdateUserStatusRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class AdminUserServiceTest {
    // test mctestface
    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var service: AdminUserService

    private val tenantId: UUID = UUID.randomUUID()
    private val adminUserId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userRepository = Mockito.mock(UserRepository::class.java)
        passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
        service = AdminUserService(userRepository, passwordEncoder)

        // Simulate authenticated OWNER/ADMIN in the current tenant
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
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `listUsers returns active users first, then sorted by email`() {
        // given
        val inactiveUser = User(
            tenantId = tenantId,
            fullName = "Zed Inactive",
            email = "zed@tenant.test",
            passwordHash = "x",
            displayName = "Zed",
            roles = setOf(Role.USER),
            active = false
        )

        val alice = User(
            tenantId = tenantId,
            fullName = "Alice Active",
            email = "alice@tenant.test",
            passwordHash = "x",
            displayName = "Alice",
            roles = setOf(Role.ADMIN),
            active = true
        )

        val bob = User(
            tenantId = tenantId,
            fullName = "Bob Active",
            email = "bob@tenant.test",
            passwordHash = "x",
            displayName = "Bob",
            roles = setOf(Role.USER),
            active = true
        )

        val page = PageImpl(listOf(inactiveUser, alice, bob))
        val expectedPageable = PageRequest.of(0, Int.MAX_VALUE)

        Mockito.`when`(
            userRepository.findAllByTenantId(tenantId, expectedPageable)
        ).thenReturn(page)

        // when
        val result = service.listUsers()

        // then
        assertEquals(3, result.size, "Expected three users in the DTO list")

        // Active users first, sorted by email
        assertEquals("alice@tenant.test", result[0].email)
        assertEquals("bob@tenant.test", result[1].email)
        assertEquals("zed@tenant.test", result[2].email)

        assertTrue(result[0].active, "First user should be active")
        assertTrue(result[1].active, "Second user should be active")
        assertFalse(result[2].active, "Last user should be inactive")
    }

    @Test
    fun `createUser creates user in current tenant and returns dto`() {
        // given
        val request = CreateUserRequest(
            email = "new.user@tenant.test",
            displayName = "New User",
            password = "Secret123!",
            roles = listOf("ADMIN", "USER")
        )

        // No existing user with that email in this tenant
        Mockito.`when`(
            userRepository.findByEmailAndTenantId(request.email, tenantId)
        ).thenReturn(null)

        Mockito.`when`(
            passwordEncoder.encode(request.password)
        ).thenReturn("encoded-pw")

        val savedUser = User(
            tenantId = tenantId,
            fullName = request.displayName!!,
            email = request.email,
            passwordHash = "encoded-pw",
            displayName = request.displayName,
            roles = setOf(Role.ADMIN, Role.USER),
            active = true
        )

        Mockito.`when`(
            userRepository.save(ArgumentMatchers.any(User::class.java))
        ).thenReturn(savedUser)

        // when
        val dto = service.createUser(request)

        // then
        assertEquals(savedUser.email, dto.email, "Email should match saved user")
        assertEquals(savedUser.displayName, dto.displayName, "Display name should match")
        assertTrue(dto.active, "New user should be active by default")
        assertEquals(setOf("ADMIN", "USER"), dto.roles.toSet(), "Roles should be mapped correctly")
    }

    @Test
    fun `createUser throws ConflictException when email already exists in tenant`() {
        // given
        val request = CreateUserRequest(
            email = "existing@tenant.test",
            displayName = "Existing User",
            password = "pw",
            roles = listOf("USER")
        )

        val existingUser = User(
            tenantId = tenantId,
            fullName = "Existing User",
            email = request.email,
            passwordHash = "hashed",
            displayName = "Existing",
            roles = setOf(Role.USER),
            active = true
        )

        Mockito.`when`(
            userRepository.findByEmailAndTenantId(request.email, tenantId)
        ).thenReturn(existingUser)

        // when + then
        assertThrows(ConflictException::class.java) {
            service.createUser(request)
        }
    }

    @Test
    fun `updateRoles updates roles for user in current tenant`() {
        // given
        val userId = UUID.randomUUID()

        val user = User(
            tenantId = tenantId,
            fullName = "Role User",
            email = "role.user@tenant.test",
            passwordHash = "hashed",
            displayName = "Role User",
            roles = setOf(Role.USER),
            active = true
        )

        Mockito.`when`(
            userRepository.findByIdAndTenantId(userId, tenantId)
        ).thenReturn(user)

        Mockito.`when`(
            userRepository.save(ArgumentMatchers.any(User::class.java))
        ).thenAnswer { it.arguments[0] as User }

        val request = UpdateUserRolesRequest(
            roles = listOf("OWNER", "ADMIN")
        )

        // when
        val dto = service.updateRoles(userId, request)

        // then
        assertEquals(
            setOf("OWNER", "ADMIN"),
            dto.roles.toSet(),
            "Roles should be updated and mapped to strings"
        )
    }

    @Test
    fun `updateRoles throws NotFoundException when user is not in tenant`() {
        val userId = UUID.randomUUID()

        Mockito.`when`(
            userRepository.findByIdAndTenantId(userId, tenantId)
        ).thenReturn(null)

        val request = UpdateUserRolesRequest(
            roles = listOf("OWNER")
        )

        assertThrows(NotFoundException::class.java) {
            service.updateRoles(userId, request)
        }
    }

    @Test
    fun `updateStatus updates active flag for user in current tenant`() {
        // given
        val userId = UUID.randomUUID()

        val user = User(
            tenantId = tenantId,
            fullName = "Status User",
            email = "status.user@tenant.test",
            passwordHash = "hashed",
            displayName = "Status User",
            roles = setOf(Role.USER),
            active = true
        )

        Mockito.`when`(
            userRepository.findByIdAndTenantId(userId, tenantId)
        ).thenReturn(user)

        Mockito.`when`(
            userRepository.save(ArgumentMatchers.any(User::class.java))
        ).thenAnswer { it.arguments[0] as User }

        val request = UpdateUserStatusRequest(
            active = false
        )

        // when
        val dto = service.updateStatus(userId, request)

        // then
        assertFalse(dto.active, "User should be marked inactive after update")
    }

    @Test
    fun `updateStatus throws NotFoundException when user is not in tenant`() {
        val userId = UUID.randomUUID()

        Mockito.`when`(
            userRepository.findByIdAndTenantId(userId, tenantId)
        ).thenReturn(null)

        val request = UpdateUserStatusRequest(
            active = false
        )

        assertThrows(NotFoundException::class.java) {
            service.updateStatus(userId, request)
        }
    }
}
