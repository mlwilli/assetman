package com.github.mlwilli.assetman.common.security

import java.util.UUID

fun currentCompanyIdOrNull(): UUID? = requireCurrentUser().companyId

fun requireCurrentCompanyId(): UUID =
    currentCompanyIdOrNull()
        ?: throw IllegalStateException("No active company selected for this request")
