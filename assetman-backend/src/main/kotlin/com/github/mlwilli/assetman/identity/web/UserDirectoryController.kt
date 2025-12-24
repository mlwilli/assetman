package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.UserDirectoryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PathVariable
import java.util.UUID

// check api, 2.0 overhaul change..
// todo: move to diff branch
@RestController
@RequestMapping("/api/users")
class UserDirectoryController(
    private val service: UserDirectoryService
) {

    @GetMapping("/directory")
    @PreAuthorize(
        "hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN') and " +
                "(#activeOnly == true or hasAnyRole('OWNER','ADMIN'))"
    )
    fun listDirectory(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean
    ): List<UserDirectoryDto> =
        service.listDirectory(search, limit, activeOnly)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MANAGER','TECHNICIAN')")
    fun getUser(@PathVariable id: UUID): UserDirectoryDto =
        service.getUser(id)
}
