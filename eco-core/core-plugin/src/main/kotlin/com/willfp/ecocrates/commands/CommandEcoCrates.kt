package com.willfp.ecocrates.commands

import com.willfp.ecocrates.plugin
import wiki.ecohub.core.command.EcoCommand
import wiki.ecohub.core.command.command

/** Root `/ecocrates` command; dispatches to all crate-related subcommands. */
internal fun ecoCratesCommand(): EcoCommand =
    plugin.command(name = "ecocrates", permission = "ecocrates.command.ecocrates") {
        mount(
            CommandReload,
            CommandOpen,
            CommandPreview,
            CommandGive,
            CommandKeys,
            CommandSet,
            CommandForceOpen,
            CommandResetWins,
            CommandResetTotalWins,
            CommandConvert,
            CommandGiveall,
            CommandTake,
            CommandGiveoffline,
            CommandSend
        )
        envoyCommand()
    }
