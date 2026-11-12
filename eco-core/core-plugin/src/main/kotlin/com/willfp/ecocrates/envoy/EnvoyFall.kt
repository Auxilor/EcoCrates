package com.willfp.ecocrates.envoy

import wiki.ecohub.config.Config

/**
 * Envoy crates float down from [height] blocks above their spawn point at
 * [speed] blocks per tick, and become claimable once they land.
 */
class EnvoyFall(config: Config) {
    val height = (config.getDoubleOrNull("height") ?: 30.0).coerceAtLeast(1.0)

    val speed = (config.getDoubleOrNull("speed") ?: 0.25).coerceAtLeast(0.01)
}
