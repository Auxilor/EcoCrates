package com.willfp.ecocrates.event

import com.willfp.ecocrates.pouch.Pouch
import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/** Fired when a pouch roll finishes, just before [reward] is given. */
class PouchRewardEvent @JvmOverloads constructor(
    player: Player,
    val pouch: Pouch,
    var reward: Reward,
    override val pick: PickInfo = PickInfo.FIRST
) : PlayerEvent(player), PickEvent {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}
