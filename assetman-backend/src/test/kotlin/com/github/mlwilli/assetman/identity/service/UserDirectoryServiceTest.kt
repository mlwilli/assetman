package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.security.AuthenticatedUser
import com.github.mlwilli.assetman.common.security.TenantContext
import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.testsupport.pageOf
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.util.UUID

class UserDirectoryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var service: UserDirectoryService

    private val tenantId: UUID = UUID.randomUUID()
    private val userId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        userRepository = Mockito.mock(UserRepository::class.java)
        service = UserDirectoryService(userRepository)

        TenantContext.set(
            AuthenticatedUser(
                userId = userId,
                tenantId = tenantId,
                email = "tester@tenant.test",
                roles = setOf("ADMIN")
            )
        )
    }

    @AfterEach
    fun tearDown() {
        TenantContext.clear()
    }

    @Test
    fun `listDirectory blank search + activeOnly=true uses findAllByTenantIdAndActive`() {
        val activeUser = User(
            tenantId = tenantId,
            fullName = "Active User",
            email = "active@tenant.test",
            passwordHash = "x",
            roles = setOf(Role.USER),
            active = true
        )

        whenever(
            userRepository.findAllByTenantIdAndActive(
                eq(tenantId),
                eq(true),
                any<Pageable>()

            )
        ).thenReturn(pageOf(activeUser))

        val result = service.listDirectory(search = "   ", limit = 20, activeOnly = true)

        assertEquals(1, result.size)
        assertEquals("active@tenant.test", result[0].email)

        verify(userRepository).findAllByTenantIdAndActive(
            eq(tenantId),
            eq(true),
            any<Pageable>()

        )

        verify(userRepository, never()).findAllByTenantId(
            eq(tenantId),
            any<Pageable>()

        )
    }

    @Test
    fun `listDirectory blank search + activeOnly=false uses findAllByTenantId`() {
        val u1 = User(
            tenantId = tenantId,
            fullName = "Active User",
            email = "active@tenant.test",
            passwordHash = "x",
            roles = setOf(Role.USER),
            active = true
        )
        val u2 = User(
            tenantId = tenantId,
            fullName = "Inactive User",
            email = "inactive@tenant.test",
            passwordHash = "x",
            roles = setOf(Role.USER),
            active = false
        )

        whenever(
            userRepository.findAllByTenantId(
                eq(tenantId),
                any<Pageable>()

            )
        ).thenReturn(pageOf(u1, u2))


        val result = service.listDirectory(search = null, limit = 20, activeOnly = false)

        assertEquals(2, result.size)
        assertEquals(listOf("active@tenant.test", "inactive@tenant.test"), result.map { it.email })

        verify(userRepository).findAllByTenantId(
            eq(tenantId),
            any<Pageable>()
        )

        verify(userRepository, never()).findAllByTenantIdAndActive(
            eq(tenantId),
            eq(true),
            any<Pageable>()
        )
    }

    @Test
    fun `listDirectory non-blank search uses searchDirectory and passes activeOnly`() {
        val matched = User(
            tenantId = tenantId,
            fullName = "Alice Admin",
            email = "alice@tenant.test",
            passwordHash = "x",
            roles = setOf(Role.ADMIN),
            active = true
        )

        whenever(
            userRepository.searchDirectory(
                eq(tenantId),
                eq("alice"),
                eq(true),
                any<Pageable>()
            )
        ).thenReturn(pageOf(matched))

        val result = service.listDirectory(search = " alice ", limit = 20, activeOnly = true)

        assertEquals(1, result.size)
        assertEquals("alice@tenant.test", result[0].email)

        verify(userRepository).searchDirectory(
            eq(tenantId),
            eq("alice"),
            eq(true),
            any<Pageable>()
        )

        verify(userRepository, never()).findAllByTenantId(
            eq(tenantId),
            any<Pageable>()
        )

        verify(userRepository, never()).findAllByTenantIdAndActive(
            eq(tenantId),
            eq(true),
            any<Pageable>()
        )
    }
}
