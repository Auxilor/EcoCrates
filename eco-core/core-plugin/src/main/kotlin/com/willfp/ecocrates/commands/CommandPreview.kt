package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.commands.target.LegacySyntax
import com.willfp.ecocrates.commands.target.TargetCommands
import com.willfp.ecocrates.commands.target.TargetType
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.pouch.Pouches
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** `/ecocrates preview <crate|pouch> <id>`. The old `preview <crate>` still works. */
object CommandPreview : Subcommand(
    plugin,
    "preview",
    "ecocrates.command.preview",
    true
) {
    private val previewTypes = setOf(TargetType.CRATE, TargetType.POUCH)

    override fun onExecute(player: CommandSender, args: List<String>) {
        player as Player

        val target = TargetCommands.parse(player, args, previewTypes, LegacySyntax.PREVIEW) ?: return

        when (target.type) {
            TargetType.CRATE -> Crates.getByID(target.id)?.previewForPlayer(player)
                ?: return player.sendMessage(plugin.langYml.getMessage("invalid-crate"))
            TargetType.POUCH -> Pouches[target.id]?.previewForPlayer(player)
                ?: return player.sendMessage(plugin.langYml.getMessage("invalid-pouch"))
            else -> return
        }

        TargetCommands.warnIfLegacy(player, "preview", null, target, includeAmount = false)
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> =
        TargetCommands.tabComplete(args, previewTypes, includeAmounts = false)
}
