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
    @Deprecated("Rolls can come from pouches too", ReplaceWith("source"))
    val crate: Crate
        get() = source as? Crate
            ?: throw IllegalStateException("Roll source '${source.id}' is not a crate")
}
