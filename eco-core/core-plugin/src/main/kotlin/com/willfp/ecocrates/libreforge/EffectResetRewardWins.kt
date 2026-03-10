package com.willfp.ecocrates.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecocrates.reward.Rewards
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.getFormattedString
import com.willfp.libreforge.triggers.TriggerData

object EffectResetRewardWins : Effect<NoCompileData>("reward_reset_wins") {
    override val arguments = arguments {
        require("reward", "You must specify the reward!")
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val reward = Rewards[config.getFormattedString("reward", data)] ?: return false

        reward.resetWins(player)

        return true
    }
}
