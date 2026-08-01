package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import java.time.LocalTime

/**
 * When a category should automatically start a session.
 *
 * If [times] is non-empty it takes priority and [intervalTicks] is ignored,
 * matching the design: a category schedules on wall-clock times OR on an
 * interval, never both.
 */
class EnvoySchedule(
    times: List<String>,
    val intervalTicks: Int
) {
    private val times: List<LocalTime> = times.mapNotNull { parseTime(it) }

    val isEnabled: Boolean
        get() = this.times.isNotEmpty() || intervalTicks > 0

    /**
     * Whether a session should start right now.
     *
     * [now] is the current wall-clock time, [lastStart] the wall-clock time of
     * this category's last automatic start (null if it has never started), and
     * [ticksSinceLastStart] how many ticks have elapsed since then.
     */
    fun isDueAt(now: LocalTime, lastStart: LocalTime?, ticksSinceLastStart: Long): Boolean {
        if (times.isNotEmpty()) {
            val matches = times.any { it.hour == now.hour && it.minute == now.minute }

            if (!matches) {
                return false
            }

            // Guard against firing repeatedly for every check inside the same minute.
            val alreadyStartedThisMinute = lastStart != null
                && lastStart.hour == now.hour
                && lastStart.minute == now.minute

            return !alreadyStartedThisMinute
        }

        if (intervalTicks > 0) {
            return ticksSinceLastStart >= intervalTicks
        }

        return false
    }

    /**
     * Ticks until the next scheduled start. Times take priority over the
     * interval, matching [isDueAt].
     */
    fun ticksUntilNext(now: LocalTime, ticksSinceLastStart: Long): Int? {
        if (times.isNotEmpty()) {
            val secondsNow = now.toSecondOfDay()

            val secondsUntil = times
                .map { it.toSecondOfDay() }
                .minOf { target ->
                    // Wrap to tomorrow if today's slot has already passed.
                    if (target >= secondsNow) target - secondsNow else target - secondsNow + 86400
                }

            return secondsUntil * 20
        }

        if (intervalTicks > 0) {
            return (intervalTicks - ticksSinceLastStart).coerceAtLeast(0).toInt()
        }

        return null
    }

    companion object {
        private fun parseTime(raw: String): LocalTime? {
            val split = raw.trim().split(":")

            if (split.size != 2) {
                return null
            }

            val hour = split[0].toIntOrNull() ?: return null
            val minute = split[1].toIntOrNull() ?: return null

            if (hour !in 0..23 || minute !in 0..59) {
                return null
            }

            return LocalTime.of(hour, minute)
        }

        fun fromConfig(config: Config) = EnvoySchedule(
            config.getStrings("start.time"),
            config.getInt("start.interval")
        )
    }
}
