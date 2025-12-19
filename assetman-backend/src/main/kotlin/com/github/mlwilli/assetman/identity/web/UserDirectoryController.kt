package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.UserDirectoryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID


@RestController
@RequestMapping("/api/users")
class UserDirectoryController(
    private val service: UserDirectoryService
) {

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun listUsers(
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "20") limit: Int,
        @RequestParam(defaultValue = "true") activeOnly: Boolean
    ): List<UserDirectoryDto> =
        service.listDirectory(search, limit, activeOnly)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN','VIEWER')")
    fun getUser(@PathVariable id: UUID): UserDirectoryDto =
        service.getUser(id)
}

