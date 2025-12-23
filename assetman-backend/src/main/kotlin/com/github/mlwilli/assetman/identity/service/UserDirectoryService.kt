package com.github.mlwilli.assetman.identity.service

import com.github.mlwilli.assetman.common.security.currentTenantId
import com.github.mlwilli.assetman.identity.domain.User
import com.github.mlwilli.assetman.identity.repo.UserRepository
import com.github.mlwilli.assetman.identity.web.UserDirectoryDto
import org.springframework.stereotype.Service
import com.github.mlwilli.assetman.common.error.NotFoundException
import java.util.UUID
import com.github.mlwilli.assetman.common.web.PagingLimits
import com.github.mlwilli.assetman.common.web.firstPage
import org.springframework.data.domain.Sort

@Service
class UserDirectoryService(
    private val userRepository: UserRepository
) {

    fun listDirectory(search: String?, limit: Int, activeOnly: Boolean): List<UserDirectoryDto> {
        val tenantId = currentTenantId()

        val pageable = firstPage(
            limit = limit,
            defaultLimit = PagingLimits.DEFAULT_LIST_LIMIT,
            maxLimit = PagingLimits.MAX_DIRECTORY_LIMIT,
            sort = Sort.by("active").descending().and(Sort.by("email").ascending())
        )

        val q = (search ?: "").trim()

        val page =
            if (q.isBlank()) {
                if (activeOnly) {
                    userRepository.findAllByTenantIdAndActive(tenantId, true, pageable)
                } else {
                    userRepository.findAllByTenantId(tenantId, pageable)
                }
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

