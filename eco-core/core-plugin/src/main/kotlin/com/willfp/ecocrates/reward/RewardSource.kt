package com.willfp.ecocrates.reward

import com.willfp.ecocrates.util.weightedRandom
import org.bukkit.entity.Player

/** The source-type IDs used in pending-reward storage and permissions. */
object SourceTypes {
    const val CRATE = "crate"
    const val POUCH = "pouch"
}

/**
 * Anything a roll can be run for: a crate or a pouch.
 * Rolls only ever talk to this, never to a concrete type.
 */
interface RewardSource {
    val id: String

    /** The formatted display name. */
    val name: String

    /** One of [SourceTypes]. */
    val sourceType: String

    val rewards: List<Reward>

    /** How far above the roll location world-based rolls should hover items. */
    val rollHeight: Double

    /** Gives [reward] to [player] (or queues it if they're offline), firing events and finish-effects. */
    fun handleFinish(player: Player, reward: Reward)

    fun getRandomReward(player: Player): Reward =
        rewards.weightedRandom { it.getEffectiveWeight(player) }
            ?: throw IllegalStateException("$sourceType '$id' has no rewards")

    fun getRandomRewards(player: Player, amount: Int): List<Reward> =
        List(amount.coerceAtLeast(0)) { getRandomReward(player) }

    fun hasRanOutOfRewards(player: Player): Boolean =
        rewards.all { it.getWeight(player) <= 0 }
}
