package com.willfp.ecocrates.event

import com.willfp.ecocrates.envoy.EnvoyCategory
import com.willfp.ecocrates.envoy.EnvoyRarity
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Fired when a player collects an envoy crate, after the reward has been
 * rolled but before it is given, so listeners can swap it.
 */
class EnvoyOpenEvent(
    val player: Player,
    val category: EnvoyCategory,
    val rarity: EnvoyRarity,
    var reward: Reward,
    val location: Location
) : Event() {
    override fun getHandlers(): HandlerList = handlerListInstance

    companion object {
        private val handlerListInstance = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerListInstance
    }
}
