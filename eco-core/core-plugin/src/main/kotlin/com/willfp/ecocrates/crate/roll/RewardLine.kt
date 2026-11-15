package com.willfp.ecocrates.crate.roll

import kotlin.math.cos
import kotlin.math.sin

/**
 * A horizontal offset from the crate for one reward in a line of rewards.
 */
data class LineOffset(
    val x: Double,
    val z: Double
)

/**
 * Lays rewards out side by side in a line perpendicular to the player's view, centred on the crate.
 */
object RewardLine {
    /**
     * The default distance in blocks between neighbouring rewards, wide enough that their name labels
     * don't overlap.
     */
    const val DEFAULT_SPACING = 1.25

    /**
     * Gets the offsets of [count] rewards from the crate, ordered left to right as the player sees them.
     *
     * @param count The number of rewards.
     * @param yaw The player's yaw in degrees, as Minecraft measures it.
     * @param spacing The distance in blocks between neighbouring rewards.
     * @return One offset per reward. A single reward sits on the crate.
     */
    fun offsets(count: Int, yaw: Double, spacing: Double = DEFAULT_SPACING): List<LineOffset> {
        val radians = Math.toRadians(yaw)
        val rightX = -cos(radians)
        val rightZ = -sin(radians)
        val centre = (count - 1) / 2.0

        return (0 until count).map { index ->
            val distance = (index - centre) * spacing
            LineOffset(x = rightX * distance, z = rightZ * distance)
        }
    }
}
