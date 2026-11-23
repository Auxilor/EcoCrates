package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.reward.Rewards
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.config.section.getFormattedString
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object EffectResetRewardWins : Effect<NoCompileData>("reward_reset_wins") {
    override val description = "Resets the player's win count for the specified reward, allowing it to be won again if it has a win limit."

    override val categories = setOf("inventory")

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "reward",
            "You must specify the reward!",
            description = "The ID of the reward to reset the win count for.",
            type = ArgType.STRING
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val reward = Rewards[config.getFormattedString("reward", data)] ?: return false

        reward.resetWins(player)

        return true
    }
}
