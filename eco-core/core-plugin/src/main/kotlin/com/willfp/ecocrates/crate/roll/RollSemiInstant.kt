package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList

class RollSemiInstant private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val itemLifespan = plugin.configYml.getInt("rolls.semi_instant.item-lifespan")

    private val randomness = plugin.configYml.getDouble("rolls.semi_instant.velocity.randomness")

    private val items = CopyOnWriteArrayList<Item>()

    private fun randomVelocity(): Vector =
        Vector(
            plugin.configYml.getDouble("rolls.semi_instant.velocity.x"),
            plugin.configYml.getDouble("rolls.semi_instant.velocity.y"),
            plugin.configYml.getDouble("rolls.semi_instant.velocity.z")
        ).apply {
            this.x += NumberUtils.randFloat(-randomness, randomness)
            this.y += NumberUtils.randFloat(-randomness, randomness)
            this.z += NumberUtils.randFloat(-randomness, randomness)
        }

    @Suppress("DEPRECATION")
    private fun dropItem(itemReward: Reward): Item =
        location.world!!.dropItem(location, itemReward.getDisplay(player, source)).apply {
            pickupDelay = Int.MAX_VALUE
            isCustomNameVisible = true
            customName = itemReward.displayName
            RollItems.mark(this)
        }

    override fun roll() {
        rewards.forEach { items.add(dropItem(it)) }

        player.closeInventory()

        items.forEach { it.velocity = randomVelocity() }
    }

    override fun tick(tick: Int) {
        // No tick.
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return false
    }

    override fun onFinish() {
        items.forEach { item ->
            plugin.scheduler.on(item).runLater(itemLifespan.toLong()) {
                item.remove()
            }
        }
    }

    object Factory : RollFactory<RollSemiInstant>("semi_instant") {
        override val maxRewards = Int.MAX_VALUE

        override fun create(options: RollOptions): RollSemiInstant =
            RollSemiInstant(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
