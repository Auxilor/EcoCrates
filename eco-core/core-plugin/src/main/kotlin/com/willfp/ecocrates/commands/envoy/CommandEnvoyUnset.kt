package com.willfp.ecocrates.commands.envoy

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.spawn.EnvoyPoints
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandEnvoyUnset : Subcommand(
    plugin,
    "unset",
    "ecocrates.command.envoy.set",
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

        val number = args.getOrNull(1)?.toIntOrNull()

        if (number == null) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-point"))
            return
        }

        if (!EnvoyPoints.remove(category.id, number)) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-point-not-found"))
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("envoy-point-unset")
                .replace("%number%", number.toString())
                .withEnvoyPlaceholders(category)
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
            return completions
        }

        if (args.size == 2) {
            val category = Envoys[args[0]] ?: return emptyList()

            StringUtil.copyPartialMatches(
                args[1],
                EnvoyPoints.get(category.id).keys.map { it.toString() },
                completions
            )
            return completions
        }

        return emptyList()
    }
}
