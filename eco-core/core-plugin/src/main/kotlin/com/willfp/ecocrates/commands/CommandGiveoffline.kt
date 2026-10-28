package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.data.forEachSavedPlayer
import com.willfp.ecocrates.commands.target.GiveTarget
import com.willfp.ecocrates.commands.target.GiveTargets
import com.willfp.ecocrates.commands.target.LegacySyntax
import com.willfp.ecocrates.commands.target.ParsedTarget
import com.willfp.ecocrates.commands.target.TargetCommands
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.util.CrateKeyListener
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import java.util.logging.Level

/**
 * `/ecocrates giveoffline <type> <id> [variant] [amount]` - gives to every
 * known player. Offline players get physical items queued for when they join.
 * Walks every player with saved data off the main thread, without keeping their profiles loaded.
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

        val givenOnline = Bukkit.getOnlinePlayers().map { online ->
            // Inventory work has to happen on the player's region.
            plugin.scheduler.on(online).run { target.give(online, resolved.id, resolved.variant, resolved.amount) }
            online.uniqueId
        }.toSet()

        forEachSavedPlayer(plugin) { offline ->
            if (offline.uniqueId !in givenOnline) {
                queueAndHandToLateJoiner(offline, target, resolved)
            }
        }.exceptionally { error ->
            plugin.logger.log(Level.WARNING, "Failed giving to offline players", error)
            null
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-item-offline")
                .replace("%amount%", resolved.amount.toString())
                .replace("%item%", target.displayName(resolved.id, resolved.variant))
        )

        TargetCommands.warnIfLegacy(sender, "giveoffline", null, resolved, includeAmount = true)
    }

    private fun queueAndHandToLateJoiner(offline: OfflinePlayer, target: GiveTarget, resolved: ParsedTarget) {
        target.queue(offline, resolved.id, resolved.variant, resolved.amount)

        Bukkit.getPlayer(offline.uniqueId)?.let { joined ->
            plugin.scheduler.on(joined).run { CrateKeyListener.grantPending(joined) }
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> =
        TargetCommands.tabComplete(args, GiveTargets.giveTypes, includeAmounts = true)
}
