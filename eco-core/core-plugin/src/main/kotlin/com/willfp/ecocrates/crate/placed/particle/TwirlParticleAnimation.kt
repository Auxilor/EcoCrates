package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.util.NumberUtils
import org.bukkit.util.Vector
import kotlin.math.PI

class TwirlParticleAnimation : ParticleAnimation("twirl") {
    override fun getOffset(tick: Int): Vector {
        val small = config.getDouble("small-radius")
        val large = config.getDouble("large-radius")
        val ticks = config.getDouble("ticks")
        val radius = lerp(small, large, (tick % ticks) / ticks)
        val startHeight = config.getDouble("start-height")
        val endHeight = config.getDouble("end-height")
        val height = lerp(startHeight, endHeight, (tick % ticks) / ticks)

        return Vector(
            NumberUtils.fastSin(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * radius,
            height,
            NumberUtils.fastCos(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * radius
        ).let {
            if (tick % 2 == 0) {
                it.setX(-it.x)
                    .setZ(-it.z)
            } else it
        }
    }
}

private fun lerp(start: Double, end: Double, fraction: Double): Double =
    (start * (1 - fraction)) + (end * fraction)
