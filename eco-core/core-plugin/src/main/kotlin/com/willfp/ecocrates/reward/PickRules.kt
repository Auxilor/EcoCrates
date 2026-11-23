package com.willfp.ecocrates.reward

/**
 * Per-candidate limits applied while picking the rewards of one open.
 *
 * @param T The candidate type.
 * @property maxPerOpen The most times a candidate can be picked in one open, or 0 or less for no limit.
 * @property isBonusEligible If a candidate can be picked as a bonus reward.
 * @property maxWinsLeft The wins a candidate has left before max-wins, or [Int.MAX_VALUE] for no limit.
 */
class PickRule<T>(
    val maxPerOpen: (T) -> Int,
    val isBonusEligible: (T) -> Boolean,
    val maxWinsLeft: (T) -> Int
)

/**
 * Works out which candidates can still be picked in an open.
 */
object PickRules {
    /**
     * Filter candidates down to those that can be picked next.
     *
     * @param T The candidate type.
     * @param candidates Every possible candidate.
     * @param taken The candidates already picked in this open, counted against max-per-open.
     * @param isBonus If the next pick is a bonus pick.
     * @param rule The limits to apply.
     * @param pending The picks of [taken] not yet given, counted against max-wins. Given picks are
     * already counted in the wins left.
     * @return The candidates that can still be picked, in their original order.
     */
    fun <T> eligible(
        candidates: List<T>,
        taken: List<T>,
        isBonus: Boolean,
        rule: PickRule<T>,
        pending: List<T> = taken
    ): List<T> =
        candidates.filter { candidate ->
            val timesTaken = taken.count { it == candidate }
            val maxPerOpen = rule.maxPerOpen(candidate)

            (maxPerOpen <= 0 || timesTaken < maxPerOpen) &&
                (!isBonus || rule.isBonusEligible(candidate)) &&
                pending.count { it == candidate } < rule.maxWinsLeft(candidate)
        }
}
