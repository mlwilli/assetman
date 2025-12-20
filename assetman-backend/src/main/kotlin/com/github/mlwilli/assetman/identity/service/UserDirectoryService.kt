package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.UserDirectoryDto
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import com.github.mlwilli.assetman.common.error.NotFoundException
import java.util.UUID

@Service
class UserDirectoryService(
    private val userRepository: UserRepository
) {

    fun listDirectory(search: String?, limit: Int, activeOnly: Boolean): List<UserDirectoryDto> {
        val tenantId = currentTenantId()
        val safeLimit = limit.coerceIn(1, 50)

        val pageable = PageRequest.of(
            0,
            safeLimit,
            Sort.by("active").descending().and(Sort.by("email").ascending())
        )

        val q = (search ?: "").trim()

        val page =
            if (q.isBlank()) {
                userRepository.findAllByTenantId(tenantId, pageable)
            } else {
                userRepository.searchDirectory(tenantId, q, activeOnly, pageable)
            }

        return page.content.map { it.toDirectoryDto() }
    }

    private fun User.toDirectoryDto(): UserDirectoryDto =
        UserDirectoryDto(
            id = id,
            email = email,
            fullName = fullName,
            displayName = displayName,
            active = active
        )

    fun getUser(userId: UUID): UserDirectoryDto {
        val tenantId = currentTenantId()
        val user = userRepository.findByIdAndTenantId(userId, tenantId)
            ?: throw NotFoundException("User not found")
        return user.toDirectoryDto()
    }
}
