package com.willfp.ecocrates.crate.placed

import com.willfp.ecocrates.plugin
import org.bukkit.scheduler.BukkitTask

object CrateDisplay {
    @Volatile private var tick = 0
    private var syncTask: BukkitTask? = null
    private var asyncTask: BukkitTask? = null

    fun start() {
        syncTask?.cancel()
        asyncTask?.cancel()

        syncTask = plugin.scheduler.runTimer(1, 1) { tick() }
        asyncTask = plugin.scheduler.runAsyncTimer(1, 1) { tickAsync() }
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