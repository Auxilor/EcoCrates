package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.data.forEachSavedProfile
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandResetWins : Subcommand(
    plugin,
    "resetwins",
    "ecocrates.command.resetwins",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        if (args[0] == "all") {
            resetWinsForAll(sender)
        } else {
            lookupOfflinePlayer(sender, args[0]) { resetWinsFor(sender, it) }
        }
    }

    private fun resetWinsForAll(sender: CommandSender) {
        sender.sendMessage(plugin.langYml.getMessage("resetting-wins").replace("%user%", "all"))

        forEachSavedProfile(plugin) { uuid ->
            resetWins(Bukkit.getOfflinePlayer(uuid))
        }.thenRun {
            plugin.scheduler.global().run {
                sender.sendMessage(plugin.langYml.getMessage("reset-wins").replace("%user%", "all"))
            }
        }
    }

    private fun resetWinsFor(sender: CommandSender, player: OfflinePlayer) {
        if (!player.hasPlayedBefore() && !player.isOnline) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        sender.sendMessage(plugin.langYml.getMessage("resetting-wins").replace("%user%", player.savedDisplayName))

        resetWins(player)

        sender.sendMessage(plugin.langYml.getMessage("reset-wins").replace("%user%", player.savedDisplayName))
    }

    private fun resetWins(player: OfflinePlayer) {
        for (crate in Crates.values()) {
            crate.rewards.forEach { it.resetWins(player) }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return Bukkit.getOnlinePlayers().map { it.name }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name } union listOf("all"),
                completions
            )

            return completions
        }

        return emptyList()
    }
}
