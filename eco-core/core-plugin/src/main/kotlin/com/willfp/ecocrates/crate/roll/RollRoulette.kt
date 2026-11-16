package com.willfp.ecocrates.crate.roll

import com.willfp.eco.util.NumberUtils
import com.willfp.ecocrates.crate.OpenMethod
import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.reward.Reward
import com.willfp.ecocrates.reward.RewardSource
import com.willfp.ecocrates.util.RollItems
import kotlin.math.PI
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.util.Vector

private const val SPARKLE_INTERVAL = 4

/**
 * The inverse of encircle: the items stand still in a ring on the ground and a
 * glowing cursor runs around them, slowing down until it stops on the winner.
 * With several rewards the cursor stops once per reward, and each earlier stop
 * stays lit until the roll ends.
 * Once the cursor has landed on its last stop, everything but the winners is
 * cleared and the winners sit there.
 */
class RollRoulette private constructor(
    override val rewards: List<Reward>,
    override val source: RewardSource,
    override val player: Player,
    override val location: Location,
    override val isReroll: Boolean,
    override val method: OpenMethod
) : Roll {
    override val reward: Reward = rewards.first()

    private val itemCount = RingItems.count(
        configured = plugin.configYml.getInt("rolls.roulette.items"),
        minimum = 2,
        rewards = rewards.size
    )
    private val radius = plugin.configYml.getDouble("rolls.roulette.radius")
    private val height = plugin.configYml.getDouble("rolls.roulette.height")
    private val steps = plugin.configYml.getInt("rolls.roulette.steps").coerceAtLeast(itemCount)
    private val bias = plugin.configYml.getDouble("rolls.roulette.bias")
    private val maxDelay = plugin.configYml.getInt("rolls.roulette.max-delay").coerceAtLeast(1)
    private val revealTime = plugin.configYml.getInt("rolls.roulette.reveal-time")

    private val stopSteps = RingItems.stopSteps(count = rewards.size, steps = steps, itemCount = itemCount)

    /*
    Same easing as the csgo roll: step delays follow a bias curve so the cursor
    starts fast and crawls to a stop, rather than halting abruptly.
     */
    private val stopDelays = stopSteps.map { stepsToStop ->
        (1..stepsToStop)
            .asSequence()
            .map { it / stepsToStop.toDouble() }
            .map { NumberUtils.bias(it, bias) }
            .map { (it * maxDelay).toInt() }
            .map { it.coerceAtLeast(1) }
            .toList()
    }

    private val timeout = plugin.configYml.getInt("rolls.roulette.timeout") +
        stopDelays.drop(1).sumOf { it.sum() }

    // The ring is laid out so that each stop of the cursor lands on its winner.
    private val winnerIndices = RingItems.stopIndices(stopSteps = stopSteps, itemCount = itemCount)
    private val winnerSlots = winnerIndices.toSet()

    private val fillerItems = source.getRandomRewards(player, itemCount - rewards.size)

    private val center = location.toVector().add(Vector(0.0, height, 0.0))

    private val display = mutableListOf<Item>()

    private var step = 0
    private var stop = 0
    private var stepInStop = 0
    private var ticksSinceStep = 0
    private var timeSpentRevealing = 0

    @Suppress("DEPRECATION")
    override fun roll() {
        val world = location.world!!
        val angle = 2 * PI / itemCount

        player.closeInventory()

        val fillers = fillerItems.iterator()

        for (index in 0 until itemCount) {
            val displayReward = winnerIndices.indexOf(index).let { if (it >= 0) rewards[it] else fillers.next() }

            val position = center.clone().add(
                Vector(
                    NumberUtils.fastSin(angle * index) * radius,
                    0.0,
                    NumberUtils.fastCos(angle * index) * radius
                )
            )

            val entity = world.dropItem(position.toLocation(world), displayReward.getDisplay(player, source))

            entity.pickupDelay = Int.MAX_VALUE
            entity.setGravity(false)
            entity.velocity = Vector(0, 0, 0)
            entity.isCustomNameVisible = true
            entity.customName = displayReward.displayName
            RollItems.mark(entity)
            display.add(entity)
        }

        display.firstOrNull()?.isGlowing = true
    }

    override fun tick(tick: Int) {
        // The ring keeps its original size even if an item is lost, otherwise the
        // cursor's landing position would no longer line up with the winner.
        when {
            display.none { it.isValid } -> timeSpentRevealing = revealTime + 1
            stop >= stopSteps.size -> reveal(tick)
            else -> advanceCursor()
        }
    }

    private fun reveal(tick: Int) {
        timeSpentRevealing++

        if (tick % SPARKLE_INTERVAL == 0) {
            sparkle(winnerIndices.dropLast(1))
        }
    }

    private fun sparkle(indices: Collection<Int>) {
        indices.map { display[it] }
            .filter { it.isValid }
            .forEach { it.world.spawnParticle(Particle.END_ROD, it.location, 6, 0.1, 0.1, 0.1, 0.01) }
    }

    private fun advanceCursor() {
        ticksSinceStep++

        if (ticksSinceStep < stopDelays[stop][stepInStop]) {
            return
        }

        ticksSinceStep = 0
        step++
        stepInStop++

        val cursorIndex = step.mod(itemCount)
        val landed = stepInStop >= stopSteps[stop]
        val earlierStops = winnerIndices.take(stop).toSet()

        if (landed) {
            stop++
            stepInStop = 0
        }

        for ((index, item) in display.withIndex()) {
            item.isGlowing = index == cursorIndex || index in earlierStops
        }

        sparkle(earlierStops + cursorIndex)

        if (stop >= stopSteps.size) {
            for ((index, item) in display.withIndex()) {
                if (index !in winnerSlots) {
                    item.remove()
                }
            }
        }

        if (landed) {
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f)
        } else {
            player.playSound(player.location, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1.0f, 1.0f)
        }
    }

    override fun shouldContinueTicking(tick: Int): Boolean =
        (stop < stopSteps.size || timeSpentRevealing <= revealTime) && tick < timeout

    override fun onFinish() {
        display.forEach { it.remove() }
    }

    object Factory : RollFactory<RollRoulette>("roulette") {
        override val maxRewards: Int
            get() = RingItems.capacity(plugin.configYml.getInt("rolls.roulette.items"))

        override fun create(options: RollOptions): RollRoulette =
            RollRoulette(
                options.rewards,
                options.source,
                options.player,
                options.location,
                options.isReroll,
                options.method
            )
    }
}
