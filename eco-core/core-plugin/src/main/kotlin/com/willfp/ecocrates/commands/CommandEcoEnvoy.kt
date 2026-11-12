package com.willfp.ecocrates.commands

import com.willfp.ecocrates.plugin
import wiki.ecohub.core.command.EcoCommand
import wiki.ecohub.core.command.command

/**
 * `/ecoenvoy <sub>` is an alias for `/ecocrates envoy <sub>`, mounting the
 * exact same subcommand objects.
 */
internal fun ecoEnvoyCommand(): EcoCommand =
    plugin.command(name = "ecoenvoy", permission = "ecocrates.command.envoy") {
        mountEnvoySubcommands()
    }
