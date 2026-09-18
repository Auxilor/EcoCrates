package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.commands.target.GiveTargets
import com.willfp.ecocrates.commands.target.LegacySyntax
import com.willfp.ecocrates.commands.target.TargetCommands
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/** `/ecocrates giveall <type> <id> [variant] [amount]` - gives to every online player. */
object CommandGiveall : Subcommand(
    plugin,
    "giveall",
    "ecocrates.command.giveall",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        val parsed = TargetCommands.parse(sender, args, GiveTargets.giveTypes, LegacySyntax.GIVE) ?: return
        val (target, resolved) = TargetCommands.resolveGive(sender, parsed) ?: return

        for (player in Bukkit.getOnlinePlayers()) {
            target.give(player, resolved.id, resolved.variant, resolved.amount)
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-item-all")
                .replace("%amount%", resolved.amount.toString())
                .replace("%item%", target.displayName(resolved.id, resolved.variant))
        )

        TargetCommands.warnIfLegacy(sender, "giveall", null, resolved, includeAmount = true)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> =
        TargetCommands.tabComplete(args, GiveTargets.giveTypes, includeAmounts = true)
}
