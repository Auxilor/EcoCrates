package com.willfp.ecocrates.event

/**
 * Which pick of a multi-reward open an event describes.
 *
 * @property pickNumber The 1-based position of the pick within the open.
 * @property isBonus If the pick was an extra one beyond the first.
 */
data class PickInfo(
    val pickNumber: Int,
    val isBonus: Boolean
) {
    companion object {
        /** The only pick of a single-reward open. */
        val FIRST = PickInfo(1, false)
    }
}
