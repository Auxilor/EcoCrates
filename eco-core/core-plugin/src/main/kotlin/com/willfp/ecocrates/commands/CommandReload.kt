package com.willfp.ecocrates.commands

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.Crates
import com.willfp.ecocrates.crate.Keys
import com.willfp.ecocrates.crate.placed.particle.ParticleAnimations
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Rewards
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import wiki.ecohub.core.platform.ownsGlobal
import wiki.ecohub.core.plugin.startup.Prerequisite
import wiki.ecohub.core.stdlib.toNiceString
import wiki.ecohub.ui.text.StringUtils

object CommandReload : EcoCratesSubcommand(
    "reload",
    "ecocrates.command.reload",
    false
) {
    override fun onExecute(sender: CommandSender, args: List<String>) {
        if (Prerequisite.HAS_FOLIA.isMet && !Bukkit.getServer().ownsGlobal()) {
            plugin.scheduler.global().run { onExecute(sender, args) }
            return
        }

        sender.sendMessage(
            plugin.langYml.getMessage("reloaded", StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)
                .replace("%time%", plugin.reloadWithTime().toNiceString())
                .replace("%crates%", Crates.values().size.toString())
                .replace("%keys%", Keys.values().size.toString())
                .replace("%rewards%", Rewards.values().size.toString())
        )
        ParticleAnimations.reload()
    }
}
