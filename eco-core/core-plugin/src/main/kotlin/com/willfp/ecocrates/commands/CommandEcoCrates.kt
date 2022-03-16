package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import org.bukkit.command.CommandSender

class CommandEcoCrates(plugin: EcoPlugin) : PluginCommand(
    plugin,
    "ecocrates",
    "ecocrates.command.ecocrates",
    true
) {
    init {
        this.addSubcommand(CommandReload(plugin))
            .addSubcommand(CommandOpen(plugin))
            .addSubcommand(CommandPreview(plugin))
            .addSubcommand(CommandGive(plugin))
            .addSubcommand(CommandKeys(plugin))
            .addSubcommand(CommandSet(plugin))
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("invalid-command"))
    }
}
