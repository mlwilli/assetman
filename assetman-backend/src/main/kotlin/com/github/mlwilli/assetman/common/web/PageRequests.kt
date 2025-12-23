package com.github.mlwilli.assetman.common.web

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/**
 * Centralized paging + sorting helpers.
 *
 * Use this to avoid scattering:
 *  - limit.coerceIn(...)
 *  - PageRequest.of(0, limit)
 *  - Sort.by(...).and(...)
 *
 * across services/controllers.
 */
object PageRequests {

    /**
     * Page 0 with a clamped size.
     */
    fun firstPage(
        limit: Int,
        min: Int = 1,
        max: Int = 200,
        sort: Sort = Sort.unsorted()
    ): PageRequest {
        val safe = limit.coerceIn(min, max)
        return PageRequest.of(0, safe, sort)
    }

    /**
     * Generic page request with clamped size.
     */
    fun page(
        page: Int,
        size: Int,
        minSize: Int = 1,
        maxSize: Int = 200,
        sort: Sort = Sort.unsorted()
    ): PageRequest {
        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(minSize, maxSize)
        return PageRequest.of(safePage, safeSize, sort)
    }
}

/**
 * Canonical Sorts used across the app.
 * Keep these stable to reduce drift.
 */
object Sorts {

    fun activeDescEmailAsc(): Sort =
        Sort.by("active").descending()
            .and(Sort.by("email").ascending())

    fun nameAsc(): Sort =
        Sort.by("name").ascending()

    fun createdAtDescNameAsc(): Sort =
        Sort.by("createdAt").descending()
            .and(Sort.by("name").ascending())
}
