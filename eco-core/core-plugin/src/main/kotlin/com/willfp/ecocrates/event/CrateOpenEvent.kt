package com.willfp.ecocrates.event

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class CrateOpenEvent(
    player: Player,
    val crate: Crate,
    val isPhysicalKey: Boolean,
    var reward: Reward
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
