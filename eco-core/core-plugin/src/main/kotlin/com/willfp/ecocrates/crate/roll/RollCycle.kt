package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.crate.placed.PlacedCrate
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.util.RollItems
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList

class RollCycle private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod,
    override val placedCrate: PlacedCrate?
) : Roll {
    override val reward: Reward = rewards.first()

    private val duration = plugin.configYml.getInt("rolls.cycle.duration")
    private val wait = plugin.configYml.getInt("rolls.cycle.wait")
    private val interval = plugin.configYml.getInt("rolls.cycle.interval")
    private val heightOffset = plugin.configYml.getDouble("rolls.cycle.height-offset")

    // Line up with wherever the crate's preview item/hologram normally floats,
    // adjustable via rolls.cycle.height-offset.
    private val base = placedCrate?.location ?: location
    private val height = source.rollHeight + heightOffset

    private val cycleItems = CopyOnWriteArrayList<CycleItem>()

    /**
     * One reward's item, hovering at its place in the line, cycling through rewards
     * until its lock tick and then showing [itemReward].
     */
    private inner class CycleItem(
        private val itemReward: Reward,
        private val offset: LineOffset,
        val lockTick: Int
    ) {
        private val display = source.getRandomRewards(player, 100)

        private val hoverLocation: Location
            get() = base.clone().add(Vector(offset.x, height, offset.z))

        val item: Item = location.world!!.dropItem(hoverLocation, display[0].getDisplay(player, source)).apply {
            velocity = Vector(0.0, 0.0, 0.0)
            pickupDelay = Int.MAX_VALUE
            setGravity(false)
            isCustomNameVisible = true
            RollItems.mark(this)
        }

        fun hover() {
            item.velocity = Vector(0.0, 0.0, 0.0)
            item.teleportCompat(hoverLocation)
        }

        @Suppress("DEPRECATION")
        fun cycle(tick: Int) {
            if (tick < lockTick) {
                val index = tick.floorDiv(interval).coerceAtMost(display.lastIndex)
                item.itemStack = display[index].getDisplay(player, source)
                item.customName = display[index].displayName
            } else {
                item.itemStack = itemReward.getDisplay(player, source)
                item.customName = itemReward.displayName
            }
        }
    }

    override fun roll() {
        RewardLine.offsets(count = rewards.size, yaw = player.location.yaw.toDouble())
            .zip(rewards)
            .forEachIndexed { index, (offset, itemReward) ->
                cycleItems.add(
                    CycleItem(
                        itemReward,
                        offset,
                        RewardStagger.lockTick(index = index, firstLock = duration, gap = wait)
                    )
                )
            }
    }

    override fun tick(tick: Int) {
        cycleItems.forEach { it.hover() }

        cycleItems.filter { tick % interval == 0 || it.lockTick == tick }.forEach { it.cycle(tick) }

        if (tick % interval == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                1f,
                0.5f
            )
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        tick < RewardStagger.endTick(count = rewards.size, firstLock = duration, gap = wait, hold = wait)

    override fun onFinish() {
        cycleItems.forEach { it.item.remove() }
    }

    object Factory : RollFactory<RollCycle>("cycle") {
        override val maxRewards = 5

        override fun create(options: RollOptions): RollCycle =
            RollCycle(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method,
                options.placedCrate
            )
    }
}
