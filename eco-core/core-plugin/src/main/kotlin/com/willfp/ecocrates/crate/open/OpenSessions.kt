package com.willfp.ecocrates.crate.open

import com.willfp.ecocrates.plugin
import com.willfp.ecocrates.runOwned
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry of opens still in progress, keyed by player.
 *
 * An open stays here from its first roll until its last reward is given or queued,
 * including the gaps between rolls while a reroll menu is open.
 */
internal object OpenSessions {
    private val sessions = ConcurrentHashMap<UUID, OpenSession>()

    fun register(player: Player, session: OpenSession) {
        sessions[player.uniqueId] = session
    }

    fun unregister(player: Player) {
        sessions.remove(player.uniqueId)
    }

    fun get(player: Player): OpenSession? = sessions[player.uniqueId]

    /**
     * Registers [session] and plays its first roll, unregistering it again if that fails.
     *
     * @param session The open to start.
     */
    fun start(session: OpenSession) {
        register(session.player, session)
        runCatching { session.play() }
            .onFailure { unregister(session.player) }
            .getOrThrow()
    }

    /**
     * Finishes every open in progress, giving or queueing each reward not yet given.
     *
     * Each open is finished on its player's region, as finishing touches the player.
     *
     * @param queueForLater If the rewards are queued for the next join instead of given now.
     */
    fun finalizeAll(queueForLater: Boolean) {
        for (session in sessions.values.toList()) {
            session.player.runOwned {
                runCatching { session.finishAll(queueForLater) }
                    .onFailure { plugin.logger.warning("Error while force-finishing a crate open: $it") }
            }
        }
    }
}
