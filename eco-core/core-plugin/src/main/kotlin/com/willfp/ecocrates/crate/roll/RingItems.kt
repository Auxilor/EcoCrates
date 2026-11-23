package com.willfp.ecocrates.crate.roll

import org.bukkit.util.Vector
import kotlin.random.Random

/**
 * Sizes and fills the ring of items shown by the ring rolls: encircle, orbit collapse, vortex and roulette.
 */
object RingItems {
    /** Marks a slot in an [order] that shows a filler rather than a reward. */
    const val FILLER = -1

    /**
     * Gets the most rewards one ring roll can give, leaving at least one filler in the ring.
     *
     * @param configured The configured number of items in the ring.
     * @return The number of rewards, at least 1.
     */
    fun capacity(configured: Int): Int = (configured - 1).coerceAtLeast(1)

    /**
     * Gets the number of items a ring roll shows.
     *
     * @param configured The configured number of items in the ring.
     * @param minimum The fewest items the roll shows for one reward.
     * @param rewards The number of rewards.
     * @return The configured count, raised to [minimum], and for more than one reward raised to one more than [rewards].
     */
    fun count(configured: Int, minimum: Int, rewards: Int): Int =
        configured.coerceAtLeast(minimum).coerceAtLeast(if (rewards > 1) rewards + 1 else minimum)

    /**
     * Gets a shuffled order of the items in a ring.
     *
     * @param itemCount The number of items in the ring.
     * @param rewards The number of rewards.
     * @param random The source of randomness.
     * @return One entry per item: the index of the reward it shows, or [FILLER].
     */
    fun order(itemCount: Int, rewards: Int, random: Random = Random.Default): List<Int> =
        (List(itemCount - rewards) { FILLER } + List(rewards) { it }).shuffled(random)

    /**
     * Gets the winners from the items a ring roll spawned, in reward order.
     *
     * @param T The type of a spawned item.
     * @param spawned Each spawned item paired after its slot entry from [order].
     * @return The items that show a reward, the item for reward 0 first.
     */
    fun <T> winners(spawned: List<Pair<Int, T>>): List<T> =
        spawned.asSequence()
            .filter { it.first != FILLER }
            .sortedBy { it.first }
            .map { it.second }
            .toList()

    /**
     * Gets where the winners of a ring roll settle: a line across [center], perpendicular to the player's view.
     *
     * @param center The centre of the ring.
     * @param count The number of rewards.
     * @param yaw The player's yaw in degrees.
     * @return One position per reward, left to right. A single reward settles on [center].
     */
    fun winnerTargets(center: Vector, count: Int, yaw: Double): List<Vector> =
        RewardLine.offsets(count = count, yaw = yaw).map { center.clone().add(Vector(it.x, 0.0, it.z)) }

    /**
     * Gets how many steps the roulette cursor takes before each stop.
     * The first stop takes [steps]. Each later stop takes one lap plus an even spread,
     * so no two stops land on one item.
     *
     * @param count The number of stops, one per reward.
     * @param steps The configured steps before the first stop.
     * @param itemCount The number of items in the ring, more than [count].
     * @return The steps before each stop.
     */
    fun stopSteps(count: Int, steps: Int, itemCount: Int): List<Int> =
        List(count) { if (it == 0) steps else itemCount + (itemCount / count).coerceAtLeast(1) }

    /**
     * Gets the item the roulette cursor lands on at each stop, starting from item 0.
     *
     * @param stopSteps The steps before each stop.
     * @param itemCount The number of items in the ring.
     * @return The index of the item under the cursor at each stop.
     */
    fun stopIndices(stopSteps: List<Int>, itemCount: Int): List<Int> =
        stopSteps.runningReduce(Int::plus).map { it.mod(itemCount) }
}
