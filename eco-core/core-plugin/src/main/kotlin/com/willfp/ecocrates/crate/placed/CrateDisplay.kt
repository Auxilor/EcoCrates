package com.willfp.ecocrates.crate.placed

import com.willfp.eco.core.scheduling.EcoWrappedTask
import com.willfp.ecocrates.plugin

object CrateDisplay {
    @Volatile
    private var tick = 0
    private var syncTask: EcoWrappedTask? = null
    private var asyncTask: EcoWrappedTask? = null

    fun start() {
        syncTask?.cancelTask()
        asyncTask?.cancelTask()

        syncTask = plugin.scheduler.runTaskTimer(1, 1) { tick() }
        asyncTask = plugin.scheduler.runTaskAsyncTimer(1, 1) { tickAsync() }
    }

    private fun tick() {
        for (crate in PlacedCrates.values()) {
            // folia issue, won't wake up the chunk
            plugin.scheduler.runTask(crate.location) {
                if (crate.location.isChunkLoaded)
                    crate.tick(tick)
            }
        }

        tick++
    }

    private fun tickAsync() {
        for (crate in PlacedCrates.values()) {
            // folia issue, won't wake up the chunk
            plugin.scheduler.runTask(crate.location) {
                if (crate.location.isChunkLoaded)
                    crate.tickAsync(tick)
            }
        }
    }
}