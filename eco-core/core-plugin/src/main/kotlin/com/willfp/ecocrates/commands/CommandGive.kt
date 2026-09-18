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
 * `/ecocrates give <player> <key|envoy|pouch> <id> [variant] [amount]`.
 * The old `give <player> <crate> [physical|virtual|flare|compass] [amount]`
 * still works, with a deprecation notice.
 */
object CommandGive : Subcommand(
    plugin,
    "give",
    "ecocrates.command.give",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        @Suppress("DEPRECATION")
        val player = Bukkit.getOfflinePlayer(args[0])

        if (!player.hasPlayedBefore() && !player.isOnline) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        val parsed = TargetCommands.parse(sender, args.drop(1), GiveTargets.giveTypes, LegacySyntax.GIVE) ?: return
        val (target, resolved) = TargetCommands.resolveGive(sender, parsed) ?: return

        if (!target.give(player, resolved.id, resolved.variant, resolved.amount)) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-item")
                .replace("%amount%", resolved.amount.toString())
                .replace("%item%", target.displayName(resolved.id, resolved.variant))
                .replace("%user%", player.savedDisplayName)
        )

        TargetCommands.warnIfLegacy(sender, "give", args[0], resolved, includeAmount = true)
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
