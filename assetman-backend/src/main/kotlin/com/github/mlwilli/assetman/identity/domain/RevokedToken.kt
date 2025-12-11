package com.github.mlwilli.assetman.identity.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "revoked_tokens")
class RevokedToken(

    @Id
    @Column(name = "token", length = 512, nullable = false)
    val token: String,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "revoked_at", nullable = false, updatable = false)
    val revokedAt: Instant = Instant.now()
)
