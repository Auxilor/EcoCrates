package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.core.particle.SpawnableParticle
import com.willfp.ecocrates.EcoCratesPlugin
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.util.Vector

abstract class ParticleAnimation(
    val id: String
) {
    protected open val config = EcoCratesPlugin.instance.configYml.getSubsection("animations.$id")

    init {
        register()
    }

    private fun register() {
        ParticleAnimations.addNewAnimation(this)
    }

    fun spawnParticle(center: Location, tick: Int, particle: SpawnableParticle) {
        val location = center.clone().add(getOffset(tick))
        particle.spawn(location)
    }

    protected abstract fun getOffset(tick: Int): Vector
}
