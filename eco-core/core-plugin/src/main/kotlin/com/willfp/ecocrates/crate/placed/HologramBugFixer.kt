package com.willfp.ecocrates.crate.placed

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent

object HologramBugFixer: Listener {
    @EventHandler
    fun handle(event: ChunkUnloadEvent) {
        val chunk = event.chunk
        for (crate in PlacedCrates.values()) {
            if (crate.location.chunk == chunk) {
                crate.handleChunkUnload()
            }
        }
    }
}