package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.crate.KeyGUI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandKeys(plugin: EcoPlugin) : Subcommand(
    plugin,
    "keys",
    "ecocrates.command.keys",
    true
) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player
        KeyGUI.open(player)
    }
}
