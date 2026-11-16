package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.Reward

/**
 * Where the rewards of a roll sit in their open, for rolls that apply the open's pick rules
 * themselves, such as choose.
 *
 * @property bonusSlots Per reward of the roll, in order, if it is a bonus pick.
 * @property otherPicks The rewards the open's other picks already hold, outside this roll.
 * @property pendingPicks The rewards of [otherPicks] not yet given.
 */
class RollPickContext(
    val bonusSlots: List<Boolean>,
    val otherPicks: List<Reward>,
    val pendingPicks: List<Reward> = otherPicks
) {
    /**
     * Gets if a reward of the roll is a bonus pick.
     *
     * @param slot The index of the reward in the roll.
     * @return If it is a bonus. A slot not listed is a bonus unless it is the first.
     */
    fun isBonus(slot: Int): Boolean = bonusSlots.getOrElse(slot) { slot > 0 }

    companion object {
        /**
         * The context of a roll outside a session: the first reward is the main pick and nothing else is held.
         */
        val DEFAULT = RollPickContext(bonusSlots = emptyList(), otherPicks = emptyList())
    }
}
