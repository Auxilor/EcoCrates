package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.RewardWeightEvent
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.templates.MultiMultiplierEffect
import com.willfp.libreforge.toDispatcher
import org.bukkit.event.EventHandler

object EffectRewardWeightMultiplier : MultiMultiplierEffect<Reward>("reward_weight_multiplier") {
    override val description = "Multiplies the weight of one or all EcoCrates rewards while the holder is active."

    override val categories = setOf("economy")

    override val arguments = arguments {
        require(
            "multiplier",
            "You must specify the multiplier!",
            description = "The reward weight multiplier. Supports expressions.",
            type = ArgType.EXPRESSION
        )
        optional(
            "rewards",
            description = "List of reward names to apply the multiplier to. If omitted, applies to all rewards.",
            type = ArgType.STRING_LIST
        )
    }

    override val key = "rewards"

    override fun getElement(key: String): Reward? {
        return Rewards.getByID(key)
    }

    override fun getAllElements(): Collection<Reward> {
        return Rewards.values()
    }

    @EventHandler(ignoreCancelled = true)
    fun handle(event: RewardWeightEvent) {
        event.weight *= getMultiplier(event.player.toDispatcher(), event.reward)
    }
}