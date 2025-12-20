package com.github.mlwilli.assetman.identity.web3

import com.github.mlwilli.assetman.identity.service.AdminUserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
class AdminUserController(
    private val service: AdminUserService
) {

    @GetMapping
    fun list(): List<AdminUserDto> =
        service.listUsers()

    @PostMapping
    fun create(@RequestBody req: CreateUserRequest): AdminUserDto =
        service.createUser(req)

    @PatchMapping("/{id}/roles")
    fun updateRoles(
        @PathVariable id: UUID,
        @RequestBody req: UpdateUserRolesRequest
    ): AdminUserDto =
        service.updateRoles(id, req)

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody req: UpdateUserStatusRequest
    ): AdminUserDto =
        service.updateStatus(id, req)
}
