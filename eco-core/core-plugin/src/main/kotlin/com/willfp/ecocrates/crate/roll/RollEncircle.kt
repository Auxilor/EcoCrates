package com.willfp.ecocrates.crate.roll

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.util.lerp
import org.bukkit.Location
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.PI

class RollEncircle private constructor(
    override val reward: Reward,
    private val crate: Crate,
    private val plugin: EcoPlugin,
    private val player: Player,
    private val location: Location
) : Roll {
    private val riseVelocity = plugin.configYml.getDouble("rolls.encircle.rise-velocity")
    private val spinVelocity = plugin.configYml.getDouble("rolls.encircle.spin-velocity")
    private val revealVelocity = plugin.configYml.getDouble("rolls.encircle.reveal-velocity")
    private val spinTime = plugin.configYml.getInt("rolls.encircle.spin-time")
    private val revealTime = plugin.configYml.getInt("rolls.encircle.reveal-time")
    private val spinsPerSecond = plugin.configYml.getDouble("rolls.encircle.spins-per-second")
    private val itemCount = plugin.configYml.getInt("rolls.encircle.items")
    private val fillerItems = crate.getRandomRewards(
        player,
        itemCount - 2, // Take two (?) off as the actual reward is in the display
        displayWeight = true
    )
    private val radius = plugin.configYml.getDouble("rolls.encircle.radius")
    private val height = plugin.configYml.getDouble("rolls.encircle.height")
    private val angle = 2 * PI / itemCount

    private val circleCenter = location.toVector()
        .add(Vector(0.0, height, 0.0))

    private var state = EncircleState.RISE
    private var timeSpentSpinning = 0
    private var timeSpentRevealing = 0

    private val display = mutableListOf<Item>()

    override fun roll() {
        val world = location.world!!

        val itemsToDisplay = fillerItems.toMutableList()
        itemsToDisplay.add(reward)
        itemsToDisplay.shuffle()

        for (item in itemsToDisplay) {
            val entity = world.dropItem(location, item.display)

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.isCustomNameVisible = true
            entity.customName = item.displayName
            display.add(entity)
        }
    }

    override fun tick(tick: Int) {
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
                for (item in display.toSet()) {
                    if (item.itemStack != reward.display) {
                        item.remove()
                        display.remove(item)
                    }
                }

                val rewardItem = display.first()

                if (circleCenter.distance(rewardItem.location.toVector()) > 0.2) {
                    rewardItem.velocity = circleCenter.clone()
                        .subtract(rewardItem.location.toVector())
                        .normalize()
                        .multiply(revealVelocity)
                }

                timeSpentRevealing++

                if (timeSpentRevealing > revealTime) {
                    state = EncircleState.DONE
                }
            }
            else -> {
                // Do nothing
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
        override fun create(options: RollOptions): RollEncircle =
            RollEncircle(
                options.reward,
                options.crate,
                options.plugin,
                options.player,
                options.location
            )
    }
}
