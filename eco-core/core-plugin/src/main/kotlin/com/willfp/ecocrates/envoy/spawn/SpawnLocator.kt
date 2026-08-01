package com.willfp.ecocrates.envoy.spawn

import com.willfp.ecocrates.plugin
import org.bukkit.Location

object SpawnLocator {
    /**
     * Find a legal spawn block at or near [candidate].
     *
     * A legal spot is an air block with a solid, non-liquid block directly
     * beneath it, so crates land on the ground instead of floating or sinking.
     * Returns null if nothing legal is found within the configured scan radius.
     */
    fun resolve(candidate: Location): Location? {
        val world = candidate.world ?: return null
        val scanRadius = plugin.configYml.getInt("envoy.spawn-scan-radius").coerceAtLeast(0)

        if (isLegal(candidate)) {
            return candidate.block.location
        }

        // Expand outwards a shell at a time so the nearest legal spot wins.
        for (radius in 1..scanRadius) {
            for (x in -radius..radius) {
                for (y in -radius..radius) {
                    for (z in -radius..radius) {
                        // Only the surface of this shell; inner blocks were checked already.
                        if (maxOf(kotlin.math.abs(x), kotlin.math.abs(y), kotlin.math.abs(z)) != radius) {
                            continue
                        }

                        val test = Location(
                            world,
                            (candidate.blockX + x).toDouble(),
                            (candidate.blockY + y).toDouble(),
                            (candidate.blockZ + z).toDouble()
                        )

                        if (isLegal(test)) {
                            return test.block.location
                        }
                    }
                }
            }
        }

        return null
    }

    private fun isLegal(location: Location): Boolean {
        val world = location.world ?: return false

        if (location.blockY <= world.minHeight || location.blockY >= world.maxHeight) {
            return false
        }

        val block = location.block

        if (!block.type.isAir) {
            return false
        }

        val below = block.getRelative(0, -1, 0)

        return below.type.isSolid && !below.isLiquid
    }
}
