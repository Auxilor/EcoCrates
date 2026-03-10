package com.willfp.ecocrates.util

import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class RewardWeightEvent(
    player: Player,
    val reward: Reward,
    var weight: Double
) : PlayerEvent(player) {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}
