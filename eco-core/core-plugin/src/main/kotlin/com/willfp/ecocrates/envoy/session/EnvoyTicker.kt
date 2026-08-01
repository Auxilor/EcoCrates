package com.willfp.ecocrates.envoy.session

import com.willfp.ecocrates.plugin
import org.bukkit.scheduler.BukkitTask

/**
 * The single repeating task behind envoys: it advances the active session
 * (visuals + countdown) and, once a second, checks whether any category is
 * due to auto-start.
 */
object EnvoyTicker {
    @Volatile
    private var tick = 0

    private var task: BukkitTask? = null

    fun start() {
        task?.cancel()

        tick = 0
        task = plugin.scheduler.runTimer(1, 1) {
            EnvoySessions.tick(tick)

            // The schedule check is added in Task 11; leaving the hook here
            // keeps the ticking surface in one place.
            tick++
        }
    }

    fun stop() {
        task?.cancel()
        task = null
    }
}
