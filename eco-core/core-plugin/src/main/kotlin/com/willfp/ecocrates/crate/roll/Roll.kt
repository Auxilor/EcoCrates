package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.Reward
import org.bukkit.entity.Player

abstract class Roll(
    val id: String,
    val player: Player,
    val reward: Reward
) {
    abstract fun roll()
}