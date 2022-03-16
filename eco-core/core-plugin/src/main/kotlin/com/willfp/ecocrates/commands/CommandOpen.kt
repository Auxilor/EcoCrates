package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.crate.Crates
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

class CommandOpen(plugin: EcoPlugin) : Subcommand(
    plugin,
    "open",
    "ecocrates.command.open",
    true
) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player
        val crate = Crates.getByID(args[0]) ?: return

        crate.openWithKey(player)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return Crates.values().map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Crates.values().map { it.id },
                completions
            )

            return completions
        }

        return emptyList()
    }
}
