package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private const val ARRIVAL_DISTANCE = 0.1

class RollQuick private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val riseVelocity = plugin.configYml.getDouble("rolls.quick.rise-velocity")
    private val height = plugin.configYml.getDouble("rolls.quick.height")
    private val suspend = plugin.configYml.getInt("rolls.quick.suspend")
    private val risingItems = CopyOnWriteArrayList<RisingItem>()

    /**
     * One reward's item, rising from [start] to [end] and then held there for the suspend time.
     */
    private inner class RisingItem(itemReward: Reward, start: Location) {
        val end: Vector = start.toVector().add(Vector(0.0, height, 0.0))

        @Suppress("DEPRECATION")
        val item: Item = start.world!!.dropItem(start, itemReward.getDisplay(player, source)).apply {
            pickupDelay = Int.MAX_VALUE
            setGravity(false)
            isCustomNameVisible = true
            customName = itemReward.displayName
            RollItems.mark(this)
        }

        private val done = AtomicBoolean(false)
        private val suspendTicks = AtomicInteger(0)

        val isDone: Boolean
            get() = done.get()

        fun tick() {
            if (!item.isValid) {
                done.set(true)
                return
            }

            if (item.location.toVector().distance(end) < ARRIVAL_DISTANCE) {
                item.teleportCompat(end.toLocation(item.world))
                item.velocity = Vector(0, 0, 0)

                if (suspendTicks.incrementAndGet() >= suspend) {
                    done.set(true)
                }
            } else {
                item.velocity = end.clone()
                    .subtract(item.location.toVector())
                    .normalize()
                    .multiply(riseVelocity)
            }
        }
    }

    override fun roll() {
        RewardLine.offsets(rewards.size, player.location.yaw.toDouble())
            .zip(rewards)
            .forEach { (offset, itemReward) ->
                risingItems.add(RisingItem(itemReward, location.clone().add(offset.x, 0.0, offset.z)))
            }

        player.closeInventory()
    }

    override fun tick(tick: Int) {
        risingItems.filterNot { it.isDone }.forEach { it.tick() }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        risingItems.isEmpty() || risingItems.any { !it.isDone }

    override fun onFinish() {
        risingItems.forEach { it.item.remove() }
    }

    object Factory : RollFactory<RollQuick>("quick") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollQuick =
            RollQuick(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
