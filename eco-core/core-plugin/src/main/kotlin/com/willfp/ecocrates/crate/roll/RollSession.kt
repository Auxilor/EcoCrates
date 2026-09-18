package com.willfp.ecocrates.crate.roll

import com.willfp.ecocrates.crate.isOpeningCrate
import com.willfp.ecocrates.plugin

/**
 * Drives a roll from start to finish: marks the player as opening, ticks the
 * roll every tick, and finalizes exactly once - when the roll says it's done,
 * when the player stops opening (e.g. quits), or when a tick throws.
 */
object RollSession {
    /**
     * @param onFinalize Runs after the roll has finished and the player is no
     * longer marked as opening. `forced` is true if the roll was aborted by an
     * error rather than completing normally.
     */
    fun start(roll: Roll, onFinalize: (roll: Roll, forced: Boolean) -> Unit) {
        val player = roll.player
        var tick = 0
        var hasFinalized = false

        fun finalizeRoll(forced: Boolean) {
            if (hasFinalized) {
                return
            }

            hasFinalized = true

            try {
                roll.onFinish()
            } catch (e: Exception) {
                plugin.logger.warning("Error while finishing roll for ${player.name}")
                e.printStackTrace()
            }

            player.isOpeningCrate = false

            onFinalize(roll, forced)
        }

        plugin.scheduler.on(player).runTimer({ task ->
            try {
                roll.tick(tick)
            } catch (e: Exception) {
                /*
                Bukkit doesn't cancel repeating tasks that throw, so without this the
                tick counter would never advance and the roll would repeat the same
                tick (and its effects) forever.
                 */
                plugin.logger.warning("Error while ticking roll for ${player.name}, cancelling")
                e.printStackTrace()

                task.cancel()
                finalizeRoll(true)
                return@runTimer
            }

            tick++

            if (!roll.shouldContinueTicking(tick) || !player.isOpeningCrate) {
                task.cancel()
                finalizeRoll(false)
            }
        }, 1, 1)

        player.isOpeningCrate = true

        roll.roll()
    }
}
