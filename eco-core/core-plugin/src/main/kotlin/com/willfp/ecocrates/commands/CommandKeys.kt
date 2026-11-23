package com.willfp.ecocrates.commands

import com.willfp.ecocrates.crate.KeyGUI
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object CommandKeys : EcoCratesSubcommand(
    "keys",
    "ecocrates.command.keys",
    true
) {
    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player
        KeyGUI.open(player)
    }
}
