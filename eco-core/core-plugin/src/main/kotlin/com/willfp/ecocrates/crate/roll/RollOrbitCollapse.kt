package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.lerp
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.PI

class RollOrbitCollapse private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val riseVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.rise-velocity")
    private val orbitVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.orbit-velocity")
    private val collapseVelocity = plugin.configYml.getDouble("rolls.orbit_collapse.collapse-velocity")
    private val radius = plugin.configYml.getDouble("rolls.orbit_collapse.radius")
    private val height = plugin.configYml.getDouble("rolls.orbit_collapse.height")
    private val orbitTime = plugin.configYml.getInt("rolls.orbit_collapse.orbit-time")
    private val rewardHoldTime = plugin.configYml.getInt("rolls.orbit_collapse.reward-hold-time")
    private val spinsPerSecond = plugin.configYml.getDouble("rolls.orbit_collapse.spins-per-second")
    private val itemCount = plugin.configYml.getInt("rolls.orbit_collapse.items").coerceAtLeast(1)

    // Items are moved by velocity, so they can end up circling a target they never quite
    // reach. This caps the whole animation so a player can never be stuck mid-open.
    private val timeout = plugin.configYml.getInt("rolls.orbit_collapse.timeout")

    private val fillerItems = crate.getRandomRewards(
        player,
        itemCount - 1 // One slot is reserved for the winning reward.
    )

    private val angle = 2 * PI / itemCount

    private val circleCenter = location.toVector()
        .add(Vector(0.0, height, 0.0))

    private val display = mutableListOf<Item>()
    private var rewardItem: Item? = null

    private var state = OrbitCollapseState.RISE
    private var timeSpentOrbiting = 0
    private var timeSpentHolding = 0

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!

        val itemsToDisplay = fillerItems.toMutableList()
        itemsToDisplay.add(reward)
        itemsToDisplay.shuffle()

        player.closeInventory()

        for (item in itemsToDisplay) {
            val entity = world.dropItem(location, item.getDisplay(player, crate))

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.isCustomNameVisible = true
            entity.customName = item.displayName
            entity.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))
            display.add(entity)

            if (item === reward) {
                rewardItem = entity
            }
        }
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
        val winner = rewardItem

        if (winner == null || !winner.isValid) {
            state = OrbitCollapseState.DONE
            return
        }

        // Everything that isn't the winner falls inwards and is swallowed at the centre.
        for (item in display.toList()) {
            if (item === winner) {
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

        if (winner.location.toVector().distance(circleCenter) <= 0.3) {
            winner.teleport(circleCenter.toLocation(winner.world))
            winner.velocity = Vector(0, 0, 0)
            timeSpentHolding++
        } else {
            winner.velocity = circleCenter.clone()
                .subtract(winner.location.toVector())
                .normalize()
                .multiply(collapseVelocity)
        }

        if (timeSpentHolding > rewardHoldTime) {
            state = OrbitCollapseState.DONE
        }
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
        override fun create(options: RollOptions): RollOrbitCollapse =
            RollOrbitCollapse(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
