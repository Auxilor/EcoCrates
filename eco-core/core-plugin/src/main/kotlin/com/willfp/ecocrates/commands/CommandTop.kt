package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.eco.core.leaderboard.Leaderboard
import com.willfp.eco.core.leaderboard.Leaderboards
import com.willfp.eco.core.placeholder.context.placeholderContext
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil

/** `/ecocrates top <crate> [page]`: shows who has opened a crate the most. */
object CommandTop : Subcommand(
    plugin,
    "top",
    "ecocrates.command.top",
    false
) {
    private const val PAGE_SIZE = 10

    private val pagePattern = Regex("\\d*")

    override fun onExecute(sender: CommandSender, args: List<String>) {
        plugin.scheduler.async().run { showTop(sender, args) }
    }

    private fun showTop(sender: CommandSender, args: List<String>) {
        val crate = Crates.getByID(args.getOrNull(0))
        val leaderboard = crate?.leaderboard
        val page = args.getOrNull(1)?.trim().orEmpty()

        when {
            crate == null || leaderboard == null -> sender.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            !pagePattern.matches(page) -> sender.sendMessage(plugin.langYml.getMessage("invalid-page"))
            else -> sendPage(sender, crate.name, leaderboard, page.toIntOrNull() ?: 1)
        }
    }

    private fun sendPage(sender: CommandSender, crateName: String, leaderboard: Leaderboard, page: Int) {
        val offset = (page.coerceAtLeast(1) - 1) * PAGE_SIZE
        val snapshot = leaderboard.snapshot

        val lines = (offset + 1..offset + PAGE_SIZE).asSequence()
            .mapNotNull { snapshot.getEntry(it) }
            .map { entry ->
                plugin.langYml.getString("top-line-format")
                    .replace(oldValue = "%rank%", newValue = entry.position.toString())
                    .replace(oldValue = "%opens%", newValue = entry.value.toInt().toString())
                    .replace(oldValue = "%player%", newValue = entry.player.savedDisplayName)
            }
            .toList()
            .ifEmpty { listOf(Leaderboards.getNoRecordsMessage(plugin)) }

        plugin.langYml.getStrings("top.format")
            .flatMap { if (it == "%lines%") lines else listOf(it.replace(oldValue = "%crate%", newValue = crateName)) }
            .forEach { sender.sendMessage(it.formatEco(placeholderContext(player = sender as? Player))) }
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        if (!plugin.configYml.getBool("leaderboard.enabled")) {
            return emptyList()
        }

        return when {
            args.size == 1 -> StringUtil.copyPartialMatches(args[0], Crates.values().map { it.id }, mutableListOf())
            args.size == 2 && Crates.getByID(args[0]) != null ->
                StringUtil.copyPartialMatches(args[1], listOf("1", "2", "3", "4", "5"), mutableListOf())
            else -> emptyList()
        }
    }
}
