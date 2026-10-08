package com.willfp.ecocrates.envoy.compass

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * One player's running compass effect.
 *
 * [shownWaypoints] is the set of waypoint IDs currently drawn on that
 * player's locator bar, so the refresh loop can diff against it instead of
 * clearing and redrawing every tick.
 */
class ActiveCompass(
    val categoryId: String,
    ticksRemaining: Int,
    val previousReceiveRange: Double?
) {
    @Volatile
    var ticksRemaining: Int = ticksRemaining

    val shownWaypoints: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
}
