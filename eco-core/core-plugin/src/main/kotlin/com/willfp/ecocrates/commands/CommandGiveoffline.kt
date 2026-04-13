package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.StringUtil

object CommandGiveoffline : Subcommand(
    plugin,
    "giveoffline",
    "ecocrates.command.giveoffline",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-crate"))
            return
        }

        val crate = Crates.getByID(args[0])

        if (crate == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            return
        }

        val physical = args.getOrNull(1)?.equals("physical", ignoreCase = true) == true

        val amount = args.getOrNull(2)?.toIntOrNull() ?: 1

        plugin.scheduler.runAsync { giveOfflineAll(physical, crate, amount) }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-keys-all-offline")
                .replace("%amount%", amount.toString())
                .replace("%crate%", crate.name)
        )
    }

    private fun giveOfflineAll(physical: Boolean, crate: Crate, amount: Int) {
        for (player in Bukkit.getOfflinePlayers()) {
            if (physical) {
                val online = player.player
                if (online != null) {
                    plugin.scheduler.run {
                        giveOnline(online, crate, amount)
                    }
                } else {
                    crate.adjustKeysToGet(player, amount)
                }
            } else {
                crate.adjustVirtualKeys(player, amount)
            }
        }
    }

    private fun giveOnline(player: Player, crate: Crate, amount: Int) {
        val items = mutableListOf<ItemStack>().apply {
            repeat(amount) { add(crate.sharedKey.createItem(player)) }
        }

        DropQueue(player)
            .addItems(items)
            .forceTelekinesis()
            .push()
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return Crates.values().map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Crates.values().map { it.id },
                completions
            )

            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                listOf("physical", "virtual"),
                completions
            )

            return completions
        }

        if (args.size == 3) {
            StringUtil.copyPartialMatches(
                args[2],
                listOf("1", "2", "3", "4", "5", "10"),
                completions
            )

            return completions
        }

        return emptyList()
    }
}


