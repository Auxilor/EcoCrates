package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Keys
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

object CommandSend : Subcommand(
    plugin,
    "send",
    "ecocrates.command.send",
    true
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender as Player

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-player"))
            return
        }

        @Suppress("DEPRECATION")
        val recipient = Bukkit.getOfflinePlayer(args[0])

        if ((!recipient.hasPlayedBefore() && !recipient.isOnline) || recipient.uniqueId == sender.uniqueId) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-key"))
            return
        }

        val keyArgs = args.drop(1)
        val amountArg = if (keyArgs.size > 1) keyArgs.last().toIntOrNull()?.takeIf { it > 0 } else null
        val keyName = if (amountArg != null) keyArgs.dropLast(1).joinToString(" ") else keyArgs.joinToString(" ")
        val amount = amountArg ?: 1

        val key = Keys.values().firstOrNull { it.name.equals(keyName, ignoreCase = true) }

        if (key == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-key"))
            return
        }

        if (!key.isTradeable) {
            sender.sendMessage(plugin.langYml.getMessage("key-not-tradeable"))
            return
        }

        if (key.getVirtualKeys(sender) < amount) {
            sender.sendMessage(
                plugin.langYml.getMessage("key-trade-insufficient")
                    .replace("%key%", key.displayName)
            )
            return
        }

        key.adjustVirtualKeys(sender, -amount)
        key.adjustVirtualKeys(recipient, amount)

        sender.sendMessage(
            plugin.langYml.getMessage("key-trade-sent")
                .replace("%amount%", amount.toString())
                .replace("%key%", key.displayName)
                .replace("%player%", recipient.savedDisplayName)
        )

        recipient.player?.sendMessage(
            plugin.langYml.getMessage("key-trade-received")
                .replace("%amount%", amount.toString())
                .replace("%key%", key.displayName)
                .replace("%player%", sender.savedDisplayName)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )
            return completions
        }

        val keyNames = Keys.values()
            .filter { it.isTradeable }
            .mapNotNull { ChatColor.stripColor(it.displayName) }

        val keyArgs = args.drop(1)

        // If args so far (excluding last) form a complete key name, suggest amounts
        if (keyArgs.size > 1) {
            val namePrefix = keyArgs.dropLast(1).joinToString(" ")
            val matched = Keys.values().firstOrNull { it.name.equals(namePrefix, ignoreCase = true) }
            if (matched != null && matched.isTradeable) {
                StringUtil.copyPartialMatches(keyArgs.last(), listOf("1", "2", "3", "4", "5", "10"), completions)
                return completions
            }
        }

        StringUtil.copyPartialMatches(keyArgs.joinToString(" "), keyNames, completions)

        if (keyArgs.size > 1) {
            val prefix = keyArgs.dropLast(1).joinToString(" ") + " "
            val trimmed = completions.mapNotNull { completion ->
                if (completion.startsWith(prefix, ignoreCase = true)) completion.substring(prefix.length) else null
            }
            completions.clear()
            completions.addAll(trimmed)
        }

        completions.sort()
        return completions
    }
}