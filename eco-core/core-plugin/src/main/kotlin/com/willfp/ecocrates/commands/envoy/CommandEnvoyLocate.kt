package com.willfp.ecocrates.commands.envoy

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.util.StringUtils
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.withEnvoyPlaceholders
import com.willfp.ecocrates.plugin
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * `/ecocrates envoy locate [number]` - lists the active envoy session's crate
 * spawns as clickable teleport commands, or teleports the sender directly to
 * one when given its number.
 */
object CommandEnvoyLocate : Subcommand(
    plugin,
    "locate",
    "ecocrates.command.envoy.locate",
    true
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        sender as Player

        val session = EnvoySessions.active

        if (session == null) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-not-active"))
            return
        }

        val spawns = session.spawns

        if (spawns.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("envoy-locate-empty"))
            return
        }

        val index = args.getOrNull(0)?.toIntOrNull()

        if (index != null) {
            val spawn = spawns.getOrNull(index - 1)

            if (spawn == null) {
                sender.sendMessage(plugin.langYml.getMessage("envoy-locate-empty"))
                return
            }

            sender.teleport(spawn.centeredLocation)
            sender.sendMessage(
                plugin.langYml.getMessage("envoy-teleported")
                    .replace("%number%", index.toString())
            )
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("envoy-locate-header")
                .withEnvoyPlaceholders(session.category)
        )

        for ((i, spawn) in spawns.withIndex()) {
            val number = i + 1
            val location = spawn.blockLocation

            val line = plugin.langYml.getMessage("envoy-locate-line", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%number%", number.toString())
                .withEnvoyPlaceholders(rarity = spawn.rarity)
                .replace("%world%", location.world?.name ?: "?")
                .replace("%x%", location.blockX.toString())
                .replace("%y%", location.blockY.toString())
                .replace("%z%", location.blockZ.toString())

            val component = TextComponent(*TextComponent.fromLegacyText(StringUtils.format(line)))
            component.clickEvent = ClickEvent(
                ClickEvent.Action.RUN_COMMAND,
                "/ecocrates envoy locate $number"
            )

            sender.spigot().sendMessage(component)
        }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> = emptyList()
}
