package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender

object CommandEcoCrates : PluginCommand(
    plugin,
    "ecocrates",
    "ecocrates.command.ecocrates",
    false
) {
    init {
        this.addSubcommand(CommandReload)
            .addSubcommand(CommandOpen)
            .addSubcommand(CommandPreview)
            .addSubcommand(CommandGive)
            .addSubcommand(CommandKeys)
            .addSubcommand(CommandSet)
            .addSubcommand(CommandForceOpen)
            .addSubcommand(CommandResetWins)
            .addSubcommand(CommandConvert)
            .addSubcommand(CommandGiveall)
            .addSubcommand(CommandTake)
            .addSubcommand(CommandGiveoffline)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("invalid-command"))
    }
}
