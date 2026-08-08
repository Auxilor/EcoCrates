package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.ecocrates.commands.envoy.CommandEnvoyEnd
import com.willfp.ecocrates.commands.envoy.CommandEnvoyLocate
import com.willfp.ecocrates.commands.envoy.CommandEnvoySet
import com.willfp.ecocrates.commands.envoy.CommandEnvoyStart
import com.willfp.ecocrates.commands.envoy.CommandEnvoyUnset
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender

/**
 * `/ecoenvoy <sub>` is an alias for `/ecocrates envoy <sub>`, mounting the
 * exact same subcommand objects.
 */
object CommandEcoEnvoy : PluginCommand(
    plugin,
    "ecoenvoy",
    "ecocrates.command.envoy",
    false
) {
    init {
        this.addSubcommand(CommandEnvoySet)
            .addSubcommand(CommandEnvoyUnset)
            .addSubcommand(CommandEnvoyLocate)
            .addSubcommand(CommandEnvoyStart)
            .addSubcommand(CommandEnvoyEnd)
    }

    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender.sendMessage(plugin.langYml.getMessage("invalid-command"))
    }
}
