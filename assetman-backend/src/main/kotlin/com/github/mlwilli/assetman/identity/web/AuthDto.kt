package com.github.mlwilli.assetman.identity.web

data class AuthDto(
    val accessToken: String,
    val refreshToken: String
)
