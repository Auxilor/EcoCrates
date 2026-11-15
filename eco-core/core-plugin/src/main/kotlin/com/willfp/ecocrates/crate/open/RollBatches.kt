package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.reward.MultiRewardFallback

/**
 * Splits the picks of an open into the batches played one roll each.
 *
 * A roll that shows several rewards gets batches of up to [maxRewards]. A roll that shows one
 * uses [fallback]: `sequential` plays one batch per pick, and `primary` plays only the first pick,
 * leaving the rest to be given once it is accepted.
 *
 * @param T The pick type.
 * @param picks Every pick of the open, in order.
 * @param maxRewards The most rewards the roll shows at once.
 * @param fallback How a roll that shows one reward plays several.
 * @return The batches, in order.
 */
internal fun <T> splitIntoRollBatches(
    picks: List<T>,
    maxRewards: Int,
    fallback: MultiRewardFallback
): List<List<T>> = when {
    maxRewards > 1 -> picks.chunked(maxRewards)
    fallback == MultiRewardFallback.PRIMARY -> listOf(picks.take(1))
    else -> picks.map { listOf(it) }
}
