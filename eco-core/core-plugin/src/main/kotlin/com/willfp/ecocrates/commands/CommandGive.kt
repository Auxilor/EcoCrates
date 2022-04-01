package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Crates
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.StringUtil

class CommandGive(plugin: EcoPlugin) : Subcommand(
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

        val players = if (args[0].equals("all", true)) {
            Bukkit.getOnlinePlayers().toList()
        } else {
            mutableListOf(Bukkit.getPlayer(args[0])).filterNotNull()
        }

        if (players.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-player"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage("must-specify-crate")
            return
        }

        val crate = Crates.getByID(args[1])

        if (crate == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            return
        }

        val physical = args.getOrNull(2)?.equals("physical", ignoreCase = true) == true

        val amount = args.getOrNull(3)?.toIntOrNull() ?: 1

        if (physical) {
            val items = mutableListOf<ItemStack>().apply { repeat(amount) { add(crate.key.item) } }

            players.forEach { player ->
                run {
                    if (plugin.configYml.getBool("track-player-keys")) {
                        items.map {
                            val meta = it.itemMeta!!
                            meta.persistentDataContainer.set(
                                plugin.namespacedKeyFactory.create("player"),
                                PersistentDataType.STRING,
                                player.uniqueId.toString()
                            )
                            it.itemMeta = meta
                        }
                    }

                    DropQueue(player)
                        .addItems(items)
                        .forceTelekinesis()
                        .push()
                }
            }
        } else {
            players.forEach { player ->
                run {
                    crate.adjustVirtualKeys(player, amount)
                }
            }
        }

        val replacement = if (players.size > 1) {
            "everyone"
        } else {
            players.first().savedDisplayName
        }

        sender.sendMessage(
            plugin.langYml.getMessage("gave-keys")
                .replace("%amount%", amount.toString())
                .replace("%crate%", crate.name)
                .replace("%user%", replacement)
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
                Bukkit.getOnlinePlayers().map { it.name }.toMutableList().apply { this.add("all") },
                completions
            )

            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                Crates.values().map { it.id },
                completions
            )

            return completions
        }

        if (args.size == 3) {
            StringUtil.copyPartialMatches(
                args[2],
                listOf("physical", "virtual"),
                completions
            )

            return completions
        }

        if (args.size == 4) {
            StringUtil.copyPartialMatches(
                args[3],
                listOf("1", "2", "3", "4", "5", "10"),
                completions
            )

            return completions
        }

        return emptyList()
    }
}
