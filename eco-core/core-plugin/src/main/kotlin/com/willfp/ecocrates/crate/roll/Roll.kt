package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.reward.Reward

interface Roll {
    /**
     * The reward that will be given.
     */
    val reward: Reward

    /**
     * Called on start - once the player begins opening the crate.
     */
    fun roll()

    /**
     * Tick the roll.
     *
     * @param tick The current tick.
     */
    fun tick(tick: Int)

    /**
     * Get if the crate should continue ticking,
     * if this returns false then the roll is finished,
     * and onFinish will be called, rewards will be given, etc.
     *
     * @param tick The current tick.
     * @return If ticking should continue.
     */
    fun shouldContinueTicking(tick: Int): Boolean

    /**
     * Called once the crate is finished, useful if the
     * roll has an inventory as it can then be closed.
     */
    fun onFinish()
}
