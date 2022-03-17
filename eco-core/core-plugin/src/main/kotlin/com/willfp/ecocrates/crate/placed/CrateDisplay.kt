package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.EcoPlugin

class CrateDisplay(
    private val plugin: EcoPlugin
) {
    private var tick = 0

    fun start() {
        plugin.scheduler.runTimer(1, 1) { tick() }
        plugin.scheduler.runAsyncTimer(1, 1) { tickAsync() }
    }

    private fun tick() {
        for (crate in PlacedCrates.values()) {
            crate.tick(tick)
        }

        tick++
    }

    private fun tickAsync() {
        for (crate in PlacedCrates.values()) {
            crate.tickAsync(tick)
        }
    }
}