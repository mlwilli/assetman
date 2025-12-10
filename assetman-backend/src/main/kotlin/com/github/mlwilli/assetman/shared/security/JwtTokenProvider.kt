package com.github.mlwilli.assetman.shared.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class JwtTokenProvider(
    @Value("\${assetman.security.jwt.secret}") private val secret: String,
    @Value("\${assetman.security.jwt.access-token-validity-seconds}") private val accessValidity: Long,
    @Value("\${assetman.security.jwt.refresh-token-validity-seconds}") private val refreshValidity: Long
) {
    private lateinit var key: javax.crypto.SecretKey

    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(
        userId: UUID,
        tenantId: UUID,
        email: String,
        roles: Set<String>
    ): String {
        val now = Instant.now()

        return Jwts.builder()
            .subject(userId.toString())
            .claim("tid", tenantId.toString())
            .claim("email", email)
            .claim("roles", roles.joinToString(","))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessValidity)))
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(userId: UUID): String {
        val now = Instant.now()

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(refreshValidity)))
            .signWith(key)
            .compact()
    }

    fun parseToken(token: String): AuthenticatedUser? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            // We only treat it as access token if it has tenant + roles
            val tenantId = claims["tid"]?.toString() ?: return null
            val email = claims["email"]?.toString() ?: return null
            val roles = claims["roles"]?.toString()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()

            AuthenticatedUser(
                userId = UUID.fromString(claims.subject),
                tenantId = UUID.fromString(tenantId),
                email = email,
                roles = roles
            )
        } catch (ex: Exception) {
            null
        }
    }
}
