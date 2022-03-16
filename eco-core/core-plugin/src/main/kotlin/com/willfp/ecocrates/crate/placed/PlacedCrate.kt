package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.EcoCratesPlugin
import com.willfp.ecocrates.crate.Crate
import org.bukkit.Location

class PlacedCrate(
    val crate: Crate,
    blockLocation: Location
) {
    private val plugin = EcoCratesPlugin.instance

    // Center the location, they're mutable because bukkit is bad at designing APIs.
    private val location = blockLocation.clone().apply {
        x += 0.5
        y += 0.5
        z += 0.5
    }

    private val hologram = HologramManager.createHologram(location.clone().apply {
        y += crate.hologramHeight
    }, crate.hologramLines)

    internal fun tick(tick: Int) {
        tick.toLong() // Just shut up, compiler
        plugin.scheduler.runAsync { tickAsync(tick) }
    }

    private fun tickAsync(tick: Int) {
        tickParticles(tick)
    }

    internal fun onRemove() {
        hologram.remove()
    }

    private fun tickParticles(tick: Int) {
        for ((particle, animation) in crate.particles) {
            animation.spawnParticle(location, tick, particle)
        }
    }
}
