package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.Crate
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import kotlin.math.PI

/**
 * The inverse of encircle: the items stand still in a ring on the ground and a
 * glowing cursor runs around them, slowing down until it stops on the winner.
 */
class RollRoulette private constructor(
    override val reward: Reward,
    override val crate: Crate,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    private val itemCount = plugin.configYml.getInt("rolls.roulette.items").coerceAtLeast(2)
    private val radius = plugin.configYml.getDouble("rolls.roulette.radius")
    private val height = plugin.configYml.getDouble("rolls.roulette.height")
    private val steps = plugin.configYml.getInt("rolls.roulette.steps").coerceAtLeast(itemCount)
    private val bias = plugin.configYml.getDouble("rolls.roulette.bias")
    private val maxDelay = plugin.configYml.getInt("rolls.roulette.max-delay").coerceAtLeast(1)
    private val revealTime = plugin.configYml.getInt("rolls.roulette.reveal-time")
    private val timeout = plugin.configYml.getInt("rolls.roulette.timeout")

    /*
    Same easing as the csgo roll: step delays follow a bias curve so the cursor
    starts fast and crawls to a stop, rather than halting abruptly.
     */
    private val delays = (1..steps)
        .asSequence()
        .map { it / steps.toDouble() }
        .map { NumberUtils.bias(it, bias) }
        .map { (it * maxDelay).toInt() }
        .map { it.coerceAtLeast(1) }
        .toList()

    // The ring is laid out so that the final step of the cursor lands on the winner.
    private val winnerIndex = steps.mod(itemCount)

    private val fillerItems = crate.getRandomRewards(player, itemCount - 1)

    private val center = location.toVector().add(Vector(0.0, height, 0.0))

    private val display = mutableListOf<Item>()

    private var step = 0
    private var ticksSinceStep = 0
    private var timeSpentRevealing = 0

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!
        val angle = 2 * PI / itemCount

        player.closeInventory()

        val fillers = fillerItems.iterator()

        for (index in 0 until itemCount) {
            val displayReward = if (index == winnerIndex) reward else fillers.next()

            val position = center.clone().add(
                Vector(
                    NumberUtils.fastSin(angle * index) * radius,
                    0.0,
                    NumberUtils.fastCos(angle * index) * radius
                )
            )

            val entity = world.dropItem(position.toLocation(world), displayReward.getDisplay(player, crate))

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.velocity = Vector(0, 0, 0)
            entity.isCustomNameVisible = true
            entity.customName = displayReward.displayName
            entity.setMetadata("ecocrates-roll-item", plugin.metadataValueFactory.create(true))
            display.add(entity)
        }

        display.firstOrNull()?.isGlowing = true
    }

    override fun tick(tick: Int) {
        // The ring keeps its original size even if an item is lost, otherwise the
        // cursor's landing position would no longer line up with the winner.
        if (display.none { it.isValid }) {
            timeSpentRevealing = revealTime + 1
            return
        }

        if (step >= steps) {
            timeSpentRevealing++
            return
        }

        ticksSinceStep++

        if (ticksSinceStep < delays[step]) {
            return
        }

        ticksSinceStep = 0
        step++

        val cursorIndex = step.mod(itemCount)

        for ((index, item) in display.withIndex()) {
            item.isGlowing = index == cursorIndex
        }

        val cursor = display[cursorIndex]

        if (cursor.isValid) {
            cursor.world.spawnParticle(Particle.END_ROD, cursor.location, 6, 0.1, 0.1, 0.1, 0.01)
        }

        if (step >= steps) {
            // Landed: clear everything but the winner and let it sit there.
            for ((index, item) in display.withIndex()) {
                if (index != winnerIndex) {
                    item.remove()
                }
            }

            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
        } else {
            player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean {
        return (step < steps || timeSpentRevealing <= revealTime) && tick < timeout
    }

    override fun onFinish() {
        display.forEach { it.remove() }
    }

    object Factory : RollFactory<RollRoulette>("roulette") {
        override fun create(options: RollOptions): RollRoulette =
            RollRoulette(
                options.reward,
                options.crate,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
