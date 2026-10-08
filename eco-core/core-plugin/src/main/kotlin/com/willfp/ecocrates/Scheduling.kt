package com.willfp.ecocrates

import com.willfp.eco.core.Eco
import com.willfp.eco.core.Prerequisite
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

/**
 * Run [block] on the region owning this entity: now if the current thread already owns it,
 * which is always the case off Folia, otherwise on the entity's next tick.
 */
internal inline fun Entity.runOwned(crossinline block: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(this)) {
        block()
    } else {
        plugin.scheduler.on(this).run { block() }
    }
}

/**
 * Run [block] on the region owning this location: now if the current thread already owns it,
 * which is always the case off Folia, otherwise on the region's next tick.
 */
internal inline fun Location.runOwned(crossinline block: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(this)) {
        block()
    } else {
        plugin.scheduler.at(this).run { block() }
    }
}

/**
 * Run [block] on the global region: now if this thread is already the global region thread,
 * which is always the case off Folia, otherwise on the global region's next tick.
 */
internal inline fun runGlobal(crossinline block: () -> Unit) {
    if (Prerequisite.HAS_FOLIA.isMet && !Bukkit.isGlobalTickThread()) {
        plugin.scheduler.global().run { block() }
    } else {
        block()
    }
}

/**
 * Entity#teleport is unsupported on Folia, where teleportAsync replaces it.
 */
internal fun Entity.teleportCompat(location: Location) {
    if (Prerequisite.HAS_FOLIA.isMet) {
        teleportAsync(location)
    } else {
        teleport(location)
    }
}
