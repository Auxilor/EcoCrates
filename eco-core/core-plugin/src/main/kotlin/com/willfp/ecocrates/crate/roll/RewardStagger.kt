package com.willfp.ecocrates.crate.roll

/**
 * Schedules rewards that lock in turn, a fixed gap apart, during one roll.
 */
object RewardStagger {
    /**
     * Gets the tick a reward locks on.
     *
     * @param index The reward's position in the roll, starting at 0.
     * @param firstLock The tick the first reward locks on.
     * @param gap The ticks between neighbouring rewards locking.
     * @return The tick the reward locks on. The first reward locks on [firstLock].
     */
    fun lockTick(index: Int, firstLock: Int, gap: Int): Int = firstLock + index * gap

    /**
     * Gets the tick a roll ends on, [hold] ticks after its last reward locks.
     *
     * @param count The number of rewards.
     * @param firstLock The tick the first reward locks on.
     * @param gap The ticks between neighbouring rewards locking.
     * @param hold The ticks the roll continues after the last reward locks.
     * @return The tick the roll ends on. One reward ends on [firstLock] plus [hold].
     */
    fun endTick(count: Int, firstLock: Int, gap: Int, hold: Int): Int = lockTick(count - 1, firstLock, gap) + hold

    /**
     * Gets how many rewards have locked by a tick.
     *
     * @param tick The current tick.
     * @param firstLock The tick the first reward locks on.
     * @param gap The ticks between neighbouring rewards locking.
     * @param count The number of rewards.
     * @return The number of rewards locked on or before [tick].
     */
    fun lockedCount(tick: Int, firstLock: Int, gap: Int, count: Int): Int =
        (0 until count).count { lockTick(it, firstLock, gap) <= tick }
}
