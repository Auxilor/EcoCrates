package com.willfp.ecocrates.event

import com.willfp.ecocrates.pouch.Pouch
import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

/**
 * Fired before a pouch is opened, before anything is paid or consumed.
 * Cancelling stops the open entirely; [reward] can be swapped.
 */
class PouchOpenEvent(
    player: Player,
    val pouch: Pouch,
    var reward: Reward
) : PlayerEvent(player), Cancellable {
    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

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
