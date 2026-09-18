package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Registry of in-flight rolls, keyed by player.
 *
 * eco cancels every plugin task before handleDisable and handleReload run, so
 * a roll ticker cannot finish itself there.
 */
object ActiveRolls {
    private val active = mutableMapOf<UUID, (Boolean) -> Unit>()

    fun register(player: Player, finalize: (Boolean) -> Unit) {
        active[player.uniqueId] = finalize
    }

    fun unregister(player: Player) {
        active.remove(player.uniqueId)
    }

    /**
     * Force-finishes every active roll, queueing the rewards for next join
     * instead of giving them immediately when [queueForLater] is set.
     */
    fun finalizeAll(queueForLater: Boolean) {
        val snapshot = active.values.toList()
        active.clear()

        for (finalize in snapshot) {
            try {
                finalize(queueForLater)
            } catch (e: Exception) {
                plugin.logger.warning("Error while force-finishing a crate roll: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }
}
