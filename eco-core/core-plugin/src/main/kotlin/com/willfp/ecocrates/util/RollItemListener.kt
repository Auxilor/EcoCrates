package com.willfp.ecocrates.util

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent

/**
 * Removes roll display items left behind by a previous server session, for the
 * chunks that the enable-time sweep could not reach.
 */
object RollItemListener : Listener {
    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        event.chunk.entities
            .filter { RollItems.isOrphaned(it) }
            .forEach { it.remove() }
    }
}
