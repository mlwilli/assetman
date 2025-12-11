package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, String>
