package com.willfp.ecocrates.libreforge

import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.Rewards
import com.willfp.ecocrates.util.RewardWeightEvent
import com.willfp.libreforge.effects.templates.MultiMultiplierEffect
import com.willfp.libreforge.toDispatcher
import org.bukkit.event.EventHandler

object EffectRewardWeightMultiplier : MultiMultiplierEffect<Reward>("reward_weight_multiplier") {
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