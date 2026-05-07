package com.willfp.ecocrates.crate.placed

import com.willfp.ecocrates.plugin
import org.bukkit.scheduler.BukkitTask

object CrateDisplay {
    @Volatile private var tick = 0
    private var syncTask: BukkitTask? = null

    fun start() {
        syncTask?.cancel()
        syncTask = plugin.scheduler.runTimer(1, 1) { tick() }
    }

    private fun tick() {
        for (crate in PlacedCrates.values()) {
            if (!(crate.location.isChunkLoaded)) continue
            crate.tick(tick)
        }

        tick++
    }
}