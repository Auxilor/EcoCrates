package com.willfp.ecocrates.envoy.session

import org.bukkit.Bukkit
import org.bukkit.boss.BossBar
import java.util.UUID

/**
 * Renders the single active session's bossbar, if its category has one configured and
 * enabled. Only one envoy session can run at a time, so this only ever tracks one bar -
 * unlike Boosters' BoosterBossBarManager, which tracks one bar per concurrently active
 * booster.
 */
object EnvoyBossBarManager {
    private var bar: BossBar? = null
    private var barCategoryId: String? = null

    // BossBar#getPlayers() returns a List, so checking membership against it directly is
    // O(n) per player and O(n^2) overall for the add/remove pass below. Mirroring
    // membership in a Set keeps that at O(1) per player instead.
    private val shownTo = mutableSetOf<UUID>()

    @Volatile
    private var tick = 0

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

        // The countdown only has second, not tick, resolution, so rebuilding the title
        // string and re-setting progress every single tick is wasted work - once every
        // 20 ticks (once a second) is all it can ever visibly change.
        if (tick % 20 == 0) {
            activeBar.setTitle(bossbar.getName(session.category, session))
            activeBar.color = bossbar.color
            activeBar.style = bossbar.style
            activeBar.progress = bossbar.getProgress(session.category, session)
        }
        tick++

        val onlinePlayers = Bukkit.getOnlinePlayers().toSet()

        for (player in onlinePlayers) {
            if (shownTo.add(player.uniqueId)) {
                activeBar.addPlayer(player)
            }
        }

        val onlineIds = onlinePlayers.mapTo(mutableSetOf()) { it.uniqueId }

        for (uuid in shownTo.toList()) {
            if (uuid !in onlineIds) {
                shownTo.remove(uuid)
                val player = Bukkit.getPlayer(uuid) ?: continue
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
        shownTo.clear()
        tick = 0
    }
}
