package com.willfp.ecocrates.crate.placed.particle

import com.willfp.ecocrates.EcoCratesPlugin
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector

abstract class ParticleAnimation(
    val id: String
) {
    protected val config = EcoCratesPlugin.instance.configYml.getSubsection("animations.$id")

    init {
        register()
    }

    private fun register() {
        ParticleAnimations.addNewAnimation(this)
    }

    fun spawnParticle(center: Location, tick: Int, particle: Particle) {
        val location = center.clone().add(getOffset(tick))
        val world = location.world ?: return
        world.spawnParticle(particle, location, 1, 0.0, 0.0, 0.0, 0.0)
    }

    protected abstract fun getOffset(tick: Int): Vector
}
