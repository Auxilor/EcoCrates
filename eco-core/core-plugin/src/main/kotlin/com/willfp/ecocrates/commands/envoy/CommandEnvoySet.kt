package com.willfp.ecocrates.commands.envoy

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.spawn.EnvoyPoints
import com.willfp.ecocrates.envoy.spawn.SpawnLocationMode
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

/**
 * `/ecocrates envoy set <category> [current|x y z]` - adds a spawn point for a
 * points-mode envoy category, targeting the block the player is looking at by
 * default.
 */
object CommandEnvoySet : Subcommand(
    plugin,
    "set",
    "ecocrates.command.envoy.set",
    true
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender as Player

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-envoy"))
            return
        }

        val category = Envoys[args[0]]

        if (category == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-envoy"))
            return
        }

        if (category.locationMode != SpawnLocationMode.POINTS) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-not-points-mode"))
            return
        }

        val location = resolveLocation(sender, args) ?: return
        val number = EnvoyPoints.add(category.id, location)

        sender.sendMessage(
            plugin.langYml.getMessage("envoy-point-set")
                .replace("%number%", number.toString())
                .withEnvoyPlaceholders(category)
        )
    }

    /**
     * Either the block the player is looking at ("current" or no argument),
     * or explicit x y z coordinates in the player's own world.
     */
    private fun resolveLocation(player: Player, args: List<String>): Location? {
        val useCurrent = args.size < 2 || args[1].equals("current", ignoreCase = true)

        if (useCurrent) {
            val block = player.getTargetBlockExact(10)

            if (block == null) {
                player.sendMessage(plugin.langYml.getMessage("must-target-block"))
                return null
            }

            return block.location
        }

        if (args.size < 4) {
            player.sendMessage(plugin.langYml.getMessage("invalid-coordinates"))
            return null
        }

        val x = args[1].toIntOrNull()
        val y = args[2].toIntOrNull()
        val z = args[3].toIntOrNull()

        if (x == null || y == null || z == null) {
            player.sendMessage(plugin.langYml.getMessage("invalid-coordinates"))
            return null
        }

        return Location(player.world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val completions = mutableListOf<String>()

        if (args.size <= 1) {
            StringUtil.copyPartialMatches(
                args.getOrElse(0) { "" },
                Envoys.values()
                    .filter { it.locationMode == SpawnLocationMode.POINTS }
                    .map { it.id },
                completions
            )
            return completions
        }

        if (args.size == 2) {
            StringUtil.copyPartialMatches(args[1], listOf("current"), completions)
            return completions
        }

        return emptyList()
    }
}
