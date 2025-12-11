package com.github.mlwilli.assetman.asset.domain

/**
 * High-level lifecycle status of an asset.
 */
enum class AssetStatus {
    PLANNED,
    PROCURED,
    IN_SERVICE,
    UNDER_MAINTENANCE,
    RETIRED,
    DISPOSED
}
