package com.github.mlwilli.assetman.identity.web3

import com.github.mlwilli.assetman.identity.service.AuthService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class MeGraphQLController(
    private val authService: AuthService
) {

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun me(): CurrentUserDto =
        authService.currentUser()
}
