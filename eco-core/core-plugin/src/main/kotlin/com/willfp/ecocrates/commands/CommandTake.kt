package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.commands.target.GiveTargets
import com.willfp.ecocrates.commands.target.LegacySyntax
import com.willfp.ecocrates.commands.target.TargetCommands
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

/**
 * `/ecocrates take <player> <type> <id> [variant] [amount]`. All-or-nothing:
 * takes nothing if the player doesn't have enough.
 */
object CommandTake : Subcommand(
    plugin,
    "take",
    "ecocrates.command.take",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        val player = Bukkit.getPlayer(args[0])

        if (player == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val parsed = TargetCommands.parse(sender, args.drop(1), GiveTargets.giveTypes, LegacySyntax.GIVE) ?: return
        val (target, resolved) = TargetCommands.resolveGive(sender, parsed) ?: return

        val taken = target.take(player, resolved.id, resolved.variant, resolved.amount)

        sender.sendMessage(
            plugin.langYml.getMessage(if (taken) "took-item" else "not-enough-took-item")
                .replace("%amount%", resolved.amount.toString())
                .replace("%item%", target.displayName(resolved.id, resolved.variant))
                .replace("%user%", player.savedDisplayName)
        )

        TargetCommands.warnIfLegacy(sender, "take", args[0], resolved, includeAmount = true)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (args.size <= 1) {
            return StringUtil.copyPartialMatches(
                args.firstOrNull() ?: "",
                Bukkit.getOnlinePlayers().map { it.name },
                mutableListOf()
            )
        }

        return TargetCommands.tabComplete(args.drop(1), GiveTargets.giveTypes, includeAmounts = true)
    }
}
