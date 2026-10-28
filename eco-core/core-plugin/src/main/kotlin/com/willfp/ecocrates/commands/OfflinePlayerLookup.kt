package com.willfp.ecocrates.commands

import com.willfp.eco.core.Prerequisite
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Find the player called [name], then run [action] with them.
 *
 * An online player, or one the server has cached, is found at once and [action] runs straight
 * away. Anyone else needs a lookup that can wait on the network, so that runs off the main thread
 * and [action] follows once it is done: on [sender]'s region when [sender] is a player still
 * online, otherwise on the global region.
 */
internal fun lookupOfflinePlayer(sender: CommandSender, name: String, action: (OfflinePlayer) -> Unit) {
    val known = Bukkit.getPlayerExact(name) ?: cachedOfflinePlayer(name)

    if (known != null) {
        action(known)
        return
    }

    plugin.scheduler.async().run {
        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(name)

        if (sender is Player) {
            plugin.scheduler.on(sender).run {
                if (sender.isOnline) {
                    action(player)
                }
            }
        } else {
            plugin.scheduler.global().run { action(player) }
        }
    }
}

private fun cachedOfflinePlayer(name: String): OfflinePlayer? =
    if (Prerequisite.HAS_PAPER.isMet) Bukkit.getOfflinePlayerIfCached(name) else null
