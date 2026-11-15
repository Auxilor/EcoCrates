package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.Location
import org.bukkit.entity.Player

data class RollOptions(
    val reward: Reward,
    val source: RewardSource,
    val player: Player,
    val location: Location,
    val isReroll: Boolean,
    val method: OpenMethod,
    val placedCrate: PlacedCrate? = null
) {
    /**
     * All rewards to give, the first being [reward]. Not part of the data class components,
     * so the original constructor, copy and component functions stay binary compatible.
     */
    var rewards: List<Reward> = listOf(reward)
        private set

    /**
     * Where the rewards sit in their open. Set by the open session before the roll is created.
     * Not part of the data class components, so the constructors, copy and component functions stay binary compatible.
     */
    var pickContext: RollPickContext = RollPickContext.DEFAULT
        internal set

    constructor(
        reward: Reward,
        source: RewardSource,
        player: Player,
        location: Location,
        isReroll: Boolean,
        method: OpenMethod,
        placedCrate: PlacedCrate?,
        rewards: List<Reward>
    ) : this(reward, source, player, location, isReroll, method, placedCrate) {
        this.rewards = rewards
    }

    @Deprecated("Rolls can come from pouches too", ReplaceWith("source"))
    val crate: Crate
        get() = source as? Crate
            ?: throw IllegalStateException("Roll source '${source.id}' is not a crate")
}
