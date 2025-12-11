package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.shared.security.TenantContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class MeResponse(
    val userId: UUID,
    val tenantId: UUID,
    val email: String,
    val roles: Set<String>
)

@RestController
@RequestMapping("/api")
class MeController {

    @GetMapping("/me")
    fun me(): ResponseEntity<MeResponse> {
        val user = TenantContext.get()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated")

        val body = MeResponse(
            userId = user.userId,
            tenantId = user.tenantId,
            email = user.email,
            roles = user.roles
        )

        return ResponseEntity.ok(body)
    }
}
