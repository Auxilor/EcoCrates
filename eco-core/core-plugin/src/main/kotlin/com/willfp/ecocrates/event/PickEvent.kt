package com.willfp.ecocrates.event

/**
 * An event that describes one pick of a multi-reward open.
 */
interface PickEvent {
    /** The pick this event describes. */
    val pick: PickInfo

    /** The 1-based position of the pick within the open. */
    val pickNumber: Int
        get() = pick.pickNumber

    /** If the pick was an extra one beyond the first. */
    @Suppress("U003")
    val isBonus: Boolean
        get() = pick.isBonus
}
