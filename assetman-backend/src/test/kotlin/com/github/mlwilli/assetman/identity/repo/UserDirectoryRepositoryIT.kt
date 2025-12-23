package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

@DataJpaTest
class UserDirectoryRepositoryIT {

    @Autowired lateinit var userRepository: UserRepository

    private val tenantA: UUID = UUID.randomUUID()
    private val tenantB: UUID = UUID.randomUUID()

    @BeforeEach
    fun seed() {
        userRepository.deleteAll()

        fun save(
            tenantId: UUID,
            email: String,
            fullName: String,
            displayName: String? = null,
            active: Boolean = true
        ) {
            userRepository.save(
                User(
                    tenantId = tenantId,
                    fullName = fullName,
                    email = email,
                    passwordHash = "x",
                    roles = setOf(Role.USER),
                    active = active
                ).also { it.displayName = displayName }
            )
        }

        // Tenant A
        save(tenantA, "alice@a.test", "Alice Admin", displayName = "Alice", active = true)
        save(tenantA, "bob@a.test", "Bob Builder", displayName = null, active = true)
        save(tenantA, "inactive@a.test", "Inactive Person", displayName = "Old", active = false)

        // Tenant B (should never appear in tenantA searches)
        save(tenantB, "alice@b.test", "Alice Other", displayName = "Alice", active = true)
    }

    @Test
    fun `searchDirectory - matches email fullName displayName - tenant scoped`() {
        val pageable = PageRequest.of(
            0,
            50,
            Sort.by("active").descending().and(Sort.by("email").ascending())
        )

        val page = userRepository.searchDirectory(
            tenantId = tenantA,
            q = "alice",
            activeOnly = false,
            pageable = pageable
        )

        assertEquals(1, page.content.size)
        assertEquals("alice@a.test", page.content[0].email)
    }

    @Test
    fun `searchDirectory - activeOnly true excludes inactive`() {
        val pageable = PageRequest.of(0, 50)

        val page = userRepository.searchDirectory(
            tenantId = tenantA,
            q = "inactive",
            activeOnly = true,
            pageable = pageable
        )

        assertTrue(page.content.isEmpty())
    }

    @Test
    fun `findAllByTenantIdAndActive - returns only active for tenant`() {
        val pageable = PageRequest.of(0, 50, Sort.by("email").ascending())

        val page = userRepository.findAllByTenantIdAndActive(tenantA, true, pageable)

        assertEquals(listOf("alice@a.test", "bob@a.test"), page.content.map { it.email })
    }
}
