package com.willfp.ecocrates.crate.roll

/**
 * Sizes the multi-reward versions of the player-interactive rolls, pick and match.
 */
object ChoiceRolls {
    /** The most rewards one pick roll can give: one fewer than the widest row of boxes. */
    const val PICK_MAX_REWARDS = 6

    /** The most card rows the match menu holds: six menu rows less a filler row above and below. */
    const val MATCH_MAX_ROWS = 4

    /** The most cards in one match row, leaving a border of filler either side. */
    const val MATCH_CARDS_PER_ROW = 7

    private const val PICK_MIN_BOXES = 2
    private const val PICK_MAX_BOXES = 7
    private const val MATCH_MIN_TO_MATCH = 2

    private const val MATCH_SINGLE_REWARD_ROWS = 3
    private const val MATCH_SINGLE_CARDS = MATCH_SINGLE_REWARD_ROWS * MATCH_CARDS_PER_ROW
    private const val MATCH_MAX_CARDS = MATCH_MAX_ROWS * MATCH_CARDS_PER_ROW

    /**
     * Gets the number of boxes a pick roll shows.
     *
     * @param configured The configured number of boxes.
     * @param rewards The number of rewards.
     * @return The configured count kept within the row, raised to one more than [rewards], at most 7.
     */
    fun pickBoxes(configured: Int, rewards: Int): Int =
        configured.coerceIn(minimumValue = PICK_MIN_BOXES, maximumValue = PICK_MAX_BOXES)
            .coerceAtLeast(rewards + 1)
            .coerceAtMost(PICK_MAX_BOXES)

    /**
     * Gets the number of copies that make a set in a match roll.
     *
     * @param configured The configured to-match.
     * @return The configured count, at least 2 and at most the cards of a single-reward card.
     */
    fun matchToMatch(configured: Int): Int =
        configured.coerceIn(minimumValue = MATCH_MIN_TO_MATCH, maximumValue = MATCH_SINGLE_CARDS)

    /**
     * Gets the most rewards one match roll can give: as many sets as fit the largest card.
     *
     * @param toMatch The copies that make a set.
     * @return The number of sets that fit, at least 1.
     */
    fun matchCapacity(toMatch: Int): Int = (MATCH_MAX_CARDS / toMatch).coerceAtLeast(1)

    /**
     * Gets the most losing cards a match roll deals. The losers keep the room they have on a
     * single-reward card, and the card grows to fit the extra sets, up to the largest card.
     *
     * @param toMatch The copies that make a set.
     * @param rewards The number of rewards, one set each.
     * @return The most losing cards, never below 0.
     */
    fun matchLoserCapacity(toMatch: Int, rewards: Int): Int =
        minOf(a = MATCH_SINGLE_CARDS - toMatch, b = MATCH_MAX_CARDS - rewards * toMatch).coerceAtLeast(0)

    /**
     * Gets how far through its current set a reward is in a match roll.
     *
     * @param found The cards of that reward scratched so far, at least 1.
     * @param toMatch The copies that make a set.
     * @return From 1 to [toMatch], where [toMatch] means the latest card completed a set.
     */
    fun setProgress(found: Int, toMatch: Int): Int = (found - 1) % toMatch + 1
}
