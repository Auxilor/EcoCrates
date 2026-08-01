package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.envoy.EnvoyItemType
import com.willfp.ecocrates.envoy.EnvoyItems
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

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

        if (args.size < 2) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-crate"))
            return
        }

        val envoyItemType = EnvoyItemType.fromToken(args.getOrNull(2))

        if (envoyItemType != null) {
            takeEnvoyItem(sender, player, args, envoyItemType)
            return
        }

        val crate = Crates.getByID(args[1])

        if (crate == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            return
        }

        val physical = args.getOrNull(2)?.equals("physical", ignoreCase = true) == true

        val takeAmount = args.getOrNull(3)?.toIntOrNull() ?: 1

        var taken = false
        if (physical) {
            val matchingSlots = player.inventory.contents
                .filterNotNull()
                .filter { crate.sharedKey.matches(it) }
            val totalKeys = matchingSlots.sumOf { it.amount }

            if (totalKeys >= takeAmount) {
                var remaining = takeAmount
                for (item in player.inventory.contents) {
                    if (remaining <= 0) break
                    if (item != null && crate.sharedKey.matches(item)) {
                        val toRemove = minOf(remaining, item.amount)
                        item.amount -= toRemove
                        if (item.amount == 0) {
                            item.type = Material.AIR
                        }
                        remaining -= toRemove
                    }
                }
                taken = true
            }
        } else {
            if (crate.getVirtualKeys(player) >= takeAmount) {
                crate.adjustVirtualKeys(player, takeAmount * -1)
                taken = true
            }
        }

        if (taken) {
            sender.sendMessage(
                plugin.langYml.getMessage("took-keys")
                    .replace("%amount%", takeAmount.toString())
                    .replace("%crate%", crate.name)
                    .replace("%user%", player.savedDisplayName)
            )
        } else {
            sender.sendMessage(
                plugin.langYml.getMessage("not-enough-took-keys")
                    .replace("%crate%", crate.name)
                    .replace("%user%", player.savedDisplayName)
            )
        }
    }

    private fun takeEnvoyItem(
        sender: CommandSender,
        player: Player,
        args: List<String>,
        type: EnvoyItemType
    ) {
        val category = Envoys[args[1]]

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

        val amount = (args.getOrNull(3)?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val displayName = item.itemMeta?.displayName ?: type.token

        // All-or-nothing, matching how taking keys already behaves.
        val taken = EnvoyItems.take(player, category, type, amount)

        val message = if (taken) "took-envoy-item" else "not-enough-took-envoy-item"

        sender.sendMessage(
            plugin.langYml.getMessage(message)
                .replace("%amount%", amount.toString())
                .replace("%item%", displayName)
                .replace("%user%", player.savedDisplayName)
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
                Bukkit.getOnlinePlayers().map { it.name },
                completions
            )

            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(
                args[1],
                Crates.values().map { it.id } + Envoys.values().map { it.id },
                completions
            )

            return completions
        }

        if (args.size == 3) {
            StringUtil.copyPartialMatches(
                args[2],
                listOf("physical", "virtual") + EnvoyItemType.tokens,
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