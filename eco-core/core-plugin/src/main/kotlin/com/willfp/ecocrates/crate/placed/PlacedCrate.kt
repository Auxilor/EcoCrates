package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.integrations.hologram.HologramManager
import com.willfp.ecocrates.crate.Crate
import org.bukkit.Location

class PlacedCrate(
    val crate: Crate,
    blockLocation: Location
) {
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
        tickHolograms()
    }

    internal fun onRemove() {
        hologram.remove()
    }

    private fun tickHolograms() {
        //hologram.setContents(crate.hologramLines)
    }
}
