package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.scheduling.EcoTask
import com.willfp.ecocrates.plugin

object CrateDisplay {
    @Volatile private var tick = 0
    private var syncTask: EcoTask? = null
    private var asyncTask: EcoTask? = null

    fun start() {
        syncTask?.cancel()
        asyncTask?.cancel()

        syncTask = plugin.scheduler.runTimer(1, 1) { tick() }
        asyncTask = plugin.scheduler.runAsyncTimer(1, 1) { tickAsync() }
    }

    private fun tick() {
        for (crate in PlacedCrates.values()) {
            if (!(crate.location.isChunkLoaded)) continue
            crate.tick(tick)
        }

        tick++
    }

    private fun tickAsync() {
        for (crate in PlacedCrates.values()) {
            if (!(crate.location.isChunkLoaded)) continue
            crate.tickAsync(tick)
        }
    }
}