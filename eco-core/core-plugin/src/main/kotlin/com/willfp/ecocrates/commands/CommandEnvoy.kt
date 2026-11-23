package com.willfp.ecocrates.commands

import com.willfp.ecocrates.commands.envoy.CommandEnvoyEnd
import com.willfp.ecocrates.commands.envoy.CommandEnvoyLocate
import com.willfp.ecocrates.commands.envoy.CommandEnvoySet
import com.willfp.ecocrates.commands.envoy.CommandEnvoyStart
import com.willfp.ecocrates.commands.envoy.CommandEnvoyUnset
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

/** `/ecocrates envoy` subcommand; groups the set/unset/locate/start/end envoy admin commands. */
internal fun CommandBuilder<CommandSender>.envoyCommand() = subcommand(
    name = "envoy",
    permission = "ecocrates.command.envoy"
) {
    mountEnvoySubcommands()
}

internal fun CommandBuilder<CommandSender>.mountEnvoySubcommands() = mount(
    CommandEnvoySet,
    CommandEnvoyUnset,
    CommandEnvoyLocate,
    CommandEnvoyStart,
    CommandEnvoyEnd
)
