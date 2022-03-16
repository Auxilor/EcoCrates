package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.entity.Player

data class RollOptions(
    val reward: Reward,
    val crate: Crate,
    val plugin: EcoPlugin,
    val player: Player,
    val location: Location
)