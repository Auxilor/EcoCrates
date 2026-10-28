package com.willfp.ecocrates.commands

import com.willfp.eco.core.Prerequisite
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

/**
 * Find the player called [name], then run [action] with them.
 *
 * An online player, or one the server has cached, is found at once and [action] runs straight
 * away. Anyone else needs a lookup that can wait on the network, so that runs off the main thread
 * and [action] follows on the global region once it is done.
 */
internal fun lookupOfflinePlayer(name: String, action: (OfflinePlayer) -> Unit) {
    val known = Bukkit.getPlayerExact(name) ?: cachedOfflinePlayer(name)

    if (known != null) {
        action(known)
        return
    }

    plugin.scheduler.async().run {
        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(name)

        plugin.scheduler.global().run { action(player) }
    }
}

private fun cachedOfflinePlayer(name: String): OfflinePlayer? =
    if (Prerequisite.HAS_PAPER.isMet) Bukkit.getOfflinePlayerIfCached(name) else null
