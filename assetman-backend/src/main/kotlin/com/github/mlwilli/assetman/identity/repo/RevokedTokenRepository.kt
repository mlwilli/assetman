package com.github.mlwilli.assetman.identity.repo

import com.github.mlwilli.assetman.identity.domain.RevokedToken
import org.springframework.data.jpa.repository.JpaRepository

interface RevokedTokenRepository : JpaRepository<RevokedToken, String>
