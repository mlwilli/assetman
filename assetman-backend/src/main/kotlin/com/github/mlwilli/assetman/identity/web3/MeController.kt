package com.github.mlwilli.assetman.identity.web3

import com.github.mlwilli.assetman.identity.service.AuthService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MeController(
    private val authService: AuthService
) {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun me(): CurrentUserDto =
        authService.currentUser()
}
