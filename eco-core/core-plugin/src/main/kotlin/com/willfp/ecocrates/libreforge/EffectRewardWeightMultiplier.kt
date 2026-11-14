package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.RewardWeightEvent
import org.bukkit.event.EventHandler
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.arguments
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.effect.MultiMultiplierEffect

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