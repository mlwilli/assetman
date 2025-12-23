package com.github.mlwilli.assetman.common.web

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

object PagingLimits {
    const val DEFAULT_LIST_LIMIT = 50
    const val MAX_LIST_LIMIT = 200
    const val MAX_DIRECTORY_LIMIT = 50
}

/**
 * Standardizes "limit" clamping + PageRequest creation.
 * We only use page 0 today (API uses "limit" not "page"), but this keeps it consistent.
 */
fun firstPage(
    limit: Int?,
    defaultLimit: Int,
    maxLimit: Int,
    sort: Sort
): Pageable {
    val requested = limit ?: defaultLimit
    val safe = requested.coerceIn(1, maxLimit)
    return PageRequest.of(0, safe, sort)
}
