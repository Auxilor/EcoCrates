package com.willfp.ecocrates.commands.envoy

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandEnvoyStart : Subcommand(
    plugin,
    "start",
    "ecocrates.command.envoy.start",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-envoy"))
            return
        }

        val category = Envoys[args[0]]

        if (category == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-envoy"))
            return
        }

        if (EnvoySessions.isActive()) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-already-active"))
            return
        }

        if (!EnvoySessions.start(category)) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-start-failed"))
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("envoy-started")
                .withEnvoyPlaceholders(category)
                .replace("%amount%", EnvoySessions.remaining().toString())
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size <= 1) {
            StringUtil.copyPartialMatches(
                args.getOrElse(0) { "" },
                Envoys.values().map { it.id },
                completions
            )
        }

        return completions
    }
}
