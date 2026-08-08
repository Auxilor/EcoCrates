package com.willfp.ecocrates.envoy

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.envoy.session.EnvoySession
import com.willfp.ecocrates.plugin
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import java.util.Locale

/**
 * A category's session bossbar. Shown to every online player for the duration of that
 * category's active session; cleared when the session ends for any reason.
 *
 * The name template supports its own small set of hand-rolled placeholders (not
 * PlaceholderAPI, so the bar works with or without PAPI installed) - %envoy_category%,
 * %envoy_remaining_crates%, %envoy_total_crates%, %envoy_remaining_time%,
 * %envoy_remaining_seconds%. If `name` is omitted, the category's display name is used as-is,
 * matching Boosters' own bossbar.name fallback.
 */
class EnvoyBossBar(
    private val config: Config
) {
    val enabled = config.getBool("enabled")

    private val nameTemplate = if (config.has("name")) {
        config.getFormattedString("name")
    } else {
        null
    }

    val color = parseBarColor(config.getString("color"))

    val style = parseBarStyle(config.getString("style"))

    /**
     * Renders the bar's current title for a still-running session. Recomputed on every
     * refresh so the crate counts and countdown stay live.
     */
    fun getName(category: EnvoyCategory, session: EnvoySession): String {
        val template = nameTemplate ?: return category.name

        return template
            .replace("%envoy_category%", category.name)
            .replace("%envoy_remaining_crates%", session.spawns.size.toString())
            .replace("%envoy_total_crates%", session.totalSpawned.toString())
            .replace("%envoy_remaining_time%", formatTicks(session.ticksRemaining))
            .replace("%envoy_remaining_seconds%", (session.ticksRemaining / 20).coerceAtLeast(0).toString())
    }

    /** Progress from 1.0 (just started) down to 0.0 (about to end). */
    fun getProgress(category: EnvoyCategory, session: EnvoySession): Double {
        val duration = category.duration.coerceAtLeast(1)
        return (session.ticksRemaining.toDouble() / duration).coerceIn(0.0, 1.0)
    }

    private fun formatTicks(ticks: Int): String {
        val totalSeconds = (ticks / 20).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun parseBarColor(raw: String): BarColor {
        if (raw.isBlank()) {
            return BarColor.WHITE
        }

        val normalized = raw.trim().replace("-", "_").replace(" ", "_").uppercase(Locale.ROOT)

        return try {
            BarColor.valueOf(normalized)
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning("Invalid bossbar color '$raw' for an envoy, defaulting to WHITE.")
            BarColor.WHITE
        }
    }

    private fun parseBarStyle(raw: String): BarStyle {
        if (raw.isBlank()) {
            return BarStyle.SOLID
        }

        val normalized = raw.trim().replace("-", "_").replace(" ", "_").uppercase(Locale.ROOT)

        return try {
            BarStyle.valueOf(normalized)
        } catch (_: IllegalArgumentException) {
            plugin.logger.warning("Invalid bossbar style '$raw' for an envoy, defaulting to SOLID.")
            BarStyle.SOLID
        }
    }
}
