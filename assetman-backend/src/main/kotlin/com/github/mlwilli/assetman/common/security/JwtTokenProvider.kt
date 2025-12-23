package com.github.mlwilli.assetman.common.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${assetman.security.jwt.secret}")
    private val secret: String,

    @Value("\${assetman.security.jwt.access-token-validity-seconds}")
    private val accessValidity: Long,

    @Value("\${assetman.security.jwt.refresh-token-validity-seconds}")
    private val refreshValidity: Long
) {

    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        val normalized = secret.trim()
        require(normalized.isNotBlank()) {
            "JWT secret is missing. Set ASSETMAN_JWT_SECRET (or configure assetman.security.jwt.secret)."
        }
        // HS256 requires at least 256-bit (32 bytes) key
        require(normalized.toByteArray(Charsets.UTF_8).size >= 32) {
            "JWT secret is too short. Provide at least 32 bytes (256 bits) for HS256."
        }
        key = Keys.hmacShaKeyFor(normalized.toByteArray(Charsets.UTF_8))
    }


    /**
     * Access token for API calls.
     * Claims:
     * - sub  = userId
     * - tid  = tenantId
     * - email
     * - roles = comma-separated roles
     */
    fun generateAccessToken(
        userId: UUID,
        tenantId: UUID,
        email: String,
        roles: Set<String>,
        companyId: UUID? = null
    ): String {
        val now = Instant.now()

        val builder = Jwts.builder()
            .subject(userId.toString())
            .claim("tid", tenantId.toString())
            .claim("email", email)
            .claim("roles", roles.joinToString(","))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessValidity)))
            .signWith(key)

        if (companyId != null) {
            builder.claim("cid", companyId.toString())
        }

        return builder.compact()
    }


    /**
     * Refresh token:
     * - sub  = userId
     * - type = "refresh"
     */
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

    /**
     * Parse an ACCESS token into AuthenticatedUser.
     * Returns null if token is invalid or not an access token
     * (e.g. missing tenant/roles).
     */
    fun parseToken(token: String): AuthenticatedUser? =
        try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            // If this is a refresh token, it won't have tenant/roles/email,
            // so we treat it as invalid for access.
            val tenantId = claims["tid"]?.toString() ?: return null
            val email = claims["email"]?.toString() ?: return null
            val roles = claims["roles"]?.toString()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet()
                ?: emptySet()

            val companyId = claims["cid"]?.toString()?.takeIf { it.isNotBlank() }?.let(UUID::fromString)

            AuthenticatedUser(
                userId = UUID.fromString(claims.subject),
                tenantId = UUID.fromString(tenantId),
                email = email,
                roles = roles,
                companyId = companyId
            )
        } catch (ex: Exception) {
            null
        }

    /**
     * Parse REFRESH token and return userId.
     */
    fun parseRefreshToken(token: String): UUID {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val type = claims["type"]?.toString()
        if (type != "refresh") {
            throw IllegalArgumentException("Not a refresh token")
        }

        return UUID.fromString(claims.subject)
    }
}
