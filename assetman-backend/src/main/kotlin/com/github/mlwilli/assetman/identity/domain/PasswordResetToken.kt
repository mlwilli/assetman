package com.github.mlwilli.assetman.identity.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(

    @Id
    @Column(name = "token", length = 64, nullable = false)
    val token: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,

    @Column(name = "used", nullable = false)
    var used: Boolean = false,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
