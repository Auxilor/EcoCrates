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

    fun getRewardWeights(player: Player): List<RewardWeight> =
        rewards.map { it.getRewardWeight(player) }

    fun getRandomReward(player: Player): Reward =
        pickRandomReward(getRewardWeights(player))

    fun getRandomRewards(player: Player, amount: Int): List<Reward> {
        val weights = getRewardWeights(player)
        return List(amount.coerceAtLeast(0)) { pickRandomReward(weights) }
    }

    private fun pickRandomReward(weights: List<RewardWeight>): Reward =
        weights.filter { it.weight > 0 }
            .ifEmpty { weights }
            .weightedRandom { it.effectiveWeight }
            ?.reward
            ?: throw IllegalStateException("$sourceType '$id' has no rewards")

    fun hasRanOutOfRewards(player: Player): Boolean =
        rewards.all { it.getWeight(player) <= 0 }
}
