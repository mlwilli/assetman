package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.identity.domain.Role
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.AdminUserDto
import com.github.mlwilli.assetman.identity.web.CreateUserRequest
import com.github.mlwilli.assetman.identity.web.UpdateUserRolesRequest
import com.github.mlwilli.assetman.identity.web.UpdateUserStatusRequest
import com.github.mlwilli.assetman.common.error.ConflictException
import com.github.mlwilli.assetman.common.error.NotFoundException
import com.github.mlwilli.assetman.common.security.currentTenantId
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminUserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun listUsers(): List<AdminUserDto> {
        val tenantId = currentTenantId()

        // Not using pagination yet; if you want, we can switch to pageable.
        val usersPage = userRepository.findAllByTenantId(tenantId, org.springframework.data.domain.PageRequest.of(0, Int.MAX_VALUE))

        return usersPage.content
            .sortedWith(
                compareBy<User> { !it.active }    // active first
                    .thenBy { it.email.lowercase() }
            )
            .map { user -> toDto(user) }
    }

    fun createUser(request: CreateUserRequest): AdminUserDto {
        val tenantId = currentTenantId()

        // Check email only within the current tenant (no cross-tenant info leak).
        val existing = userRepository.findByEmailAndTenantId(request.email, tenantId)
        if (existing != null) {
            throw ConflictException("A user with this email already exists in the tenant")
        }
        val roles = mapRoles(request.roles)
        val user = User(
            tenantId = tenantId,
            fullName = request.displayName ?: request.email,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            displayName = request.displayName,
            roles = roles,
            active = true
        )


        val saved = userRepository.save(user)
        return toDto(saved)
    }

    fun updateRoles(userId: UUID, request: UpdateUserRolesRequest): AdminUserDto {
        val tenantId = currentTenantId()

        val user = userRepository.findByIdAndTenantId(userId, tenantId)
            ?: throw NotFoundException("User not found")

        user.setRoles(mapRoles(request.roles))

        val updated = userRepository.save(user)
        return toDto(updated)
    }

    fun updateStatus(userId: UUID, request: UpdateUserStatusRequest): AdminUserDto {
        val tenantId = currentTenantId()

        val user = userRepository.findByIdAndTenantId(userId, tenantId)
            ?: throw NotFoundException("User not found")

        user.active = request.active

        val updated = userRepository.save(user)
        return toDto(updated)
    }

    // --- helpers ---

    private fun toDto(user: User): AdminUserDto =
        AdminUserDto(
            id = user.id,
            email = user.email,
            displayName = user.displayName,
            roles = user.roles.map { it.name }.sorted(),
            active = user.active
        )

    private fun mapRoles(roleNames: List<String>): Set<Role> =
        roleNames
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .map { name ->
                runCatching { Role.valueOf(name) }
                    .getOrElse { throw IllegalArgumentException("Unknown role: $name") }
            }
            .toSet()
}
