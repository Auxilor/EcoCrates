package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.commands.target.GiveTargets
import com.willfp.ecocrates.commands.target.LegacySyntax
import com.willfp.ecocrates.commands.target.TargetCommands
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/**
 * `/ecocrates giveoffline <type> <id> [variant] [amount]` - gives to every
 * known player. Offline players get physical items queued for when they join.
 * Walks all offline players off the main thread.
 */
object CommandGiveoffline : Subcommand(
    plugin,
    "giveoffline",
    "ecocrates.command.giveoffline",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val parsed = TargetCommands.parse(sender, args, GiveTargets.giveTypes, LegacySyntax.GIVE) ?: return
        val (target, resolved) = TargetCommands.resolveGive(sender, parsed) ?: return

        plugin.scheduler.runAsync {
            for (offline in Bukkit.getOfflinePlayers()) {
                val online = offline.player

                if (online != null) {
                    // Inventory work has to happen on the main thread.
                    plugin.scheduler.run { target.give(online, resolved.id, resolved.variant, resolved.amount) }
                } else {
                    target.queue(offline, resolved.id, resolved.variant, resolved.amount)
                }
            }
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-item-offline")
                .replace("%amount%", resolved.amount.toString())
                .replace("%item%", target.displayName(resolved.id, resolved.variant))
        )

        TargetCommands.warnIfLegacy(sender, "giveoffline", null, resolved, includeAmount = true)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> =
        TargetCommands.tabComplete(args, GiveTargets.giveTypes, includeAmounts = true)
}
