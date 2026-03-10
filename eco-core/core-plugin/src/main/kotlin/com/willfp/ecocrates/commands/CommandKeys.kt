package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.crate.KeyGUI
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandKeys : Subcommand(
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
