package com.willfp.ecocrates.envoy.session

import com.willfp.eco.core.scheduling.EcoTask
import com.willfp.ecocrates.envoy.EnvoyScheduler
import com.willfp.ecocrates.envoy.compass.CompassRenderer
import com.willfp.ecocrates.envoy.compass.EnvoyCompasses
import com.willfp.ecocrates.plugin

/**
 * The single repeating task behind envoys: it advances the active session
 * (visuals + countdown) and, once a second, checks whether any category is
 * due to auto-start.
 */
object EnvoyTicker {
    @Volatile
    private var tick = 0

    private var task: EcoTask? = null

    fun start() {
        task?.cancel()

        tick = 0
        task = plugin.scheduler.runTimer(1, 1) {
            EnvoySessions.tick(tick)
            EnvoyCompasses.tickCountdown()
            EnvoyBossBarManager.refresh()

            // The nearest-N set only changes as players walk or crates are
            // collected, so twice a second is smooth without spamming packets.
            if (tick % 10 == 0) {
                CompassRenderer.refresh()
            }

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
