package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import org.bukkit.Location
import org.bukkit.entity.Player

class RollInstant private constructor(
    override val reward: Reward,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
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
        override val isGuiRoll = true

        override fun create(options: RollOptions): RollInstant =
            RollInstant(
                options.reward,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
