package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.reward.Reward

/**
 * One reward of an open: the guaranteed first pick or a bonus.
 *
 * @param number The 1-based position of the pick within the open when it is planned.
 * @property reward The reward, replaced in place by a reroll or by the player's choice in a choose roll.
 * @property isBonus If the pick is a bonus beyond the first.
 */
internal class OpenPick(
    number: Int,
    var reward: Reward,
    val isBonus: Boolean
) {
    /** The 1-based position of the pick within the open, renumbered if a choose roll drops a pick. */
    var number: Int = number
        internal set

    /** How many times this pick has been rerolled. */
    var rerolls: Int = 0

    /** If this pick has been given or queued. */
    var isGiven: Boolean = false
}
