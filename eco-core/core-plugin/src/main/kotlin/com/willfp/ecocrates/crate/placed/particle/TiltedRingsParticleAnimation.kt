package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.util.NumberUtils
import org.bukkit.util.Vector
import kotlin.math.PI

class TiltedRingsParticleAnimation : ParticleAnimation("tilted_rings") {
    override fun getOffset(tick: Int): Vector {
        val baseVector = if (tick % 2 == 0) {
            Vector(
                NumberUtils.fastSin(config.getDouble("spirals-per-second") * 2 * 2 * PI * tick / 20) * config.getDouble("radius"),
                0.0,
                NumberUtils.fastCos(config.getDouble("spirals-per-second") * 2 * 2 * PI * tick / 20) * config.getDouble("radius")
            )
        } else {
            Vector(
                0.0,
                NumberUtils.fastSin(config.getDouble("spirals-per-second") * 2 * 2 * PI * tick / 20) * config.getDouble("radius"),
                NumberUtils.fastCos(config.getDouble("spirals-per-second") * 2 * 2 * PI * tick / 20) * config.getDouble("radius")
            )
        }


        return baseVector.rotateAroundY(NumberUtils.fastCos(config.getDouble("y-offset")))
            .rotateAroundX(NumberUtils.fastCos(config.getDouble("x-offset")))
    }
}
