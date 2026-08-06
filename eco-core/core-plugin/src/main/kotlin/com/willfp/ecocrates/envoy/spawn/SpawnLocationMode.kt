package com.willfp.ecocrates.envoy.spawn

/** How an envoy category picks where its crates spawn. */
enum class SpawnLocationMode {
    /** Spawn at random offsets within a configured radius/box of a center point. */
    RADIUS,

    /** Spawn only at admin-defined points, set with `/ecocrates envoy set`. */
    POINTS;

    companion object {
        /** Parses [raw] case-insensitively, defaulting to [RADIUS] for unknown values. */
        fun fromString(raw: String): SpawnLocationMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: RADIUS
    }
}
