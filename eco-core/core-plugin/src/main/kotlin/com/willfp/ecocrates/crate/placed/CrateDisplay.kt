package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.EcoPlugin

class CrateDisplay(
    private val plugin: EcoPlugin
) {
    private var tick = 0

    fun start() {
        plugin.scheduler.runTimer(1, 1) { tick() }
    }

    private fun tick() {
        for (crate in PlacedCrates.values()) {
            crate.tick(tick)
        }

        tick++
    }
}