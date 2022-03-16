package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.util.NumberUtils
import org.bukkit.Location
import kotlin.math.PI

class SpiralParticleAnimation : ParticleAnimation("spiral") {
    override fun getSpawnLocation(center: Location, tick: Int): Location {
        val x = center.x + (NumberUtils.fastSin(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * config.getDouble("radius"))
        val y = center.y + (-NumberUtils.fastCos(config.getDouble("rises-per-second") * 2 * PI * tick / 20) * config.getDouble("height"))
        val z = center.z + (NumberUtils.fastCos(config.getDouble("spirals-per-second") * 2 * PI * tick / 20) * config.getDouble("radius"))

        return center.clone().apply {
            this.x = x
            this.y = y
            this.z = z
        }
    }
}
