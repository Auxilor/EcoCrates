package com.willfp.ecocrates.envoy.compass

import com.willfp.eco.core.waypoint.Waypoints
import com.willfp.ecocrates.envoy.EnvoyCategory
import com.willfp.ecocrates.plugin
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which players currently have a compass running.
 *
 * State is in-memory only and is dropped on quit, on session end, and on
 * plugin disable - compass time is never banked across a relog.
 */
object EnvoyCompasses {
    private val active = ConcurrentHashMap<UUID, ActiveCompass>()

    fun get(player: Player): ActiveCompass? = active[player.uniqueId]

    fun isActive(player: Player): Boolean = active.containsKey(player.uniqueId)

    /**
     * Starts tracking for a player. The caller is responsible for having
     * already checked cooldown, price and that there is something to track.
     */
    fun activate(player: Player, category: EnvoyCategory): Boolean {
        val compass = category.compass ?: return false

        if (active.containsKey(player.uniqueId)) {
            return false
        }

        val attribute = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE)
        val previous = attribute?.baseValue

        // The client only renders waypoints within its receive range, so a
        // compass with a 500 block range is useless until this is raised.
        attribute?.baseValue = if (compass.range <= 0) {
            MAX_RECEIVE_RANGE
        } else {
            compass.range.toDouble()
        }

        active[player.uniqueId] = ActiveCompass(category.id, compass.durationTicks, previous)

        return true
    }

    /** Clears a player's waypoints and restores their receive range. */
    fun deactivate(player: Player) {
        val compass = active.remove(player.uniqueId) ?: return

        for (waypointId in compass.shownWaypoints) {
            Waypoints.hide(player, waypointId)
        }

        compass.shownWaypoints.clear()

        val attribute = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE)

        // Restore whatever the player had before, so the compass never
        // permanently changes their attributes.
        if (compass.previousReceiveRange != null) {
            attribute?.baseValue = compass.previousReceiveRange
        } else {
            attribute?.let { it.baseValue = it.defaultValue }
        }
    }

    fun deactivateAll() {
        for (uuid in active.keys.toList()) {
            val player = Bukkit.getPlayer(uuid)

            if (player == null) {
                active.remove(uuid)
                continue
            }

            deactivate(player)
        }
    }

    /** Counts down every active compass, ending those that expire. */
    fun tickCountdown() {
        for ((uuid, compass) in active.toList()) {
            compass.ticksRemaining--

            if (compass.ticksRemaining > 0) {
                continue
            }

            val player = Bukkit.getPlayer(uuid)

            if (player == null) {
                active.remove(uuid)
                continue
            }

            deactivate(player)
            player.sendMessage(plugin.langYml.getMessage("compass-expired"))
        }
    }

    private const val MAX_RECEIVE_RANGE = 60000000.0
}
