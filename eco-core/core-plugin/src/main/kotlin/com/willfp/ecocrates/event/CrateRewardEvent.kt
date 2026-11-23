package com.willfp.ecocrates.event

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class CrateRewardEvent @JvmOverloads constructor(
    player: Player,
    override val crate: Crate,
    var reward: Reward,
    override val pick: PickInfo = PickInfo.FIRST
) : PlayerEvent(player), PickEvent, CrateEvent {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}
