package com.willfp.ecocrates.crate.roll

/**
 * Lays out the multi-reward versions of the GUI rolls: csgo lanes, slot machine paylines
 * and elimination candidates.
 *
 * The slot machine payline rows are in reward order. The first is the middle reel row,
 * the single payline the roll has always used, then the rows above and below it.
 */
object GuiRollLayout {
    /** The most lanes the csgo roll shows, one per reward. */
    const val CSGO_MAX_LANES = 4

    /** The extra scrolls each csgo lane takes over the lane above it, so the lanes stop in turn. */
    const val CSGO_LANE_GAP = 6

    /** The widest elimination row that still leaves a border of filler either side. */
    const val ELIMINATION_MAX_CANDIDATES = 7

    private const val ELIMINATION_MIN_CANDIDATES = 2

    private const val SELECTOR_ROW = "111121111"
    private const val LANE_ROW = "000000000"

    private val paylineRowsInRewardOrder = listOf(3, 2, 4)

    /** The most paylines the slot machine shows: one per reel row of its 5-row layout. */
    val slotMachineCapacity = paylineRowsInRewardOrder.size

    /**
     * Gets the number of rows of the csgo menu.
     *
     * @param lanes The number of lanes, one per reward.
     * @return One selector row above and below the lanes. One lane gives 3 rows.
     */
    fun csgoRows(lanes: Int): Int = lanes + 2

    /**
     * Gets the menu row a csgo lane scrolls in.
     *
     * @param lane The lane, starting at 0.
     * @return The 1-based menu row. Lane 0 scrolls in row 2.
     */
    fun csgoLaneRow(lane: Int): Int = lane + 2

    /**
     * Gets one row of the csgo menu's filler mask.
     *
     * @param row The row, starting at 0.
     * @param lanes The number of lanes, one per reward.
     * @return A selector row for the first and last rows, and an empty row for each lane between them.
     */
    fun csgoMaskRow(row: Int, lanes: Int): String = if (row == 0 || row == lanes + 1) SELECTOR_ROW else LANE_ROW

    /**
     * Gets how many times a csgo lane scrolls before the roll could end on it.
     *
     * @param lane The lane, starting at 0.
     * @param scrolls The configured scroll count.
     * @return [scrolls] for lane 0, then [CSGO_LANE_GAP] more for each lane after it.
     */
    fun csgoLaneScrolls(lane: Int, scrolls: Int): Int =
        RewardStagger.lockTick(index = lane, firstLock = scrolls, gap = CSGO_LANE_GAP)

    /**
     * Gets the scroll a csgo lane stops on. Every lane but the last stops one scroll early,
     * on the frame that shows its reward under the selector. The last lane scrolls once more
     * and the roll ends, as the single lane always has.
     *
     * @param lane The lane, starting at 0.
     * @param lanes The number of lanes.
     * @param scrolls The configured scroll count.
     * @return The scroll the lane stops on.
     */
    fun csgoLaneStop(lane: Int, lanes: Int, scrolls: Int): Int =
        csgoLaneScrolls(lane = lane, scrolls = scrolls) - if (lane == lanes - 1) 0 else 1

    /**
     * Gets the slot machine payline rows.
     *
     * @param count The number of paylines, one per reward.
     * @return The 1-based menu rows, the row for reward 0 first.
     */
    fun paylineRows(count: Int): List<Int> = paylineRowsInRewardOrder.take(count)

    /**
     * Gets the most rewards one elimination roll can give, leaving at least one candidate to eliminate.
     *
     * @param configured The configured number of candidates.
     * @return The candidate count less one.
     */
    fun eliminationCapacity(configured: Int): Int = eliminationCandidates(configured, 1) - 1

    /**
     * Gets the number of candidates an elimination roll shows.
     *
     * @param configured The configured number of candidates.
     * @param rewards The number of rewards.
     * @return The configured count, kept within the row, and raised to one more than [rewards].
     */
    fun eliminationCandidates(configured: Int, rewards: Int): Int =
        configured.coerceIn(minimumValue = ELIMINATION_MIN_CANDIDATES, maximumValue = ELIMINATION_MAX_CANDIDATES)
            .coerceAtLeast(rewards + 1)
}
