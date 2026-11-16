package com.willfp.ecocrates.reward

import com.willfp.ecocrates.crate.open.OpenPick
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
@Suppress("ComplexInterface")
interface RewardSource {
    val id: String

    /** The formatted display name. */
    val name: String

    /** One of [SourceTypes]. */
    val sourceType: String

    val rewards: List<Reward>

    /** How far above the roll location world-based rolls should hover items. */
    val rollHeight: Double

    /** The chances of each bonus reward beyond the guaranteed first one. */
    val bonusChances: BonusChances
        get() = BonusChances(emptyList())

    /** The multi-reward options, local overrides resolved against config.yml. */
    val multiRewardSettings: MultiRewardSettings
        get() = MultiRewardSettings.fromConfig(null)

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

    fun hasRanOutOfRewards(player: Player): Boolean =
        rewards.all { it.getWeight(player) <= 0 }
}

/**
 * Decide every pick of one open: the guaranteed first reward, then one per bonus won from the bonus chances.
 *
 * @param player The opening player.
 * @param firePick Fires the open event for a pick and returns its reward, or null if it was cancelled.
 * @return The picks, numbered contiguously, or empty if the first pick was cancelled.
 */
internal fun RewardSource.planPicks(
    player: Player,
    firePick: (pickNumber: Int, reward: Reward, isBonus: Boolean) -> Reward?
): List<OpenPick> {
    val bonusCount = if (bonusChances.isEmpty) 0 else bonusChances.count(player, multiRewardSettings.bonusChanceMode)
    val picks = ArrayList<OpenPick>(bonusCount + 1)

    for (index in 0..bonusCount) {
        val isBonus = index > 0
        val reward = pickReward(player = player, taken = picks.map { it.reward }, isBonus = isBonus)
            ?.let { firePick(picks.size + 1, it, isBonus) }

        if (reward != null) {
            picks += OpenPick(number = picks.size + 1, reward = reward, isBonus = isBonus)
        } else if (!isBonus) {
            return emptyList()
        }
    }

    return picks
}

/**
 * Pick one reward by weight, leaving out rewards that the rest of the open rules out.
 *
 * @param player The opening player.
 * @param taken The rewards already picked in this open.
 * @param isBonus If this is a bonus pick.
 * @param isReroll If this replaces a pick already made. A first pick that is not a reroll falls back to
 * every reward when nothing is eligible, so the open always has a reward.
 * @param pending The rewards of [taken] not yet given, counted against max-wins.
 * @return The reward, or null if nothing with a weight above 0 is eligible for a bonus pick or a reroll.
 */
fun RewardSource.pickReward(
    player: Player,
    taken: List<Reward>,
    isBonus: Boolean,
    isReroll: Boolean = false,
    pending: List<Reward> = taken
): Reward? {
    val weights = getRewardWeights(player)
    val candidates = PickRules.eligible(
        candidates = rewards,
        taken = taken,
        isBonus = isBonus,
        rule = pickRule(player),
        pending = pending
    ).let { eligible -> weights.filter { it.reward in eligible } }

    return if (isBonus || isReroll) {
        candidates.filter { it.weight > 0 }.takeIf { it.isNotEmpty() }?.let { pickRandomReward(it) }
    } else {
        pickRandomReward(candidates.ifEmpty { weights })
    }
}

/**
 * The pick rules of one open for [player]: each reward's max-per-open, bonus-eligible and wins left before max-wins.
 *
 * @param player The opening player.
 * @return The rules.
 */
internal fun pickRule(player: Player): PickRule<Reward> = PickRule(
    maxPerOpen = { it.maxPerOpen },
    isBonusEligible = { it.isBonusEligible },
    maxWinsLeft = { if (it.maxWins > 0) it.maxWins - it.getWins(player) else Int.MAX_VALUE }
)

private fun RewardSource.pickRandomReward(weights: List<RewardWeight>): Reward =
    weights.filter { it.weight > 0 }
        .ifEmpty { weights }
        .weightedRandom { it.effectiveWeight }
        ?.reward
        ?: error("$sourceType '$id' has no rewards")
