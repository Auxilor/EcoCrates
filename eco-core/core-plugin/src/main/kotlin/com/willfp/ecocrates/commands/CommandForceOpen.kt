package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.OpenMethod
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandForceOpen(plugin: EcoPlugin) : Subcommand(
    plugin,
    "forceopen",
    "ecocrates.command.forceopen",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.size < 2) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-crate"))
            return
        }

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        val player = Bukkit.getPlayer(args[0])

        if (player == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val crate = getCrateById(args[1])

        if (crate == null) {
            player.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            return
        }

        crate.open(player, OpenMethod.OTHER)
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
                args[0],
                Crates.values().map { it.id },
                completions
            )

            return completions
        }

        return emptyList()
    }

    private fun getCrateById(id: String): Crate? {
        return Crates.values().firstOrNull { it.id.equals(id, true) }
    }
}
