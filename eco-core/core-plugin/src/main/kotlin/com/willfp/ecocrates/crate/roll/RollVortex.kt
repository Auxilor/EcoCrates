package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.teleportCompat
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.util.RollItems
import com.willfp.ecocrates.util.lerp
import kotlin.math.PI
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Items spiral inwards and upwards around the crate, and the funnel throws out a
 * loser on every pass until only the winners are left at the top.
 * It throws out one loser per pass, keeping the winners until the funnel closes.
 * The funnel is topped up with filler rewards. One slot is reserved for each winning reward.
 */
class RollVortex private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val itemCount = RingItems.count(
        configured = plugin.configYml.getInt("rolls.vortex.items"),
        minimum = 2,
        rewards = rewards.size
    )
    private val startRadius = plugin.configYml.getDouble("rolls.vortex.start-radius")
    private val endRadius = plugin.configYml.getDouble("rolls.vortex.end-radius")
    private val startHeight = plugin.configYml.getDouble("rolls.vortex.start-height")
    private val endHeight = plugin.configYml.getDouble("rolls.vortex.end-height")
    private val spinsPerSecond = plugin.configYml.getDouble("rolls.vortex.spins-per-second")
    private val duration = plugin.configYml.getInt("rolls.vortex.duration").coerceAtLeast(1)
    private val ejectVelocity = plugin.configYml.getDouble("rolls.vortex.eject-velocity")
    private val rewardHoldTime = plugin.configYml.getInt("rolls.vortex.reward-hold-time")
    private val timeout = plugin.configYml.getInt("rolls.vortex.timeout")

    private val fillerItems = source.getRandomRewards(player, itemCount - rewards.size)

    // Losers are thrown clear one at a time, spread evenly across the funnel.
    private val ejectInterval = (duration / (itemCount - rewards.size + 1)).coerceAtLeast(1)

    private val center = location.toVector()

    private val orbiting = mutableListOf<Item>()
    private val ejected = mutableListOf<Item>()
    private val winners = CopyOnWriteArrayList<Item>()

    private var timeSpentHolding = 0

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!
        val fillers = fillerItems.iterator()

        player.closeInventory()

        for (rewardIndex in RingItems.order(itemCount = itemCount, rewards = rewards.size)) {
            val item = if (rewardIndex == RingItems.FILLER) fillers.next() else rewards[rewardIndex]
            val entity = world.dropItem(location, item.getDisplay(player, source))

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.isCustomNameVisible = true
            entity.customName = item.displayName
            RollItems.mark(entity)
            orbiting.add(entity)

            if (rewardIndex != RingItems.FILLER) {
                winners.add(entity)
            }
        }
    }

    override fun tick(tick: Int) {
        orbiting.removeIf { !it.isValid }

        if (orbiting.isEmpty() || winners.any { !it.isValid }) {
            timeSpentHolding = rewardHoldTime + 1
            return
        }

        val progress = (tick.toDouble() / duration).coerceAtMost(1.0)
        val radius = lerp(startRadius, endRadius, progress)
        val height = lerp(startHeight, endHeight, progress)
        val angle = 2 * PI / orbiting.size

        for ((index, item) in orbiting.withIndex()) {
            val theta = (angle * index) + (spinsPerSecond * 2 * PI * tick / 20)

            val target = center.clone().add(
                Vector(
                    NumberUtils.fastSin(theta) * radius,
                    height,
                    NumberUtils.fastCos(theta) * radius
                )
            )

            item.velocity = Vector(0, 0, 0)
            item.teleportCompat(target.toLocation(item.world))
        }

        if (tick > 0 && tick % ejectInterval == 0 && orbiting.size > winners.size) {
            eject(orbiting.first { it !in winners })
        }

        if (orbiting.size == winners.size) {
            timeSpentHolding++
        }

        if (tick % 4 == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                0.8f,
                lerp(0.6, 2.0, progress).toFloat()
            )
        }
    }

    private fun eject(item: Item) {
        orbiting.remove(item)
        ejected.add(item)

        item.setGravity(true)
        item.velocity = item.location.toVector()
            .subtract(center)
            .normalize()
            .multiply(ejectVelocity)
            .setY(0.3)

        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.7f, 0.6f)
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return timeSpentHolding <= rewardHoldTime && tick < timeout
    }

    override fun onFinish() {
        orbiting.forEach { it.remove() }
        ejected.forEach { it.remove() }
    }

    object Factory : RollFactory<RollVortex>("vortex") {
        override val maxRewards: Int
            get() = RingItems.capacity(plugin.configYml.getInt("rolls.vortex.items"))

        override fun create(options: RollOptions): RollVortex =
            RollVortex(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
