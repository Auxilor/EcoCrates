package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.Location
import org.bukkit.entity.Player

interface Roll {
    /**
     * The reward that will be given.
     */
    val reward: Reward

    /**
     * The player.
     */
    val player: Player

    /**
     * The reward source being opened: a crate or a pouch.
     */
    val source: RewardSource

    /**
     * The crate.
     */
    @Deprecated("Rolls can come from pouches too", ReplaceWith("source"))
    val crate: Crate
        get() = source as? Crate
            ?: throw IllegalStateException("Roll source '${source.id}' is not a crate")

    /**
     * The location.
     */
    val location: Location

    /**
     * If the roll is a reroll.
     */
    val isReroll: Boolean

    /**
     * The open method
     */
    val method: OpenMethod

    /**
     * The placed crate this roll is happening at, if any. Lets physical rolls
     * line up with where the crate's preview hologram/item normally sits.
     */
    val placedCrate: PlacedCrate?
        get() = null

    /**
     * Called on start - once the player begins opening the crate.
     */
    fun roll()

    /**
     * Tick the roll.
     *
     * @param tick The current tick.
     */
    fun tick(tick: Int)

    /**
     * Get if the crate should continue ticking,
     * if this returns false then the roll is finished,
     * and onFinish will be called, rewards will be given, etc.
     *
     * @param tick The current tick.
     * @return If ticking should continue.
     */
    fun shouldContinueTicking(tick: Int): Boolean

    /**
     * Called once the crate is finished, useful if the
     * roll has an inventory as it can then be closed.
     */
    fun onFinish()
}
