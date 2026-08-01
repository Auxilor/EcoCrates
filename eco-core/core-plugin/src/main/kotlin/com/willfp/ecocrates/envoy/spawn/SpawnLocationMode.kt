package com.willfp.ecocrates.envoy.spawn

enum class SpawnLocationMode {
    RADIUS,
    POINTS;

    companion object {
        fun fromString(raw: String): SpawnLocationMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: RADIUS
    }
}
