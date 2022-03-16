package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Crates
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

class CommandGive(plugin: EcoPlugin) : Subcommand(
    plugin,
    "give",
    "ecocrates.command.give",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(args[0])

        if (!player.hasPlayedBefore()) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }


        if (args.size < 2) {
            sender.sendMessage("must-specify-crate")
            return
        }

        val crate = Crates.getByID(args[1])

        if (crate == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            return
        }

        val amount = args.getOrNull(2)?.toIntOrNull() ?: 1

        crate.giveKeys(player, amount)
        sender.sendMessage(
            plugin.langYml.getMessage("gave-keys")
                .replace("%amount%", amount.toString())
                .replace("%crate%", crate.name)
                .replace("%user%", player.savedDisplayName)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return Crates.values().map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )

            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                Crates.values().map { it.id },
                completions
            )

            return completions
        }

        if (args.size == 3) {
            StringUtil.copyPartialMatches(
                args[2],
                listOf("1", "2", "3", "4", "5", "10"),
                completions
            )

            return completions
        }

        return emptyList()
    }
}
