package com.willfp.ecocrates.commands

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.converters.Converters
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

class CommandConvert(plugin: EcoPlugin) : Subcommand(
    plugin,
    "convert",
    "ecocrates.command.convert",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {

        if (args.isEmpty()) {
            sender.sendMessage(plugin.langYml.getMessage("must-specify-converter"))
            return
        }

        val converter = Converters.getById(args[0])

        if (converter == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-converter"))
            return
        }

        converter.convert()
        sender.sendMessage(plugin.langYml.getMessage("converted"))
    }

    override fun tabComplete(sender: CommandSender, args: MutableList<String>): MutableList<String> {
        val values = mutableListOf<String>()
        if (args.size == 1) {
            StringUtil.copyPartialMatches(
                args[0],
                Converters.BY_ID.keys,
                values
            )
        }
        return values
    }
}