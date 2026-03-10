package com.willfp.ecocrates.crate.placed.particle

import com.willfp.eco.core.particle.SpawnableParticle
import com.willfp.eco.core.registry.Registrable
import com.willfp.ecocrates.plugin
import org.bukkit.Location
import org.bukkit.util.Vector

abstract class ParticleAnimation(
    val id: String
) : Registrable {
    protected open val config = plugin.configYml.getSubsection("animations.$id")

    init {
        ParticleAnimations.register(this)
    }

    fun spawnParticle(center: Location, tick: Int, particle: SpawnableParticle) {
        val location = center.clone().add(getOffset(tick))
        particle.spawn(location)
    }

    protected abstract fun getOffset(tick: Int): Vector

    override fun getID(): String {
        return id
    }
}
