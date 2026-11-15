package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
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
 * Items rise into a spinning ring around the crate, then every loser is removed and
 * the winners glide into a line across the centre.
 * The ring is topped up with filler rewards. One slot is reserved for each winning reward.
 */
class RollEncircle private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val riseVelocity = plugin.configYml.getDouble("rolls.encircle.rise-velocity")
    private val spinVelocity = plugin.configYml.getDouble("rolls.encircle.spin-velocity")
    private val revealVelocity = plugin.configYml.getDouble("rolls.encircle.reveal-velocity")
    private val spinTime = plugin.configYml.getInt("rolls.encircle.spin-time")
    private val revealTime = plugin.configYml.getInt("rolls.encircle.reveal-time")
    private val spinsPerSecond = plugin.configYml.getDouble("rolls.encircle.spins-per-second")
    private val itemCount = RingItems.count(
        configured = plugin.configYml.getInt("rolls.encircle.items"),
        minimum = 1,
        rewards = rewards.size
    )
    private val fillerItems = source.getRandomRewards(player, itemCount - rewards.size)
    private val radius = plugin.configYml.getDouble("rolls.encircle.radius")
    private val height = plugin.configYml.getDouble("rolls.encircle.height")
    private val angle = 2 * PI / itemCount

    private val circleCenter = location.toVector()
        .add(Vector(0.0, height, 0.0))

    private var state = EncircleState.RISE
    private var timeSpentSpinning = 0
    private var timeSpentRevealing = 0

    private val display = mutableListOf<Item>()
    private val winners = CopyOnWriteArrayList<Item>()
    private val winnerTargets = RingItems.winnerTargets(
        center = circleCenter,
        count = rewards.size,
        yaw = player.location.yaw.toDouble()
    )

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!
        val fillers = fillerItems.iterator()

        player.closeInventory()

        val spawned = RingItems.order(itemCount = itemCount, rewards = rewards.size).map { rewardIndex ->
            val item = if (rewardIndex == RingItems.FILLER) fillers.next() else rewards[rewardIndex]
            val entity = world.dropItem(location, item.getDisplay(player, source))

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.isCustomNameVisible = true
            entity.customName = item.displayName
            RollItems.mark(entity)
            display.add(entity)

            rewardIndex to entity
        }

        winners.addAll(RingItems.winners(spawned))
    }

    override fun tick(tick: Int) {
        display.removeIf { !it.isValid }
        if (display.isEmpty() || state == EncircleState.REVEAL && winners.any { !it.isValid }) {
            state = EncircleState.DONE
            return
        }

        when (state) {
            EncircleState.RISE -> {
                for ((index, item) in display.withIndex()) {
                    val endPosition = circleCenter.clone().add(
                        Vector(
                            NumberUtils.fastSin(angle * index) * radius,
                            0.0,
                            NumberUtils.fastCos(angle * index) * radius
                        )
                    )

                    val velocity = endPosition.clone()
                        .subtract(item.location.toVector())
                        .normalize()
                        .multiply(riseVelocity)

                    item.velocity = velocity

                    if (item.location.toVector().distance(endPosition) < 0.25) {
                        state = EncircleState.SPIN
                    }
                }
            }

            EncircleState.SPIN -> {
                for ((index, item) in display.withIndex()) {
                    val endPosition = circleCenter.clone().add(
                        Vector(
                            NumberUtils.fastSin((angle * index) + (spinsPerSecond * 2 * PI * timeSpentSpinning / 20)) * radius,
                            0.0,
                            NumberUtils.fastCos((angle * index) + (spinsPerSecond * 2 * PI * timeSpentSpinning / 20)) * radius
                        )
                    )

                    if (endPosition.distance(item.location.toVector()) > 0.2) {
                        val velocity = endPosition.clone()
                            .subtract(item.location.toVector())
                            .normalize()
                            .multiply(spinVelocity)

                        item.velocity = velocity
                    }
                }

                timeSpentSpinning++

                if (timeSpentSpinning > spinTime) {
                    state = EncircleState.REVEAL
                }
            }

            EncircleState.REVEAL -> {
                tickReveal()
            }

            EncircleState.DONE -> {
                return
            }
        }

        if (tick % 4 == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                1f,
                lerp(0.5, 2.0, tick.toDouble() / (spinTime + revealTime)).toFloat()
            )
        }
    }

    private fun tickReveal() {
        val winnerSet = winners.toSet()

        for (item in display.toSet()) {
            if (item !in winnerSet) {
                item.remove()
                display.remove(item)
            }
        }

        for ((winner, target) in winners.zip(winnerTargets)) {
            if (target.distance(winner.location.toVector()) > 0.2) {
                winner.velocity = target.clone()
                    .subtract(winner.location.toVector())
                    .normalize()
                    .multiply(revealVelocity)
            }
        }

        timeSpentRevealing++

        if (timeSpentRevealing > revealTime) {
            state = EncircleState.DONE
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return state != EncircleState.DONE
    }

    override fun onFinish() {
        display.forEach { it.remove() }
    }

    private enum class EncircleState {
        RISE,
        SPIN,
        REVEAL,
        DONE
    }

    object Factory : RollFactory<RollEncircle>("encircle") {
        override val maxRewards: Int
            get() = RingItems.capacity(plugin.configYml.getInt("rolls.encircle.items"))

        override fun create(options: RollOptions): RollEncircle =
            RollEncircle(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
