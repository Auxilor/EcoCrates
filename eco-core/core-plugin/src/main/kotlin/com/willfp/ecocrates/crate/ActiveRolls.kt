package com.willfp.ecocrates.crate

import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.runOwned
import org.bukkit.entity.Player
import java.util.Collections
import java.util.UUID

/**
 * Registry of in-flight rolls, keyed by player.
 *
 * eco cancels every plugin task before handleDisable and handleReload run, so
 * a roll ticker cannot finish itself there.
 */
object ActiveRolls {
    private class ActiveRoll(val player: Player, val finalize: (Boolean) -> Unit)

    private val active: MutableMap<UUID, ActiveRoll> = Collections.synchronizedMap(LinkedHashMap())

    fun register(player: Player, finalize: (Boolean) -> Unit) {
        active[player.uniqueId] = ActiveRoll(player, finalize)
    }

    fun unregister(player: Player) {
        active.remove(player.uniqueId)
    }

    /**
     * Force-finishes every active roll, queueing the rewards for next join
     * instead of giving them immediately when [queueForLater] is set.
     *
     * Each roll is finished on its player's region, as finishing touches the player.
     */
    fun finalizeAll(queueForLater: Boolean) {
        val snapshot = synchronized(active) { active.keys.toList() }

        for (uuid in snapshot) {
            val roll = active.remove(uuid) ?: continue

            roll.player.runOwned {
                try {
                    roll.finalize(queueForLater)
                } catch (e: Exception) {
                    plugin.logger.warning("Error while force-finishing a crate roll: ${e.javaClass.simpleName}: ${e.message}")
                }
            }
        }
    }
}
