package com.willfp.ecocrates.commands

import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder

/** An `/ecocrates` or `/ecoenvoy` subcommand, mounted on the eco command DSL with [mount]. */
abstract class EcoCratesSubcommand(
    val name: String,
    val permission: String,
    val playersOnly: Boolean
) {
    abstract fun onExecute(sender: CommandSender, args: List<String>)

    open fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}

internal fun CommandBuilder<CommandSender>.mount(vararg commands: EcoCratesSubcommand) {
    for (command in commands) {
        if (command.playersOnly) {
            playerSubcommand(name = command.name, permission = command.permission) {
                executes { command.onExecute(sender, args) }
                completes { command.tabComplete(sender, args) }
            }
        } else {
            subcommand(name = command.name, permission = command.permission) {
                executes { command.onExecute(sender, args) }
                completes { command.tabComplete(sender, args) }
            }
        }
    }
}
