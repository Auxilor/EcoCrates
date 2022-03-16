package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.util.NumberUtils
import org.bukkit.Location
import kotlin.math.PI

class DoubleSpiralParticleAnimation : ParticleAnimation("double_spiral") {
    override fun getSpawnLocation(center: Location, tick: Int): Location {
        var x = center.x + (NumberUtils.fastSin(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * config.getDouble("radius"))
        var y = center.y + (-NumberUtils.fastCos(config.getDouble("rises-per-second") * 2 * PI * tick / 20) * config.getDouble("height"))
        var z = center.z + (NumberUtils.fastCos(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * config.getDouble("radius"))

        if (tick % 2 == 0) {
            x *= -1
            y *= -1
            z *= -1
        }

        return center.clone().apply {
            this.x = x
            this.y = y
            this.z = z
        }
    }
}
