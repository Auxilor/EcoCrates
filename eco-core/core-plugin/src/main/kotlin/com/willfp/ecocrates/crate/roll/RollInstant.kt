package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.entity.Player

class RollInstant private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val plugin: EcoPlugin,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean
) : Roll {
    override fun roll() {
        // No roll.
    }

    override fun tick(tick: Int) {
        // No tick.
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return false
    }

    override fun onFinish() {
        // No finish.
    }

    object Factory : RollFactory<RollInstant>("instant") {
        override fun create(options: RollOptions): RollInstant =
            RollInstant(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location,
                options.isReroll
            )
    }
}
