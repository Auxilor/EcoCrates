package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.envoy.EnvoyItemType
import com.willfp.ecocrates.envoy.EnvoyItems
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
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

        val envoyItemType = EnvoyItemType.fromToken(args.getOrNull(1))

        if (envoyItemType != null) {
            giveEnvoyItemOffline(sender, args, envoyItemType)
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

    private fun giveEnvoyItemOffline(sender: CommandSender, args: List<String>, type: EnvoyItemType) {
        val category = Envoys[args[0]]

        if (category == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-envoy"))
            return
        }

        val item = EnvoyItems.itemFor(category, type)

        if (item == null) {
            sender.sendMessage(
                plugin.langYml.getMessage("envoy-item-not-enabled").withEnvoyPlaceholders(category)
            )
            return
        }

        val amount = (args.getOrNull(2)?.toIntOrNull() ?: 1).coerceAtLeast(1)

        plugin.scheduler.runAsync {
            for (offline in Bukkit.getOfflinePlayers()) {
                val online = offline.player

                if (online != null) {
                    // Inventory work has to happen on the main thread.
                    plugin.scheduler.run { EnvoyItems.give(online, category, type, amount) }
                } else {
                    EnvoyItems.adjustToGet(offline, category, type, amount)
                }
            }
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-envoy-item-offline")
                .replace("%amount%", amount.toString())
                .replace("%item%", item.itemMeta?.displayName ?: type.token)
                .withEnvoyPlaceholders(category)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.isEmpty()) {
            return Crates.values().map { it.id }
        }

        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Crates.values().map { it.id } + Envoys.values().map { it.id },
                completions
            )

            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                listOf("physical", "virtual") + EnvoyItemType.tokens,
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


