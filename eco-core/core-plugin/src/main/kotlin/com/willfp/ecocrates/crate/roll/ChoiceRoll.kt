package com.willfp.ecocrates.crate.roll

/**
 * A roll that waits on the player to choose its rewards, and so can drop rewards or leave choices
 * unmade when it ends. The open session settles it before the rewards are read.
 */
internal interface ChoiceRoll {
    /**
     * The indices of [Roll.rewards] the roll dropped because it had nothing left to offer for them,
     * such as a choose roll's bonus choice with no options. Their picks are not given.
     */
    val droppedRewardSlots: Set<Int>

    /**
     * Called before the rewards are read, including when the roll is forced to finish early,
     * so a roll that waits on the player can settle what is left the same way it would on its own.
     */
    fun completePendingChoices()
}
