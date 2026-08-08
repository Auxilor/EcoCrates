package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.commands.envoy.CommandEnvoyEnd
import com.willfp.ecocrates.commands.envoy.CommandEnvoyLocate
import com.willfp.ecocrates.commands.envoy.CommandEnvoySet
import com.willfp.ecocrates.commands.envoy.CommandEnvoyStart
import com.willfp.ecocrates.commands.envoy.CommandEnvoyUnset
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender

/** `/ecocrates envoy` subcommand; groups the set/unset/locate/start/end envoy admin commands. */
object CommandEnvoy : Subcommand(
    plugin,
    "envoy",
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
