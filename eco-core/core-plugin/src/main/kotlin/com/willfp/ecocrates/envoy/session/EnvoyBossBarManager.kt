package com.willfp.ecocrates.envoy.session

import org.bukkit.Bukkit
import org.bukkit.boss.BossBar

/**
 * Renders the single active session's bossbar, if its category has one configured and
 * enabled. Only one envoy session can run at a time, so this only ever tracks one bar -
 * unlike Boosters' BoosterBossBarManager, which tracks one bar per concurrently active
 * booster.
 */
object EnvoyBossBarManager {
    private var bar: BossBar? = null
    private var barCategoryId: String? = null

    fun refresh() {
        val session = EnvoySessions.active
        val bossbar = session?.category?.bossbar

        if (session == null || bossbar == null) {
            clear()
            return
        }

        // The session changed under us (a new one started before the old bar was cleared,
        // or this category's bossbar config changed) - drop the stale bar and start fresh.
        if (barCategoryId != session.category.id) {
            clear()
        }

        val activeBar = bar ?: Bukkit.createBossBar(
            bossbar.getName(session.category, session),
            bossbar.color,
            bossbar.style
        ).also {
            bar = it
            barCategoryId = session.category.id
        }

        activeBar.setTitle(bossbar.getName(session.category, session))
        activeBar.color = bossbar.color
        activeBar.style = bossbar.style
        activeBar.progress = bossbar.getProgress(session.category, session)

        val onlinePlayers = Bukkit.getOnlinePlayers().toSet()

        for (player in onlinePlayers) {
            if (!activeBar.players.contains(player)) {
                activeBar.addPlayer(player)
            }
        }

        for (player in activeBar.players.toList()) {
            if (player !in onlinePlayers) {
                activeBar.removePlayer(player)
            }
        }
    }

    /** Removes the bar entirely. Called when a session ends, whether by timeout, the last
     *  crate being collected, or the plugin disabling/reloading. */
    fun clear() {
        bar?.removeAll()
        bar?.isVisible = false
        bar = null
        barCategoryId = null
    }
}
