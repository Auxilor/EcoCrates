package com.willfp.ecocrates.envoy.compass

import com.willfp.eco.core.waypoint.Waypoints
import com.willfp.ecocrates.envoy.EnvoyCompass
import com.willfp.ecocrates.envoy.Envoys
import com.willfp.ecocrates.envoy.session.EnvoySessions
import com.willfp.ecocrates.envoy.session.SpawnedEnvoy
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object CompassRenderer {
    /**
     * The single source of truth for whether a spawn should show up on a
     * player's compass: same rarity flag, same world, and within range.
     * [CompassListener] uses this to decide whether there is anything to
     * activate on before consuming the compass, so it can never drift from
     * what actually gets rendered below.
     */
    internal fun isVisibleFor(spawn: SpawnedEnvoy, player: Player, settings: EnvoyCompass): Boolean =
        spawn.rarity.showOnCompass &&
            spawn.blockLocation.world == player.world &&
            (settings.range <= 0 || spawn.centeredLocation.distance(player.location) <= settings.range)

    /**
     * Brings every active compass's locator bar in line with what the player
     * should currently see, sending only the differences.
     */
    fun refresh() {
        val session = EnvoySessions.active ?: return

        for (player in Bukkit.getOnlinePlayers()) {
            val compass = EnvoyCompasses.get(player) ?: continue

            if (compass.categoryId != session.category.id) {
                // The session changed under them; stop tracking rather than
                // point at crates from an envoy their compass isn't for.
                EnvoyCompasses.deactivate(player)
                continue
            }

            refreshFor(player, compass)
        }
    }

    private fun refreshFor(player: Player, compass: ActiveCompass) {
        val session = EnvoySessions.active ?: return
        val category = Envoys[compass.categoryId] ?: return
        val settings = category.compass ?: return

        val target = session.spawns
            .filter { isVisibleFor(it, player, settings) }
            .sortedBy { it.centeredLocation.distanceSquared(player.location) }
            .take(settings.maxTracked)

        val targetIds = target.map { it.waypointId }.toSet()

        // Remove markers for crates that were collected, went out of range,
        // or got pushed out of the nearest-N window.
        for (waypointId in compass.shownWaypoints.toList()) {
            if (waypointId in targetIds) {
                continue
            }

            Waypoints.hide(player, waypointId)
            compass.shownWaypoints.remove(waypointId)
        }

        for (spawn in target) {
            if (spawn.waypointId in compass.shownWaypoints) {
                continue
            }

            show(player, spawn)
            compass.shownWaypoints.add(spawn.waypointId)
        }
    }

    private fun show(player: Player, spawn: SpawnedEnvoy) {
        Waypoints.show(
            player,
            spawn.waypointId,
            spawn.centeredLocation,
            spawn.rarity.compassColor
        )
    }
}
