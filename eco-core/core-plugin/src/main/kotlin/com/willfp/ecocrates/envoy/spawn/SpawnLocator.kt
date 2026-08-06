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
            for ((x, y, z) in shellOffsets(radius)) {
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

        return null
    }

    /**
     * Offsets on the surface of a cube shell of the given [radius], i.e. every
     * point where the largest coordinate magnitude equals [radius]. Generated
     * directly instead of filtered out of the full cube, so cost stays O(radius^2)
     * per shell instead of O(radius^3).
     */
    private fun shellOffsets(radius: Int): Sequence<Triple<Int, Int, Int>> = sequence {
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                val xyMaxed = kotlin.math.abs(x) == radius || kotlin.math.abs(y) == radius
                if (xyMaxed) {
                    // Any z is on the shell as long as x or y is already at the radius.
                    for (z in -radius..radius) {
                        yield(Triple(x, y, z))
                    }
                } else {
                    // x and y are both inside the shell, so only the two z caps qualify.
                    yield(Triple(x, y, -radius))
                    yield(Triple(x, y, radius))
                }
            }
        }
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
