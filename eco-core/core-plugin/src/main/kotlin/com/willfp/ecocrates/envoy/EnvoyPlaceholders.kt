package com.willfp.ecocrates.envoy

import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.placeholder.PlayerlessPlaceholder
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.plugin

object EnvoyPlaceholders {
    /**
     * Formats ticks as m:ss, so a scoreboard can show a countdown directly.
     * Companion `_seconds` placeholders expose the raw number for maths.
     */
    private fun formatTicks(ticks: Int): String {
        val totalSeconds = (ticks / 20).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    private fun none() = plugin.langYml.getMessage("envoy-placeholder-none")

    fun register() {
        // ---- The active session ----

        PlayerlessPlaceholder(plugin, "envoy_active_name") {
            EnvoySessions.active?.category?.name ?: none()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_id") {
            EnvoySessions.active?.category?.id ?: ""
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active") {
            if (EnvoySessions.isActive()) "true" else "false"
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_remaining_crates") {
            EnvoySessions.remaining().toString()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_total_crates") {
            (EnvoySessions.active?.totalSpawned ?: 0).toString()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_collected_crates") {
            (EnvoySessions.active?.collectedCount ?: 0).toString()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_remaining_time") {
            EnvoySessions.active?.let { formatTicks(it.ticksRemaining) } ?: none()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_remaining_seconds") {
            ((EnvoySessions.active?.ticksRemaining ?: 0) / 20).coerceAtLeast(0).toString()
        }.register()

        // ---- Per-session leaderboard ----

        PlayerlessPlaceholder(plugin, "envoy_active_top_collector") {
            EnvoySessions.active?.topCollector?.savedDisplayName ?: none()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_active_top_collector_amount") {
            (EnvoySessions.active?.topCollectorAmount ?: 0).toString()
        }.register()

        PlayerPlaceholder(plugin, "envoy_active_collected") { player ->
            (EnvoySessions.active?.collectionsFor(player) ?: 0).toString()
        }.register()
    }

    /**
     * Registers the per-category placeholders. Called from EnvoyCategory's
     * init so they follow config reloads.
     */
    internal fun registerFor(category: EnvoyCategory) {
        PlayerlessPlaceholder(plugin, "envoy_${category.id}_time_to_start") {
            EnvoySessions.ticksUntilStart(category)?.let { formatTicks(it) } ?: none()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_${category.id}_seconds_to_start") {
            ((EnvoySessions.ticksUntilStart(category) ?: 0) / 20).coerceAtLeast(0).toString()
        }.register()

        PlayerlessPlaceholder(plugin, "envoy_${category.id}_name") {
            category.name
        }.register()
    }
}
