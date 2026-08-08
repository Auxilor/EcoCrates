package com.willfp.ecocrates.envoy.compass

import java.util.UUID

/**
 * One player's running compass effect.
 *
 * [shownWaypoints] is the set of waypoint IDs currently drawn on that
 * player's locator bar, so the refresh loop can diff against it instead of
 * clearing and redrawing every tick.
 */
class ActiveCompass(
    val categoryId: String,
    var ticksRemaining: Int,
    val previousReceiveRange: Double?
) {
    val shownWaypoints = mutableSetOf<UUID>()
}
