package com.github.mlwilli.assetman.identity.web

import com.github.mlwilli.assetman.identity.service.AuthService
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MeGraphQLController(
    private val authService: AuthService
) {

    @QueryMapping
    fun me(): CurrentUserDto =
        authService.currentUser()
}
