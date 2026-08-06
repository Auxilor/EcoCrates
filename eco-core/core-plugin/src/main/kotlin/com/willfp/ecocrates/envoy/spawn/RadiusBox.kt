package com.willfp.ecocrates.envoy.spawn

import com.willfp.eco.core.config.interfaces.Config
import kotlin.random.Random

/** A whole-number block coordinate. */
data class BlockPos(
    val x: Int,
    val y: Int,
    val z: Int
)

/**
 * A box in a single world that spawn candidates are rolled inside of.
 */
class RadiusBox(
    val worldName: String,
    val center: BlockPos,
    xRadius: Int,
    yRadius: Int,
    zRadius: Int
) {
    val xRadius = xRadius.coerceAtLeast(0)
    val yRadius = yRadius.coerceAtLeast(0)
    val zRadius = zRadius.coerceAtLeast(0)

    /** Picks a uniformly random position within this box. */
    fun roll(random: Random = Random.Default) = BlockPos(
        center.x + random.nextInt(-xRadius, xRadius + 1),
        center.y + random.nextInt(-yRadius, yRadius + 1),
        center.z + random.nextInt(-zRadius, zRadius + 1)
    )

    companion object {
        /** Reads a `radius:` config section, or null if it has no center world. */
        fun fromConfig(config: Config): RadiusBox? {
            val world = config.getStringOrNull("radius.center.world") ?: return null

            return RadiusBox(
                world,
                BlockPos(
                    config.getInt("radius.center.x"),
                    config.getInt("radius.center.y"),
                    config.getInt("radius.center.z")
                ),
                config.getInt("radius.x_radius"),
                config.getInt("radius.y_radius"),
                config.getInt("radius.z_radius")
            )
        }
    }
}
