package com.github.mlwilli.assetman.location.domain

/**
 * Hierarchy level / semantic type of a location.
 * You can extend this later if needed.
 */
enum class LocationType {
    COUNTRY,
    REGION,
    SITE,
    BUILDING,
    FLOOR,
    ROOM,
    OTHER
}
