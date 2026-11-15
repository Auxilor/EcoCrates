package com.willfp.ecocrates.event

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class CrateOpenEvent @JvmOverloads constructor(
    player: Player,
    override val crate: Crate,
    val method: OpenMethod,
    var reward: Reward,
    val isReroll: Boolean,
    override val pick: PickInfo = PickInfo.FIRST
) : PlayerEvent(player), PickEvent, CrateEvent, Cancellable {
    private var cancelled = false

    override fun isCancelled() = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }
}
