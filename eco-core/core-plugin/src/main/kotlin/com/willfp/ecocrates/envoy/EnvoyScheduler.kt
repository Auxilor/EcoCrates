package com.willfp.ecocrates.envoy

import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.plugin
import java.time.LocalTime

/**
 * Checks every category's schedule and starts one if it's due.
 *
 * Only one session can run at a time, so a scheduled start while another
 * session is live is simply skipped - it is not queued, and it does not
 * cancel the running session.
 */
object EnvoyScheduler {
    fun check() {
        if (EnvoySessions.isActive()) {
            return
        }

        val now = LocalTime.now()

        for (category in Envoys.values()) {
            if (!category.schedule.isEnabled) {
                continue
            }

            val due = category.schedule.isDueAt(
                now,
                EnvoySessions.lastStartTime(category.id),
                EnvoySessions.lastStartTicks(category.id)
            )

            if (!due) {
                continue
            }

            if (EnvoySessions.start(category)) {
                plugin.logger.info("Started scheduled envoy '${category.id}'.")
            }

            // One start per check, at most.
            return
        }
    }
}
