package com.willfp.ecocrates.commands

import com.willfp.eco.core.command.impl.Subcommand
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Rewards
import org.bukkit.command.CommandSender
import org.bukkit.util.StringUtil

object CommandResetTotalWins : Subcommand(
    plugin,
    "resettotalwins",
    "ecocrates.command.resettotalwins",
    false
) {
    private const val ALL_REWARDS = "all"

    override fun onExecute(sender: CommandSender, args: List<String>) {
        when {
            args.isEmpty() -> sender.sendMessage(plugin.langYml.getMessage("must-specify-reward"))
            args[0] == ALL_REWARDS -> resetAll(sender)
            else -> resetReward(sender, args[0])
        }
    }

    private fun resetAll(sender: CommandSender) {
        Rewards.values().forEach { it.resetTotalWins() }
        sendReset(sender, ALL_REWARDS)
    }

    private fun resetReward(sender: CommandSender, id: String) {
        val reward = Rewards.getByID(id)

        if (reward == null) {
            sender.sendMessage(plugin.langYml.getMessage("invalid-reward"))
            return
        }

        reward.resetTotalWins()
        sendReset(sender, reward.name)
    }

    private fun sendReset(sender: CommandSender, rewardName: String) {
        sender.sendMessage(
            plugin.langYml.getMessage("reset-total-wins").replace(oldValue = "%reward%", newValue = rewardName)
        )
    }

    override fun tabComplete(sender: CommandSender, args: List<String>): List<String> {
        val targets = Rewards.values().map { it.id } + ALL_REWARDS

        return when (args.size) {
            0 -> targets
            1 -> StringUtil.copyPartialMatches(args[0], targets, mutableListOf())
            else -> emptyList()
        }
    }
}
