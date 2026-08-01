package com.willfp.ecocrates.envoy.session

import com.willfp.ecocrates.envoy.EnvoyScheduler
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

            // Schedules only have minute resolution, so checking once a
            // second is plenty and keeps the per-tick cost near zero.
            if (tick % 20 == 0) {
                EnvoyScheduler.check()
            }

            tick++
        }
    }

    fun stop() {
        task?.cancel()
        task = null
    }
}
