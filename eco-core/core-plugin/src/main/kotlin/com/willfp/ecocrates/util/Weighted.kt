package com.willfp.ecocrates.util

import kotlin.random.Random

/**
 * Pick a random element, where an element's chance of being picked is
 * proportional to its [weight]. Negative weights count as zero.
 *
 * Returns null only if the collection is empty. If every weight is zero,
 * a uniformly random element is returned rather than nothing, so that a
 * misconfigured weight never silently drops a reward or a rarity.
 */
fun <T> Collection<T>.weightedRandom(
    random: Random = Random.Default,
    weight: (T) -> Double
): T? {
    if (this.isEmpty()) {
        return null
    }

    val weighted = this.map { it to weight(it).coerceAtLeast(0.0) }
    val totalWeight = weighted.sumOf { it.second }

    if (totalWeight <= 0.0) {
        return this.elementAt(random.nextInt(this.size))
    }

    val roll = random.nextDouble(totalWeight)
    var cumulative = 0.0

    for ((element, elementWeight) in weighted) {
        cumulative += elementWeight

        if (roll < cumulative) {
            return element
        }
    }

    return weighted.last().first
}
