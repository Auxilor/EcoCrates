package com.willfp.ecocrates.commands.envoy

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender

object CommandEnvoyEnd : Subcommand(
    plugin,
    "end",
    "ecocrates.command.envoy.end",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val session = EnvoySessions.active

        if (session == null) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-not-active"))
            return
        }

        // Captured before end(), which clears the active session.
        val category = session.category
        EnvoySessions.end()

        sender.sendMessage(
            plugin.langYml.getMessage("envoy-ended").withEnvoyPlaceholders(category)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
