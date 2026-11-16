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
 * Items orbit the crate, then collapse. Everything that isn't a winner falls inwards
 * and is swallowed at the centre, while the winners settle in a line across it.
 * The ring is topped up with filler rewards. One slot is reserved for each winning reward.
 */
class RollOrbitCollapse private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val riseVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.rise-velocity")
    private val orbitVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.orbit-velocity")
    private val collapseVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.collapse-velocity")
    private val radius = plugin.configYml.getDouble("rolls.orbit_collapse.radius")
    private val height = plugin.configYml.getDouble("rolls.orbit_collapse.height")
    private val orbitTime = plugin.configYml.getInt("rolls.orbit_collapse.orbit-time")
    private val rewardHoldTime = plugin.configYml.getInt("rolls.orbit_collapse.reward-hold-time")
    private val spinsPerSecond = plugin.configYml.getDouble("rolls.orbit_collapse.spins-per-second")
    private val itemCount = RingItems.count(
        configured = plugin.configYml.getInt("rolls.orbit_collapse.items"),
        minimum = 1,
        rewards = rewards.size
    )

    // Items are moved by velocity, so they can end up circling a target they never quite
    // reach. This caps the whole animation so a player can never be stuck mid-open.
    private val timeout = plugin.configYml.getInt("rolls.orbit_collapse.timeout")

    private val fillerItems = source.getRandomRewards(player, itemCount - rewards.size)

    private val angle = 2 * PI / itemCount

    private val circleCenter = location.toVector()
        .add(Vector(0.0, height, 0.0))

    private val display = mutableListOf<Item>()
    private val winners = CopyOnWriteArrayList<Item>()
    private val winnerTargets = RingItems.winnerTargets(
        center = circleCenter,
        count = rewards.size,
        yaw = player.location.yaw.toDouble()
    )

    private var state = OrbitCollapseState.RISE
    private var timeSpentOrbiting = 0
    private var timeSpentHolding = 0

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
        if (display.isEmpty()) {
            state = OrbitCollapseState.DONE
            return
        }

        when (state) {
            OrbitCollapseState.RISE -> tickRise()
            OrbitCollapseState.ORBIT -> tickOrbit()
            OrbitCollapseState.COLLAPSE -> tickCollapse()
            OrbitCollapseState.DONE -> return
        }

        if (tick % 4 == 0) {
            player.playSound(
                player.location,
                Sound.BLOCK_NOTE_BLOCK_PLING,
                0.8f,
                lerp(0.6, 2.0, tick.toDouble() / (orbitTime + rewardHoldTime).coerceAtLeast(1)).toFloat()
            )
        }
    }

    private fun tickRise() {
        var allAtTarget = true

        for ((index, item) in display.withIndex()) {
            val endPosition = positionOnRing(index, 0)
            val delta = endPosition.clone().subtract(item.location.toVector())

            if (delta.length() <= 0.25) {
                item.velocity = Vector(0, 0, 0)
            } else {
                allAtTarget = false
                item.velocity = delta.normalize().multiply(riseVelocity)
            }
        }

        if (allAtTarget) {
            state = OrbitCollapseState.ORBIT
        }
    }

    private fun tickOrbit() {
        for ((index, item) in display.withIndex()) {
            val endPosition = positionOnRing(index, timeSpentOrbiting)

            item.velocity = endPosition.clone()
                .subtract(item.location.toVector())
                .normalize()
                .multiply(orbitVelocity)
        }

        timeSpentOrbiting++

        if (timeSpentOrbiting > orbitTime) {
            state = OrbitCollapseState.COLLAPSE
        }
    }

    private fun tickCollapse() {
        if (winners.any { !it.isValid }) {
            state = OrbitCollapseState.DONE
            return
        }

        val winnerSet = winners.toSet()

        for (item in display.toList()) {
            if (item in winnerSet) {
                continue
            }

            if (item.location.toVector().distance(circleCenter) <= 0.3) {
                item.remove()
                display.remove(item)
            } else {
                item.velocity = circleCenter.clone()
                    .subtract(item.location.toVector())
                    .normalize()
                    .multiply(collapseVelocity)
            }
        }

        val arrived = winners.zip(winnerTargets).map { (winner, target) -> collapseWinner(winner, target) }

        if (arrived.all { it }) {
            timeSpentHolding++
        }

        if (timeSpentHolding > rewardHoldTime) {
            state = OrbitCollapseState.DONE
        }
    }

    private fun collapseWinner(winner: Item, target: Vector): Boolean {
        val arrived = winner.location.toVector().distance(target) <= 0.3

        if (arrived) {
            winner.teleportCompat(target.toLocation(winner.world))
            winner.velocity = Vector(0, 0, 0)
        } else {
            winner.velocity = target.clone()
                .subtract(winner.location.toVector())
                .normalize()
                .multiply(collapseVelocity)
        }

        return arrived
    }

    private fun positionOnRing(index: Int, ticksElapsed: Int): Vector {
        val theta = (angle * index) + (spinsPerSecond * 2 * PI * ticksElapsed / 20)

        return circleCenter.clone().add(
            Vector(
                NumberUtils.fastSin(theta) * radius,
                0.0,
                NumberUtils.fastCos(theta) * radius
            )
        )
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return state != OrbitCollapseState.DONE && tick < timeout
    }

    override fun onFinish() {
        display.forEach { it.remove() }
    }

    private enum class OrbitCollapseState {
        RISE,
        ORBIT,
        COLLAPSE,
        DONE
    }

    object Factory : RollFactory<RollOrbitCollapse>("orbit_collapse") {
        override val maxRewards: Int
            get() = RingItems.capacity(plugin.configYml.getInt("rolls.orbit_collapse.items"))

        override fun create(options: RollOptions): RollOrbitCollapse =
            RollOrbitCollapse(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
