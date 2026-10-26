package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.scheduling.EcoTask
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.runOwned

/**
 * Ticks every placed crate. The timers only fire the tick: each crate's own work runs on
 * the region owning its location, which on Folia is not the thread the timer fires on.
 */
object CrateDisplay {
    @Volatile private var tick = 0
    @Volatile private var syncTask: EcoTask? = null
    @Volatile private var asyncTask: EcoTask? = null
    @Volatile private var loadedCrates: List<PlacedCrate> = emptyList()

    fun start() {
        syncTask?.cancel()
        asyncTask?.cancel()

        syncTask = plugin.scheduler.global().runTimer(1, 1) { tick() }
        asyncTask = plugin.scheduler.async().runTimer(1, 1) { tickAsync() }
    }

    private fun tick() {
        val tick = tick

        loadedCrates = PlacedCrates.values().filter { it.location.isChunkLoaded }

        for (crate in loadedCrates) {
            crate.location.runOwned {
                if (crate.location.isChunkLoaded) {
                    crate.tick(tick)
                }
            }
        }

        this.tick++
    }

    private fun tickAsync() {
        val tick = tick

        for (crate in loadedCrates) {
            crate.location.runOwned { crate.tickAsync(tick) }
        }
    }
}
